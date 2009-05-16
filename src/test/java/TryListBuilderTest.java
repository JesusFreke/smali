/*
 * [The "BSD licence"]
 * Copyright (c) 2009 Ben Gruver
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


import org.JesusFreke.dexlib.util.TryListBuilder;
import org.JesusFreke.dexlib.util.Pair;
import org.JesusFreke.dexlib.TypeIdItem;
import org.JesusFreke.dexlib.DexFile;
import org.JesusFreke.dexlib.CodeItem;
import org.junit.*;

import java.util.List;


public class TryListBuilderTest
{

    private static class Handler
    {
        public String type;
        public int handlerAddress;

        public Handler(String type, int handlerAddress) {
            this.type = type;
            this.handlerAddress = handlerAddress;
        }
    }

    public static void checkTry(CodeItem.TryItem tryItem,
                                   int startAddress,
                                   int endAddress,
                                   int catchAllAddress,
                                   Handler[] handlers) {

        Assert.assertTrue(tryItem.getStartAddress() == startAddress);
        Assert.assertTrue(tryItem.getEndAddress() == endAddress);

        CodeItem.EncodedCatchHandler encodedCatchHandler = tryItem.getHandler();

        Assert.assertTrue(encodedCatchHandler.getCatchAllAddress() == catchAllAddress);

        Assert.assertTrue(encodedCatchHandler.getHandlerCount() == handlers.length);

        for (int i=0; i<handlers.length; i++) {
            CodeItem.EncodedTypeAddrPair typeAddrPair = encodedCatchHandler.getHandler(i);
            Handler handler = handlers[i];

            Assert.assertTrue(typeAddrPair.getType().toString().compareTo(handler.type) == 0);
            Assert.assertTrue(typeAddrPair.getHandlerAddress() == handler.handlerAddress);
        }
    }

    @Test
    public void singleTryTest() {
        TryListBuilder tryListBuilder = new TryListBuilder();

        DexFile dexFile = DexFile.makeBlankDexFile();
        TypeIdItem typeIdItem = new TypeIdItem(dexFile, "Ljava/lang/Exception;");
        tryListBuilder.addHandler(typeIdItem, 2, 5, 100);

        Pair<List<CodeItem.TryItem>, List<CodeItem.EncodedCatchHandler>> retVal = tryListBuilder.encodeTries(dexFile);
        List<CodeItem.TryItem> tries = retVal.first;

        Assert.assertTrue(tries.size() == 1);
        checkTry(tries.get(0), 2, 5, -1, new Handler[]{new Handler("Ljava/lang/Exception;", 100)});
    }


    @Test
    public void singleTryWithCatchAllTest() {
        TryListBuilder tryListBuilder = new TryListBuilder();

        DexFile dexFile = DexFile.makeBlankDexFile();
        TypeIdItem typeIdItem = new TypeIdItem(dexFile, "Ljava/lang/Exception;");
        tryListBuilder.addHandler(typeIdItem, 2, 5, 100);

        tryListBuilder.addCatchAllHandler(2, 5, 101);

        Pair<List<CodeItem.TryItem>, List<CodeItem.EncodedCatchHandler>> retVal = tryListBuilder.encodeTries(dexFile);
        List<CodeItem.TryItem> tries = retVal.first;

        Assert.assertTrue(tries.size() == 1);
        checkTry(tries.get(0), 2, 5, 101, new Handler[]{new Handler("Ljava/lang/Exception;", 100)});
    }

    @Test
    public void twoTriesTest1() {
        //|-----|
        //      |-----|
        TryListBuilder tryListBuilder = new TryListBuilder();

        DexFile dexFile = DexFile.makeBlankDexFile();
        TypeIdItem typeIdItem = new TypeIdItem(dexFile, "Ljava/lang/Exception;");
        tryListBuilder.addHandler(typeIdItem, 2, 5, 100);

        tryListBuilder.addHandler(typeIdItem, 5, 10, 101);


        Pair<List<CodeItem.TryItem>, List<CodeItem.EncodedCatchHandler>> retVal = tryListBuilder.encodeTries(dexFile);
        List<CodeItem.TryItem> tries = retVal.first;

        Assert.assertTrue(tries.size() == 2);
        checkTry(tries.get(0), 2, 5, -1, new Handler[]{new Handler("Ljava/lang/Exception;", 100)});
        checkTry(tries.get(1), 5, 10, -1, new Handler[]{new Handler("Ljava/lang/Exception;", 101)});
    }

    @Test
    public void twoTriesTest2() {
        //|-----|
        //         |-----|
        TryListBuilder tryListBuilder = new TryListBuilder();

        DexFile dexFile = DexFile.makeBlankDexFile();
        TypeIdItem typeIdItem = new TypeIdItem(dexFile, "Ljava/lang/Exception;");
        tryListBuilder.addHandler(typeIdItem, 2, 5, 100);

        tryListBuilder.addHandler(typeIdItem, 10, 15, 101);


        Pair<List<CodeItem.TryItem>, List<CodeItem.EncodedCatchHandler>> retVal = tryListBuilder.encodeTries(dexFile);
        List<CodeItem.TryItem> tries = retVal.first;

        Assert.assertTrue(tries.size() == 2);
        checkTry(tries.get(0), 2, 5, -1, new Handler[]{new Handler("Ljava/lang/Exception;", 100)});
        checkTry(tries.get(1), 10, 15, -1, new Handler[]{new Handler("Ljava/lang/Exception;", 101)});
    }

    @Test
    public void twoTriesTest3() {
        //      |-----|
        //|-----|
        TryListBuilder tryListBuilder = new TryListBuilder();

        DexFile dexFile = DexFile.makeBlankDexFile();
        TypeIdItem typeIdItem = new TypeIdItem(dexFile, "Ljava/lang/Exception;");
        tryListBuilder.addHandler(typeIdItem, 5, 10, 101);
        tryListBuilder.addHandler(typeIdItem, 2, 5, 100);


        Pair<List<CodeItem.TryItem>, List<CodeItem.EncodedCatchHandler>> retVal = tryListBuilder.encodeTries(dexFile);
        List<CodeItem.TryItem> tries = retVal.first;

        Assert.assertTrue(tries.size() == 2);
        checkTry(tries.get(0), 2, 5, -1, new Handler[]{new Handler("Ljava/lang/Exception;", 100)});
        checkTry(tries.get(1), 5, 10, -1, new Handler[]{new Handler("Ljava/lang/Exception;", 101)});
    }

    @Test
    public void twoTriesTest4() {
        //         |-----|
        //|-----|
        TryListBuilder tryListBuilder = new TryListBuilder();

        DexFile dexFile = DexFile.makeBlankDexFile();
        TypeIdItem typeIdItem = new TypeIdItem(dexFile, "Ljava/lang/Exception;");

        tryListBuilder.addHandler(typeIdItem, 10, 15, 101);

        tryListBuilder.addHandler(typeIdItem, 2, 5, 100);


        Pair<List<CodeItem.TryItem>, List<CodeItem.EncodedCatchHandler>> retVal = tryListBuilder.encodeTries(dexFile);
        List<CodeItem.TryItem> tries = retVal.first;

        Assert.assertTrue(tries.size() == 2);
        checkTry(tries.get(0), 2, 5, -1, new Handler[]{new Handler("Ljava/lang/Exception;", 100)});
        checkTry(tries.get(1), 10, 15, -1, new Handler[]{new Handler("Ljava/lang/Exception;", 101)});
    }

    @Test
    public void twoTriesTest5() {
        //|-----|
        //|-----|
        TryListBuilder tryListBuilder = new TryListBuilder();

        DexFile dexFile = DexFile.makeBlankDexFile();
        TypeIdItem typeIdItem1 = new TypeIdItem(dexFile, "Ljava/lang/Exception1;");
        TypeIdItem typeIdItem2 = new TypeIdItem(dexFile, "Ljava/lang/Exception2;");

        tryListBuilder.addHandler(typeIdItem1, 2, 5, 100);
        tryListBuilder.addHandler(typeIdItem2, 2, 5, 101);

        Pair<List<CodeItem.TryItem>, List<CodeItem.EncodedCatchHandler>> retVal = tryListBuilder.encodeTries(dexFile);
        List<CodeItem.TryItem> tries = retVal.first;

        Assert.assertTrue(tries.size() == 1);
        checkTry(tries.get(0), 2, 5, -1, new Handler[]{new Handler("Ljava/lang/Exception1;", 100), new Handler("Ljava/lang/Exception2;", 101)});
    }

    @Test
    public void twoTriesTest6() {
        //|-----|
        //   |-----|
        TryListBuilder tryListBuilder = new TryListBuilder();

        DexFile dexFile = DexFile.makeBlankDexFile();
        TypeIdItem typeIdItem1 = new TypeIdItem(dexFile, "Ljava/lang/Exception1;");
        TypeIdItem typeIdItem2 = new TypeIdItem(dexFile, "Ljava/lang/Exception2;");

        tryListBuilder.addHandler(typeIdItem1, 2, 5, 100);
        tryListBuilder.addHandler(typeIdItem2, 4, 10, 101);


        Pair<List<CodeItem.TryItem>, List<CodeItem.EncodedCatchHandler>> retVal = tryListBuilder.encodeTries(dexFile);
        List<CodeItem.TryItem> tries = retVal.first;

        Assert.assertTrue(tries.size() == 3);
        checkTry(tries.get(0), 2, 4, -1, new Handler[]{new Handler("Ljava/lang/Exception1;", 100)});
        checkTry(tries.get(1), 4, 5, -1, new Handler[]{new Handler("Ljava/lang/Exception1;", 100), new Handler("Ljava/lang/Exception2;", 101)});
        checkTry(tries.get(2), 5, 10, -1, new Handler[]{new Handler("Ljava/lang/Exception2;", 101)});
    }


    @Test
    public void twoTriesTest7() {
        //   |-----|
        //|-----|
        TryListBuilder tryListBuilder = new TryListBuilder();

        DexFile dexFile = DexFile.makeBlankDexFile();
        TypeIdItem typeIdItem1 = new TypeIdItem(dexFile, "Ljava/lang/Exception1;");
        TypeIdItem typeIdItem2 = new TypeIdItem(dexFile, "Ljava/lang/Exception2;");

        tryListBuilder.addHandler(typeIdItem2, 4, 10, 101);
        tryListBuilder.addHandler(typeIdItem1, 2, 5, 100);        


        Pair<List<CodeItem.TryItem>, List<CodeItem.EncodedCatchHandler>> retVal = tryListBuilder.encodeTries(dexFile);
        List<CodeItem.TryItem> tries = retVal.first;

        Assert.assertTrue(tries.size() == 3);
        checkTry(tries.get(0), 2, 4, -1, new Handler[]{new Handler("Ljava/lang/Exception1;", 100)});
        checkTry(tries.get(1), 4, 5, -1, new Handler[]{new Handler("Ljava/lang/Exception2;", 101), new Handler("Ljava/lang/Exception1;", 100)});
        checkTry(tries.get(2), 5, 10, -1, new Handler[]{new Handler("Ljava/lang/Exception2;", 101)});
    }

    @Test
    public void twoTriesTest8() {
        //|-----|
        // |---|
        TryListBuilder tryListBuilder = new TryListBuilder();

        DexFile dexFile = DexFile.makeBlankDexFile();
        TypeIdItem typeIdItem1 = new TypeIdItem(dexFile, "Ljava/lang/Exception1;");
        TypeIdItem typeIdItem2 = new TypeIdItem(dexFile, "Ljava/lang/Exception2;");

        tryListBuilder.addHandler(typeIdItem1, 2, 10, 100);
        tryListBuilder.addHandler(typeIdItem2, 4, 6, 101);


        Pair<List<CodeItem.TryItem>, List<CodeItem.EncodedCatchHandler>> retVal = tryListBuilder.encodeTries(dexFile);
        List<CodeItem.TryItem> tries = retVal.first;

        Assert.assertTrue(tries.size() == 3);
        checkTry(tries.get(0), 2, 4, -1, new Handler[]{new Handler("Ljava/lang/Exception1;", 100)});
        checkTry(tries.get(1), 4, 6, -1, new Handler[]{new Handler("Ljava/lang/Exception1;", 100), new Handler("Ljava/lang/Exception2;", 101)});
        checkTry(tries.get(2), 6, 10, -1, new Handler[]{new Handler("Ljava/lang/Exception1;", 100)});
    }

    @Test
    public void twoTriesTest9() {
        // |---|
        //|-----|
        TryListBuilder tryListBuilder = new TryListBuilder();

        DexFile dexFile = DexFile.makeBlankDexFile();
        TypeIdItem typeIdItem1 = new TypeIdItem(dexFile, "Ljava/lang/Exception1;");
        TypeIdItem typeIdItem2 = new TypeIdItem(dexFile, "Ljava/lang/Exception2;");

        tryListBuilder.addHandler(typeIdItem2, 4, 6, 101);
        tryListBuilder.addHandler(typeIdItem1, 2, 10, 100);


        Pair<List<CodeItem.TryItem>, List<CodeItem.EncodedCatchHandler>> retVal = tryListBuilder.encodeTries(dexFile);
        List<CodeItem.TryItem> tries = retVal.first;

        Assert.assertTrue(tries.size() == 3);
        checkTry(tries.get(0), 2, 4, -1, new Handler[]{new Handler("Ljava/lang/Exception1;", 100)});
        checkTry(tries.get(1), 4, 6, -1, new Handler[]{new Handler("Ljava/lang/Exception2;", 101), new Handler("Ljava/lang/Exception1;", 100)});
        checkTry(tries.get(2), 6, 10, -1, new Handler[]{new Handler("Ljava/lang/Exception1;", 100)});
    }

    @Test
    public void twoTriesTest10() {
        //|-----|
        //|---|
        TryListBuilder tryListBuilder = new TryListBuilder();

        DexFile dexFile = DexFile.makeBlankDexFile();
        TypeIdItem typeIdItem1 = new TypeIdItem(dexFile, "Ljava/lang/Exception1;");
        TypeIdItem typeIdItem2 = new TypeIdItem(dexFile, "Ljava/lang/Exception2;");

        tryListBuilder.addHandler(typeIdItem1, 2, 10, 100);
        tryListBuilder.addHandler(typeIdItem2, 2, 6, 101);


        Pair<List<CodeItem.TryItem>, List<CodeItem.EncodedCatchHandler>> retVal = tryListBuilder.encodeTries(dexFile);
        List<CodeItem.TryItem> tries = retVal.first;

        Assert.assertTrue(tries.size() == 2);
        checkTry(tries.get(0), 2, 6, -1, new Handler[]{new Handler("Ljava/lang/Exception1;", 100), new Handler("Ljava/lang/Exception2;", 101)});
        checkTry(tries.get(1), 6, 10, -1, new Handler[]{new Handler("Ljava/lang/Exception1;", 100)});
    }

    @Test
    public void twoTriesTest11() {
        //|---|
        //|-----|
        TryListBuilder tryListBuilder = new TryListBuilder();

        DexFile dexFile = DexFile.makeBlankDexFile();
        TypeIdItem typeIdItem1 = new TypeIdItem(dexFile, "Ljava/lang/Exception1;");
        TypeIdItem typeIdItem2 = new TypeIdItem(dexFile, "Ljava/lang/Exception2;");

        tryListBuilder.addHandler(typeIdItem2, 2, 6, 101);
        tryListBuilder.addHandler(typeIdItem1, 2, 10, 100);


        Pair<List<CodeItem.TryItem>, List<CodeItem.EncodedCatchHandler>> retVal = tryListBuilder.encodeTries(dexFile);
        List<CodeItem.TryItem> tries = retVal.first;

        Assert.assertTrue(tries.size() == 2);
        checkTry(tries.get(0), 2, 6, -1, new Handler[]{new Handler("Ljava/lang/Exception2;", 101), new Handler("Ljava/lang/Exception1;", 100)});
        checkTry(tries.get(1), 6, 10, -1, new Handler[]{new Handler("Ljava/lang/Exception1;", 100)});
    }

    @Test
    public void twoTriesTest12() {
        //|-----|
        //  |---|
        TryListBuilder tryListBuilder = new TryListBuilder();

        DexFile dexFile = DexFile.makeBlankDexFile();
        TypeIdItem typeIdItem1 = new TypeIdItem(dexFile, "Ljava/lang/Exception1;");
        TypeIdItem typeIdItem2 = new TypeIdItem(dexFile, "Ljava/lang/Exception2;");

        tryListBuilder.addHandler(typeIdItem1, 2, 10, 100);
        tryListBuilder.addHandler(typeIdItem2, 6, 10, 101);


        Pair<List<CodeItem.TryItem>, List<CodeItem.EncodedCatchHandler>> retVal = tryListBuilder.encodeTries(dexFile);
        List<CodeItem.TryItem> tries = retVal.first;

        Assert.assertTrue(tries.size() == 2);
        checkTry(tries.get(0), 2, 6, -1, new Handler[]{new Handler("Ljava/lang/Exception1;", 100)});
        checkTry(tries.get(1), 6, 10, -1, new Handler[]{new Handler("Ljava/lang/Exception1;", 100), new Handler("Ljava/lang/Exception2;", 101)});
    }

    @Test
    public void twoTriesTest13() {
        //  |---|
        //|-----|
        TryListBuilder tryListBuilder = new TryListBuilder();

        DexFile dexFile = DexFile.makeBlankDexFile();
        TypeIdItem typeIdItem1 = new TypeIdItem(dexFile, "Ljava/lang/Exception1;");
        TypeIdItem typeIdItem2 = new TypeIdItem(dexFile, "Ljava/lang/Exception2;");

        tryListBuilder.addHandler(typeIdItem2, 6, 10, 101);
        tryListBuilder.addHandler(typeIdItem1, 2, 10, 100);


        Pair<List<CodeItem.TryItem>, List<CodeItem.EncodedCatchHandler>> retVal = tryListBuilder.encodeTries(dexFile);
        List<CodeItem.TryItem> tries = retVal.first;

        Assert.assertTrue(tries.size() == 2);
        checkTry(tries.get(0), 2, 6, -1, new Handler[]{new Handler("Ljava/lang/Exception1;", 100)});
        checkTry(tries.get(1), 6, 10, -1, new Handler[]{new Handler("Ljava/lang/Exception2;", 101), new Handler("Ljava/lang/Exception1;", 100)});
    }


    @Test
    public void threeTriesTest1() {
        //  |-----|
        //            |-----|
        //|--------------------|
        TryListBuilder tryListBuilder = new TryListBuilder();

        DexFile dexFile = DexFile.makeBlankDexFile();
        TypeIdItem typeIdItem1 = new TypeIdItem(dexFile, "Ljava/lang/Exception1;");
        TypeIdItem typeIdItem2 = new TypeIdItem(dexFile, "Ljava/lang/Exception2;");
        TypeIdItem typeIdItem3= new TypeIdItem(dexFile, "Ljava/lang/Exception3;");

        tryListBuilder.addHandler(typeIdItem1, 2, 4, 100);
        tryListBuilder.addHandler(typeIdItem2, 6, 10, 101);
        tryListBuilder.addHandler(typeIdItem3, 0, 12, 102);

        Pair<List<CodeItem.TryItem>, List<CodeItem.EncodedCatchHandler>> retVal = tryListBuilder.encodeTries(dexFile);
        List<CodeItem.TryItem> tries = retVal.first;

        Handler handler1 = new Handler("Ljava/lang/Exception1;", 100);
        Handler handler2 = new Handler("Ljava/lang/Exception2;", 101);
        Handler handler3 = new Handler("Ljava/lang/Exception3;", 102);

        Assert.assertTrue(tries.size() == 5);
        checkTry(tries.get(0), 0, 2, -1, new Handler[]{handler3});
        checkTry(tries.get(1), 2, 4, -1, new Handler[]{handler1, handler3});
        checkTry(tries.get(2), 4, 6, -1, new Handler[]{handler3});
        checkTry(tries.get(3), 6, 10, -1, new Handler[]{handler2, handler3});
        checkTry(tries.get(4), 10, 12, -1, new Handler[]{handler3});
    }

    @Test
    public void catchAllTest1() {
        //|-----|
        // |---|
        TryListBuilder tryListBuilder = new TryListBuilder();

        DexFile dexFile = DexFile.makeBlankDexFile();

        tryListBuilder.addCatchAllHandler(2, 8, 100);
        tryListBuilder.addCatchAllHandler(4, 6, 101);

        Pair<List<CodeItem.TryItem>, List<CodeItem.EncodedCatchHandler>> retVal = tryListBuilder.encodeTries(dexFile);
        List<CodeItem.TryItem> tries = retVal.first;

        Assert.assertTrue(tries.size() == 3);
        checkTry(tries.get(0), 2, 4, 100, new Handler[]{});
        checkTry(tries.get(1), 4, 6, 100, new Handler[]{});
        checkTry(tries.get(2), 6, 8, 100, new Handler[]{});
    }

    @Test
    public void catchAllTest2() {
        // |---|
        //|-----|
        TryListBuilder tryListBuilder = new TryListBuilder();

        DexFile dexFile = DexFile.makeBlankDexFile();

        tryListBuilder.addCatchAllHandler(4, 6, 100);
        tryListBuilder.addCatchAllHandler(2, 8, 101);

        Pair<List<CodeItem.TryItem>, List<CodeItem.EncodedCatchHandler>> retVal = tryListBuilder.encodeTries(dexFile);
        List<CodeItem.TryItem> tries = retVal.first;

        Assert.assertTrue(tries.size() == 3);
        checkTry(tries.get(0), 2, 4, 101, new Handler[]{});
        checkTry(tries.get(1), 4, 6, 100, new Handler[]{});
        checkTry(tries.get(2), 6, 8, 101, new Handler[]{});
    }



}
