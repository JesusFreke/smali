#Copyright 2011, Google Inc.
#All rights reserved.
#
#Redistribution and use in source and binary forms, with or without
#modification, are permitted provided that the following conditions are
#met:
#
#    * Redistributions of source code must retain the above copyright
#notice, this list of conditions and the following disclaimer.
#    * Redistributions in binary form must reproduce the above
#copyright notice, this list of conditions and the following disclaimer
#in the documentation and/or other materials provided with the
#distribution.
#    * Neither the name of Google Inc. nor the names of its
#contributors may be used to endorse or promote products derived from
#this software without specific prior written permission.
#
#THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
#"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
#LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
#A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
#OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
#SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
#LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
#DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
#THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
#(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
#OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

.class public LFormat5rc_autofix;
.super LzzzRangeMethodsSuper_autofix;
.source "Format5rc_autofix.smali"

.method public constructor <init>()V
    .registers 1
    invoke-direct/range {p0}, LzzzRangeMethodsSuper_autofix;-><init>()V
    return-void
.end method

.method public superMethodTest(IIIIII)I
    .registers 7

    add-int v0, v1, v2
    add-int v0, v0, v3
    add-int v0, v0, v4
    add-int v0, v0, v5
    add-int v0, v0, v6

    #add something extra, to make the test fail if this method is called instead of the super's method
    const v1, 1
    add-int v0, v0, v1

    return v0
.end method

.method private directMethodTest(IIIIII)I
    .registers 7

    add-int v0, v1, v2
    add-int v0, v0, v3
    add-int v0, v0, v4
    add-int v0, v0, v5
    add-int v0, v0, v6

    return v0
.end method


.method public test_invoke-virtual-range()V
    .registers 7
    .annotation runtime Lorg/junit/Test;
    .end annotation

    new-instance v0, LzzzRangeMethods_autofix;
    invoke-direct/range {v0}, LzzzRangeMethods_autofix;-><init>()V

    const v1, 1
    const v2, 2
    const v3, 3
    const v4, 4
    const v5, 5
    const v6, 6

    invoke-virtual/range {v0 .. v6}, LzzzRangeMethods_autofix;->virtualMethodTest(IIIIII)I
    move-result v0

    const v1, 21
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_invoke-super-range()V
    .registers 7
    .annotation runtime Lorg/junit/Test;
    .end annotation

    move-object v0, p0
    const v1, 1
    const v2, 2
    const v3, 3
    const v4, 4
    const v5, 5
    const v6, 6

    invoke-super/range {v0 .. v6}, LFormat5rc_autofix;->superMethodTest(IIIIII)I
    move-result v0

    const v1, 21
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_invoke-direct-range()V
    .registers 7
    .annotation runtime Lorg/junit/Test;
    .end annotation

    move-object v0, p0
    const v1, 1
    const v2, 2
    const v3, 3
    const v4, 4
    const v5, 5
    const v6, 6

    invoke-direct/range {v0 .. v6}, LFormat5rc_autofix;->directMethodTest(IIIIII)I
    move-result v0

    const v1, 21
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_invoke-static-range()V
    .registers 7
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v1, 1
    const v2, 2
    const v3, 3
    const v4, 4
    const v5, 5
    const v6, 6

    invoke-static/range {v1 .. v6}, LzzzRangeMethods_autofix;->staticMethodTest(IIIIII)I
    move-result v0

    const v1, 21
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_invoke-interface-range()V
    .registers 7
    .annotation runtime Lorg/junit/Test;
    .end annotation

    move-object v0, p0
    const v1, 1
    const v2, 2
    const v3, 3
    const v4, 4
    const v5, 5
    const v6, 6

    new-instance v0, LzzzRangeMethods_autofix;
    invoke-direct/range {v0}, LzzzRangeMethods_autofix;-><init>()V

    invoke-interface/range {v0 .. v6}, LzzzRangeMethodsInterface_autofix;->interfaceMethodTest(IIIIII)I
    move-result v0

    const v1, 21
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method
