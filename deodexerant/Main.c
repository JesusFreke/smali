/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * As per the Apache license requirements, this file has been modified
 * from its original state.
 *
 * Such modifications are Copyright (C) 2010 Ben Gruver, and are released
 * under the original license
 */

/*
 * Command-line invocation of the Dalvik VM.
 */
#include "jni.h"
#include "Dalvik.h"
#include "libdex/OptInvocation.h"

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <signal.h>
#include <assert.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>

#include "utils/Log.h"

#define VERSION "1.0"

#define VER1_(x) #x
#define VER_(x) VER1_(x)
#define ANDROID_VERSION VER_(ANDROID_VER)

typedef struct InlineSub {
    Method* method;
    int     inlineIdx;
} InlineSub;

static DexStringCache stringCache;

static ClassObject* findCommonSuperclass(ClassObject* c1, ClassObject* c2);

//The following methods yanked from vm/analysis/CodeVerify.c
/*
 * Compute the "class depth" of a class.  This is the distance from the
 * class to the top of the tree, chasing superclass links.  java.lang.Object
 * has a class depth of 0.
 */
static int getClassDepth(ClassObject* clazz)
{
    int depth = 0;

    while (clazz->super != NULL) {
        clazz = clazz->super;
        depth++;
    }
    return depth;
}

/*
 * Given two classes, walk up the superclass tree to find a common
 * ancestor.  (Called from findCommonSuperclass().)
 *
 * TODO: consider caching the class depth in the class object so we don't
 * have to search for it here.
 */
static ClassObject* digForSuperclass(ClassObject* c1, ClassObject* c2)
{
    int depth1, depth2;

    depth1 = getClassDepth(c1);
    depth2 = getClassDepth(c2);

    /* pull the deepest one up */
    if (depth1 > depth2) {
        while (depth1 > depth2) {
            c1 = c1->super;
            depth1--;
        }
    } else {
        while (depth2 > depth1) {
            c2 = c2->super;
            depth2--;
        }
    }

    /* walk up in lock-step */
    while (c1 != c2) {
        c1 = c1->super;
        c2 = c2->super;

        assert(c1 != NULL && c2 != NULL);
    }

    return c1;
}

/*
 * Merge two array classes.  We can't use the general "walk up to the
 * superclass" merge because the superclass of an array is always Object.
 * We want String[] + Integer[] = Object[].  This works for higher dimensions
 * as well, e.g. String[][] + Integer[][] = Object[][].
 *
 * If Foo1 and Foo2 are subclasses of Foo, Foo1[] + Foo2[] = Foo[].
 *
 * If Class implements Type, Class[] + Type[] = Type[].
 *
 * If the dimensions don't match, we want to convert to an array of Object
 * with the least dimension, e.g. String[][] + String[][][][] = Object[][].
 *
 * This gets a little awkward because we may have to ask the VM to create
 * a new array type with the appropriate element and dimensions.  However, we
 * shouldn't be doing this often.
 */
static ClassObject* findCommonArraySuperclass(ClassObject* c1, ClassObject* c2)
{
    ClassObject* arrayClass = NULL;
    ClassObject* commonElem;
    int i, numDims;

    assert(c1->arrayDim > 0);
    assert(c2->arrayDim > 0);

    if (c1->arrayDim == c2->arrayDim) {
        //commonElem = digForSuperclass(c1->elementClass, c2->elementClass);
        commonElem = findCommonSuperclass(c1->elementClass, c2->elementClass);
        numDims = c1->arrayDim;
    } else {
        if (c1->arrayDim < c2->arrayDim)
            numDims = c1->arrayDim;
        else
            numDims = c2->arrayDim;
        commonElem = c1->super;     // == java.lang.Object
    }

    /* walk from the element to the (multi-)dimensioned array type */
    for (i = 0; i < numDims; i++) {
        arrayClass = dvmFindArrayClassForElement(commonElem);
        commonElem = arrayClass;
    }

    return arrayClass;
}

