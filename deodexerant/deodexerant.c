/*
 * Copyright 2011, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <stdio.h>
#include <dlfcn.h>

typedef struct InlineOperation {
    void *          func;
    const char*     classDescriptor;
    const char*     methodName;
    const char*     methodSignature;
} InlineOperation;

typedef const InlineOperation* (*dvmGetInlineOpsTablePtr)();
typedef int (*dvmGetInlineOpsTableLengthPtr)();

void main(int argc, char **argv) {
	int i;

	void *libdvm = dlopen("libdvm.so", RTLD_LAZY);

	if (libdvm == NULL) {
		printf("Failed to load libdvm: %s\n", dlerror());
		return;
	}

	dvmGetInlineOpsTablePtr dvmGetInlineOpsTable = dlsym(libdvm, "dvmGetInlineOpsTable");

	if (dvmGetInlineOpsTable == NULL) {
		// clear the error, and retry with the c++ mangled name
		dlerror();
		dvmGetInlineOpsTable = dlsym(libdvm, "_Z20dvmGetInlineOpsTablev");
	}

	if (dvmGetInlineOpsTable == NULL) {
		printf("Failed to load dvmGetInlineOpsTable: %s\n", dlerror());
		dlclose(libdvm);
		return;
	}

	dvmGetInlineOpsTableLengthPtr dvmGetInlineOpsTableLength = dlsym(libdvm, "dvmGetInlineOpsTableLength");

	if (dvmGetInlineOpsTableLength == NULL) {
		// clear the error, and retry with the c++ mangled name
		dlerror();
		dvmGetInlineOpsTableLength = dlsym(libdvm, "_Z26dvmGetInlineOpsTableLengthv");
	}

	if (dvmGetInlineOpsTableLength == NULL) {
		printf("Failed to load dvmGetInlineOpsTableLength: %s\n", dlerror());
		dlclose(libdvm);
		return;
	}

	const InlineOperation *inlineTable = dvmGetInlineOpsTable();
	int length = dvmGetInlineOpsTableLength();

	for (i=0; i<length; i++) {
		InlineOperation *item = &inlineTable[i];

		printf("%s->%s%s\n", item->classDescriptor, item->methodName, item->methodSignature);
	}

	dlclose(libdvm);
	return;
}