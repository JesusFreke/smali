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
#OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE

.class public LFormat52c;
.super Ljava/lang/Object;
.source "Format52c.smali"

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public test-instance-of-jumbo-success()V
    .registers 258
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-string v0, "test"

    new-instance v1, LStringWrapper;
    invoke-direct {v1, v0}, LStringWrapper;-><init>(Ljava/lang/String;)V

    move-object/16 v256, v1

    instance-of/jumbo v257, v256, Ljava/lang/Object;

    const v0, 1
    move/16 v256, v0

    invoke-static/range {v256 .. v257}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test-instance-of-jumbo-failure()V
    .registers 258
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-string v0, "test"

    new-instance v1, LStringWrapper;
    invoke-direct {v1, v0}, LStringWrapper;-><init>(Ljava/lang/String;)V

    move-object/16 v256, v1

    instance-of/jumbo v257, v256, Lzzz99999;

    const v0, 0
    move/16 v256, v0

    invoke-static/range {v256 .. v257}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test-new-array-jumbo()V
    .registers 258
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 1
    move/16 v256, v0

    new-array/jumbo v257, v256, [Lzzz99999;

    move-object/16 v1, v257

    array-length v2, v1

    invoke-static {v0, v2}, LAssert;->assertEquals(II)V
    return-void
.end method