/*
 * Find the first common superclass of the two classes.  We're not
 * interested in common interfaces.
 *
 * The easiest way to do this for concrete classes is to compute the "class
 * depth" of each, move up toward the root of the deepest one until they're
 * at the same depth, then walk both up to the root until they match.
 *
 * If both classes are arrays of non-primitive types, we need to merge
 * based on array depth and element type.
 *
 * If one class is an interface, we check to see if the other class/interface
 * (or one of its predecessors) implements the interface.  If so, we return
 * the interface; otherwise, we return Object.
 *
 * NOTE: we continue the tradition of "lazy interface handling".  To wit,
 * suppose we have three classes:
 *   One implements Fancy, Free
 *   Two implements Fancy, Free
 *   Three implements Free
 * where Fancy and Free are unrelated interfaces.  The code requires us
 * to merge One into Two.  Ideally we'd use a common interface, which
 * gives us a choice between Fancy and Free, and no guidance on which to
 * use.  If we use Free, we'll be okay when Three gets merged in, but if
 * we choose Fancy, we're hosed.  The "ideal" solution is to create a
 * set of common interfaces and carry that around, merging further references
 * into it.  This is a pain.  The easy solution is to simply boil them
 * down to Objects and let the runtime invokeinterface call fail, which
 * is what we do.
 */
static ClassObject* findCommonSuperclass(ClassObject* c1, ClassObject* c2)
{
    assert(!dvmIsPrimitiveClass(c1) && !dvmIsPrimitiveClass(c2));

    if (c1 == c2)
        return c1;

    if (dvmIsInterfaceClass(c1) && dvmImplements(c2, c1)) {
        return c1;
    }
    if (dvmIsInterfaceClass(c2) && dvmImplements(c1, c2)) {
        return c2;
    }

    if (dvmIsArrayClass(c1) && dvmIsArrayClass(c2) &&
        !dvmIsPrimitiveClass(c1->elementClass) &&
        !dvmIsPrimitiveClass(c2->elementClass))
    {
        return findCommonArraySuperclass(c1, c2);
    }

    return digForSuperclass(c1, c2);
}














//method yanked from vm/analysis/DexOptimize.c
/*
 * Try to load all classes in the specified DEX.  If they have some sort
 * of broken dependency, e.g. their superclass lives in a different DEX
 * that wasn't previously loaded into the bootstrap class path, loading
 * will fail.  This is the desired behavior.
 *
 * We have no notion of class loader at this point, so we load all of
 * the classes with the bootstrap class loader.  It turns out this has
 * exactly the behavior we want, and has no ill side effects because we're
 * running in a separate process and anything we load here will be forgotten.
 *
 * We set the CLASS_MULTIPLE_DEFS flag here if we see multiple definitions.
 * This works because we only call here as part of optimization / pre-verify,
 * not during verification as part of loading a class into a running VM.
 *
 * This returns "false" if the world is too screwed up to do anything
 * useful at all.
 */
int loadAllClasses(DvmDex* pDvmDex)
{
    u4 count = pDvmDex->pDexFile->pHeader->classDefsSize;
    u4 idx;
    int loaded = 0;

    dvmSetBootPathExtraDex(pDvmDex);

    /*
     * We have some circularity issues with Class and Object that are most
     * easily avoided by ensuring that Object is never the first thing we
     * try to find.  Take care of that here.  (We only need to do this when
     * loading classes from the DEX file that contains Object, and only
     * when Object comes first in the list, but it costs very little to
     * do it in all cases.)
     */
    if (dvmFindSystemClass("Ljava/lang/Class;") == NULL) {
        return false;
    }

    for (idx = 0; idx < count; idx++) {
        const DexClassDef* pClassDef;
        const char* classDescriptor;
        ClassObject* newClass;

        pClassDef = dexGetClassDef(pDvmDex->pDexFile, idx);
        classDescriptor =
            dexStringByTypeIdx(pDvmDex->pDexFile, pClassDef->classIdx);

        //newClass = dvmDefineClass(pDexFile, classDescriptor,
        //        NULL);
        newClass = dvmFindSystemClassNoInit(classDescriptor);
        if (newClass == NULL) {
            dvmClearOptException(dvmThreadSelf());
        } else if (newClass->pDvmDex != pDvmDex) {
            /*
             * We don't load the new one, and we tag the first one found
             * with the "multiple def" flag so the resolver doesn't try
             * to make it available.
             */
            SET_CLASS_FLAG(newClass, CLASS_MULTIPLE_DEFS);
        } else {
            loaded++;
        }
    }

    dvmSetBootPathExtraDex(NULL);
    return true;
}

