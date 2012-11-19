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

.class public LFormat52c_autofix;
.super Ljava/lang/Object;
.source "Format52c_autofix.smali"

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public test-iput-iget-jumbo()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    new-instance v2, LzzzInstanceFields;
    invoke-direct/range {v2}, LzzzInstanceFields;-><init>()V

    const v0, 23
    iput v0, v2, LzzzInstanceFields;->field99999:I

    iget v1, v2, LzzzInstanceFields;->field99999:I

    invoke-static/range {v0 .. v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test-iput-object-iget-object-jumbo()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    new-instance v2, LzzzInstanceFields;
    invoke-direct/range {v2}, LzzzInstanceFields;-><init>()V

    new-instance v0, Ljava/lang/Object;
    invoke-direct {v0}, Ljava/lang/Object;-><init>()V

    iput-object v0, v2, LzzzInstanceFields;->field99999Object:Ljava/lang/Object;

    iget-object v1, v2, LzzzInstanceFields;->field99999Object:Ljava/lang/Object;

    invoke-static/range {v0 .. v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V
    return-void
.end method

.method public test-iput-wide-iget-wide-jumbo()V
    .registers 5
    .annotation runtime Lorg/junit/Test;
    .end annotation

    new-instance v4, LzzzInstanceFields;
    invoke-direct/range {v4}, LzzzInstanceFields;-><init>()V

    const-wide v0, 0x200000000L

    iput-wide v0, v4, LzzzInstanceFields;->field99999Wide:J

    iget-wide v2, v4, LzzzInstanceFields;->field99999Wide:J

    invoke-static/range {v0 .. v3}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test-iput-boolean-iget-boolean-true-jumbo()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    new-instance v2, LzzzInstanceFields;
    invoke-direct/range {v2}, LzzzInstanceFields;-><init>()V

    const v0, 1

    iput-boolean v0, v2, LzzzInstanceFields;->field99999Boolean:Z

    iget-boolean v1, v2, LzzzInstanceFields;->field99999Boolean:Z

    invoke-static/range {v1}, Lorg/junit/Assert;->assertTrue(Z)V
    return-void
.end method

.method public test-iput-boolean-iget-boolean-false-jumbo()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    new-instance v2, LzzzInstanceFields;
    invoke-direct/range {v2}, LzzzInstanceFields;-><init>()V

    const v0, 0

    iput-boolean v0, v2, LzzzInstanceFields;->field99999Boolean:Z

    iget-boolean v1, v2, LzzzInstanceFields;->field99999Boolean:Z

    invoke-static/range {v1}, Lorg/junit/Assert;->assertFalse(Z)V
    return-void
.end method

.method public test-iput-byte-iget-byte-jumbo()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    new-instance v2, LzzzInstanceFields;
    invoke-direct/range {v2}, LzzzInstanceFields;-><init>()V

    const v0, 120T

    iput-byte v0, v2, LzzzInstanceFields;->field99999Byte:B

    iget-byte v1, v2, LzzzInstanceFields;->field99999Byte:B

    invoke-static/range {v0 .. v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test-iput-char-iget-char-jumbo()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    new-instance v2, LzzzInstanceFields;
    invoke-direct/range {v2}, LzzzInstanceFields;-><init>()V

    const v0, 'a'

    iput-char v0, v2, LzzzInstanceFields;->field99999Char:C

    iget-char v1, v2, LzzzInstanceFields;->field99999Char:C

    invoke-static/range {v0 .. v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test-iput-short-iget-short-jumbo()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    new-instance v2, LzzzInstanceFields;
    invoke-direct/range {v2}, LzzzInstanceFields;-><init>()V

    const v0, 1234S

    iput-short v0, v2, LzzzInstanceFields;->field99999Short:S

    iget-short v1, v2, LzzzInstanceFields;->field99999Short:S

    invoke-static/range {v0 .. v1}, LAssert;->assertEquals(II)V
    return-void
.end method