int dumpFields(char *classType, FILE *clientOut)
{
    ClassObject *clazz;
    if (classType[0] == '[')
        clazz = dvmFindArrayClass(classType, NULL);
    else
        clazz = dvmFindSystemClassNoInit(classType);

    if (clazz == NULL)
        return 0;

    int i;
    do
    {
        InstField *pField = clazz->ifields;
        for (i=0; i<clazz->ifieldCount; i++, pField++)
            fprintf(clientOut, "field: %d %s:%s\n", pField->byteOffset, pField->field.name, pField->field.signature);

        clazz = clazz->super;
    } while (clazz != NULL);

    return 1;
}

int dumpInlineMethods(FILE *clientOut)
{
    const InlineOperation *inlineTable = dvmGetInlineOpsTable();
    int count = dvmGetInlineOpsTableLength();

    int i;
    for (i=0; i<count; i++) {
        const InlineOperation *inlineOp = &inlineTable[i];

        ClassObject *clazz = dvmFindSystemClassNoInit(inlineOp->classDescriptor);
        if (clazz == NULL)
            return 0;

        char *methodType;
        Method *method = dvmFindDirectMethodByDescriptor(clazz, inlineOp->methodName, inlineOp->methodSignature);
        if (method == NULL)
        {
            method = dvmFindVirtualMethodByDescriptor(clazz, inlineOp->methodName, inlineOp->methodSignature);
            methodType = "virtual";
        } else {
            if (dvmIsStaticMethod(method))
                methodType = "static";
            else
                methodType = "direct";
        }

        if (method == NULL)
            return 0;

        fprintf(clientOut, "inline: %s %s->%s%s\n", methodType, method->clazz->descriptor, method->name, dexProtoGetMethodDescriptor(&method->prototype, &stringCache));
    }

    return 1;
}

int dumpVirtualMethods(char *classType, FILE *clientOut)
{
    ClassObject *clazz;
    if (classType[0] == '[')
        clazz = dvmFindArrayClass(classType, NULL);
    else
        clazz = dvmFindSystemClassNoInit(classType);


    if (clazz == NULL)
    {
        fprintf(clientOut, "err: could not find class %s\n", classType);
        return 0;
    }

    //interface classes don't have virtual methods, by definition. But it's possible
    //to call virtual methods defined on the Object class via an interface type
    if (dvmIsInterfaceClass(clazz))
    {
        clazz = dvmFindSystemClassNoInit("Ljava/lang/Object;");
        if (clazz == NULL)
        {
            fprintf(clientOut, "err: could not find class %s\n", classType);
            return 0;
        }
    }

    int i;
    for (i=0; i<clazz->vtableCount; i++)
    {
        Method *method = clazz->vtable[i];
        fprintf(clientOut, "vtable: %s%s\n", method->name,
                 dexProtoGetMethodDescriptor(&method->prototype, &stringCache));
    }
    return 1;
}

ClassObject *lookupSuperclass(char *classType)
{
    ClassObject *clazz = dvmFindSystemClassNoInit(classType);

    if (clazz == NULL)
        return NULL;

    return clazz->super;
}

/*
 * Parse arguments.  Most of it just gets passed through to the VM.  The
 * JNI spec defines a handful of standard arguments.
 */
int main(int argc, char* const argv[])
{
    const char* inputFileName;
    JavaVM* vm = NULL;
    JNIEnv* env = NULL;
    DvmDex* pDvmDex = NULL;
    DexClassLookup* pClassLookup;

    if (argc != 3) {
        fprintf(stderr, "deodexerant %s (Android %s)\n", VERSION, ANDROID_VERSION);
        fprintf(stderr, "usage: deodexerant <odex_file> <port>\n");
        return 1;
    }

    inputFileName = argv[1];

    struct stat inputInfo;

    if (stat(inputFileName, &inputInfo) != 0) {
        fprintf(stderr, "could not stat '%s' : %s\n", inputFileName, strerror(errno));
        return 1;
    }

    int odexFd = open(inputFileName, O_RDONLY);
    if (odexFd < 0) {
        fprintf(stderr, "Unable to open '%s': %s\n", inputFileName, strerror(errno));
        return 1;
    }

    int port = atoi(argv[2]);
    int socketFd = socket(AF_INET, SOCK_STREAM, 0);
    if (socketFd < 0)
    {
        fprintf(stderr, "Unable to open socket\n");
        return 1;
    }

    struct sockaddr_in serverAddress, clientAddress;
    bzero((char *)&serverAddress, sizeof(serverAddress));
    serverAddress.sin_family = AF_INET;
    serverAddress.sin_addr.s_addr = INADDR_ANY;
    serverAddress.sin_port = htons(port);
    if (bind(socketFd, (struct sockaddr *)&serverAddress, sizeof(serverAddress)) < 0)
    {
        fprintf(stderr, "Unable to bind socket\n");
        return 1;
    }

    const char* bcp = getenv("BOOTCLASSPATH");
    if (bcp == NULL) {
        fprintf(stderr, "BOOTCLASSPATH not set\n");
        return 1;
    }


    DexClassVerifyMode verifyMode = VERIFY_MODE_ALL;
    DexOptimizerMode dexOptMode = OPTIMIZE_MODE_VERIFIED;

    if (dvmPrepForDexOpt(bcp, dexOptMode, verifyMode,
            0) != 0)
    {
        fprintf(stderr, "VM init failed\n");
        return 1;
    }

    /*
     * Map the entire file (so we don't have to worry about page
     * alignment).  The expectation is that the output file contains
     * our DEX data plus room for a small header.
     */
    bool success;
    void* mapAddr;
    mapAddr = mmap(NULL, inputInfo.st_size, PROT_READ,
                MAP_PRIVATE, odexFd, 0);
    if (mapAddr == MAP_FAILED) {
        fprintf(stderr, "unable to mmap DEX cache: %s\n", strerror(errno));
        return 1;
    }

    if (dvmDexFileOpenPartial(mapAddr + *((int *)(mapAddr+8)), *((int *)(mapAddr+12)), &pDvmDex) != 0) {
        fprintf(stderr, "Unable to create DexFile\n");
        return 1;
    }

    pClassLookup = dexCreateClassLookup(pDvmDex->pDexFile);
    if (pClassLookup == NULL)
    {
        fprintf(stderr, "unable to create class lookup\n");
        return 1;
    }

    pDvmDex->pDexFile->pClassLookup = pClassLookup;

    if (!loadAllClasses(pDvmDex))
    {
        fprintf(stderr, "error while loading classes\n");
        return 1;
    }




    listen(socketFd, 1);

    int clientSocketLength = sizeof(clientAddress);
    int clientFd = accept(socketFd, (struct sockaddr *) &clientAddress, &clientSocketLength);
    if (clientFd < 0)
    {
        fprintf(stderr, "Unable to accept incomming connection\n");
        return 1;
    }

    FILE *clientIn = fdopen(clientFd, "r");
    if (clientIn == 0)
    {
        fprintf(stderr, "Unable to fdopen socket to get input stream\n");
        return 1;
    }

    FILE *clientOut = fdopen(dup(clientFd), "w");
    if (clientOut == 0)
    {
        fprintf(stderr, "Unable to fdopen socket to get output stream\n");
        return 1;
    }

    char *command = NULL;
    unsigned int len = 0;
    dexStringCacheInit(&stringCache);

    while ((command = fgetln(clientIn, &len)) != NULL) {
        while (len > 0 && (command[len-1] == '\r' || command[len-1] == '\n'))
            len--;
        char *buf = malloc(len+1);
        memcpy(buf, command, len);
        buf[len] = 0;

        //printf("%s\n", buf);

        char *cmd = strtok(buf, " ");
        if (cmd == NULL) {
            fprintf(clientOut, "err: error interpreting command\n");
            fflush(clientOut);
            continue;
        }

        switch (cmd[0])
        {
            case 'F' :
            {
                char *classType = strtok(NULL, " ");
                if (classType == NULL)
                {
                    fprintf(clientOut, "err: no classType for field lookup\n");
                    fflush(clientOut);
                    break;
                }

                if (!dumpFields(classType, clientOut))
                {
                    fprintf(clientOut, "err: error while dumping fields\n");
                    fflush(clientOut);
                    break;
                }

                fprintf(clientOut, "done\n");
                fflush(clientOut);
                break;
            }
            case 'I':
            {
                if (!dumpInlineMethods(clientOut))
                {
                    fprintf(clientOut, "err: inline method not found\n");
                    fflush(clientOut);
                    break;
                }

                fprintf(clientOut, "done\n");
                fflush(clientOut);
                break;
            }
            case 'V':
            {
                char *classType = strtok(NULL, " ");
                if (classType == NULL)
                {
                    fprintf(clientOut, "err: no classType for vtable dump\n");
                    fflush(clientOut);
                    break;
                }

                if (!dumpVirtualMethods(classType, clientOut)) {
                    fprintf(clientOut, "err: error encountered while dumping virtual methods\n");
                    fflush(clientOut);
                    break;
                }

                fprintf(clientOut, "done\n");
                fflush(clientOut);
                break;
            }
            case 'P':
            {
                char *classType = strtok(NULL, " ");
                if (classType == NULL)
                {
                    fprintf(clientOut, "err: no classType for superclass lookup\n");
                    fflush(clientOut);
                    break;
                }

                ClassObject *clazz = lookupSuperclass(classType);
                if (clazz == NULL)
                {
                    fprintf(clientOut, "class: \n");
                    fflush(clientOut);
                    break;
                }

                fprintf(clientOut, "class: %s\n", clazz->descriptor);
                fflush(clientOut);
                break;
            }
            case 'C':
            {
                char *classType1 = strtok(NULL, " ");
                if (classType1 == NULL)
                {
                    fprintf(clientOut, "err: no classType for common superclass lookup\n");
                    fflush(clientOut);
                    break;
                }

                ClassObject *clazz1;
                if (classType1[0] == '[')
                    clazz1 = dvmFindArrayClass(classType1, NULL);
                else
                    clazz1 = dvmFindSystemClassNoInit(classType1);

                if (clazz1 == NULL)
                {
                    fprintf(clientOut, "err: class %s could not be found for common superclass lookup. This can be caused if a library the odex depends on is not in the BOOTCLASSPATH environment variable\n", classType1);
                    fflush(clientOut);
                    break;
                }

                char *classType2 = strtok(NULL, " ");
                if (classType2 == NULL)
                {
                    fprintf(clientOut, "err: no classType for common superclass lookup\n");
                    fflush(clientOut);
                    break;
                }

                ClassObject *clazz2;
                if (classType2[0] == '[')
                    clazz2 = dvmFindArrayClass(classType2, NULL);
                else
                    clazz2 = dvmFindSystemClassNoInit(classType2);

                if (clazz2 == NULL)
                {
                    fprintf(clientOut, "err: class %s could not be found for common superclass lookup. This can be caused if a library the odex depends on is not in the BOOTCLASSPATH environment variable\n", classType2);
                    fflush(clientOut);
                    break;
                }

                ClassObject *clazz = findCommonSuperclass(clazz1, clazz2);
                fprintf(clientOut, "class: %s\n", clazz->descriptor);
                fflush(clientOut);
                break;
            }
            default:
                fprintf(clientOut, "err: not a valid command\n");
                fflush(clientOut);
        }

        /*gettimeofday(&tv, NULL);

        printf("end   %07d\n", tv.tv_usec);*/


    }

    return 0;
}

