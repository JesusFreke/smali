.class public LFormat23x;
.super Ljava/lang/Object;
.source "Format23x.smali"

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public test_cmpl-float()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 123.4f
    const v1, 234.5f

    cmpl-float v0, v0, v1

    const v1, -1

    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_cmpl-float-NaN()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    sget v0, Ljava/lang/Float;->NaN:F
    const v1, 234.5f

    cmpl-float v0, v0, v1

    const v1, -1

    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_cmpg-float()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 123.4f
    const v1, 234.5f

    cmpg-float v0, v0, v1

    const v1, -1

    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_cmpg-float-NaN()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    sget v0, Ljava/lang/Float;->NaN:F
    const v1, 234.5f

    cmpg-float v0, v0, v1

    const v1, 1

    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method


.method public test_cmpl-double()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 123.4
    const-wide v2, 234.5

    cmpl-double v0, v0, v2

    const v1, -1

    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_cmpl-double-NaN()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    sget-wide v0, Ljava/lang/Double;->NaN:D
    const-wide v2, 234.5

    cmpl-double v0, v0, v2

    const v1, -1

    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_cmpg-double()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 123.4
    const-wide v2, 234.5

    cmpg-double v0, v0, v2

    const v1, -1

    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_cmpg-double-NaN()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    sget-wide v0, Ljava/lang/Double;->NaN:D
    const-wide v2, 234.5

    cmpg-double v0, v0, v2

    const v1, 1

    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_cmp-long()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 0x100000000L
    const-wide v2, 0x200000000L

    cmp-long v0, v0, v2

    const v1, -1

    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void    
.end method


.method public test_aget__aput()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 3
    new-array v0, v0, [I

    const v1, 1
    const v2, 0
    aput v1, v0, v2

    const v1, 2
    const v2, 1
    aput v1, v0, v2

    const v1, 3
    const v2, 2
    aput v1, v0, v2


    const v2, 0
    aget v1, v0, v2
    const v2, 1
    invoke-static {v1, v2}, LAssert;->assertEquals(II)V

    const v2, 1
    aget v1, v0, v2
    const v2, 2
    invoke-static {v1, v2}, LAssert;->assertEquals(II)V

    const v2, 2
    aget v1, v0, v2
    const v2, 3
    invoke-static {v1, v2}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_aget-wide__aput-wide()V
    .registers 5
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 3
    new-array v0, v0, [J

    const-wide v1, 0x100000000L
    const v3, 0
    aput-wide v1, v0, v3

    const-wide v1, 0x100000001L
    const v3, 1
    aput-wide v1, v0, v3

    const-wide v1, 0x100000002L
    const v3, 2
    aput-wide v1, v0, v3


    const v3, 0
    aget-wide v1, v0, v3
    const-wide v3, 0x100000000L
    invoke-static {v1, v2, v3, v4}, Lorg/junit/Assert;->assertEquals(JJ)V

    const v3, 1
    aget-wide v1, v0, v3
    const-wide v3, 0x100000001L
    invoke-static {v1, v2, v3, v4}, Lorg/junit/Assert;->assertEquals(JJ)V


    const v3, 2
    aget-wide v1, v0, v3
    const-wide v3, 0x100000002L
    invoke-static {v1, v2, v3, v4}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_aget-object__aput-object()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 3
    new-array v0, v0, [Ljava/lang/String;

    const-string v1, "1"
    const v2, 0
    aput-object v1, v0, v2

    const-string v1, "2"
    const v2, 1
    aput-object v1, v0, v2

    const-string v1, "3"
    const v2, 2
    aput-object v1, v0, v2


    const v2, 0
    aget-object v1, v0, v2
    const-string v2, "1"
    invoke-static {v1, v2}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    const v2, 1
    aget-object v1, v0, v2
    const-string v2, "2"
    invoke-static {v1, v2}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    const v2, 2
    aget-object v1, v0, v2
    const-string v2, "3"
    invoke-static {v1, v2}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V
    return-void
.end method

.method public test_aget-boolean__aput-boolean()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 2
    new-array v0, v0, [Z

    const v1, 0
    const v2, 0
    aput-boolean v1, v0, v2

    const v1, 1
    const v2, 1
    aput-boolean v1, v0, v2


    const v2, 0
    aget-boolean v1, v0, v2
    invoke-static {v1}, Lorg/junit/Assert;->assertFalse(Z)V

    const v2, 1
    aget-boolean v1, v0, v2
    invoke-static {v1}, Lorg/junit/Assert;->assertTrue(Z)V
    return-void
.end method

.method public test_aget-byte__aput-byte()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 3
    new-array v0, v0, [B

    const v1, 1T
    const v2, 0
    aput-byte v1, v0, v2

    const v1, 2T
    const v2, 1
    aput-byte v1, v0, v2

    const v1, 3T
    const v2, 2
    aput-byte v1, v0, v2


    const v2, 0
    aget-byte v1, v0, v2
    const v2, 1T
    invoke-static {v1, v2}, LAssert;->assertEquals(II)V

    const v2, 1
    aget-byte v1, v0, v2
    const v2, 2T
    invoke-static {v1, v2}, LAssert;->assertEquals(II)V

    const v2, 2
    aget-byte v1, v0, v2
    const v2, 3T
    invoke-static {v1, v2}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_aget-char__aput-char()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 3
    new-array v0, v0, [C

    const v1, '1'
    const v2, 0
    aput-char v1, v0, v2

    const v1, '2'
    const v2, 1
    aput-char v1, v0, v2

    const v1, '3'
    const v2, 2
    aput-char v1, v0, v2


    const v2, 0
    aget-char v1, v0, v2
    const v2, '1'
    invoke-static {v1, v2}, LAssert;->assertEquals(II)V

    const v2, 1
    aget-char v1, v0, v2
    const v2, '2'
    invoke-static {v1, v2}, LAssert;->assertEquals(II)V

    const v2, 2
    aget-char v1, v0, v2
    const v2, '3'
    invoke-static {v1, v2}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_aget-short__aput-short()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 3
    new-array v0, v0, [S

    const v1, 1S
    const v2, 0
    aput-short v1, v0, v2

    const v1, 2S
    const v2, 1
    aput-short v1, v0, v2

    const v1, 3S
    const v2, 2
    aput-short v1, v0, v2


    const v2, 0
    aget-short v1, v0, v2
    const v2, 1S
    invoke-static {v1, v2}, LAssert;->assertEquals(II)V

    const v2, 1
    aget-short v1, v0, v2
    const v2, 2S
    invoke-static {v1, v2}, LAssert;->assertEquals(II)V

    const v2, 2
    aget-short v1, v0, v2
    const v2, 3S
    invoke-static {v1, v2}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_add-int()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 5
    const v1, 23

    add-int v2, v0, v1

    const/16 v1, 28
    invoke-static {v1,v2}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_sub-int()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 5
    const v1, 23

    sub-int v2, v0, v1

    const/16 v1, -18
    invoke-static {v1,v2}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_mul-int()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 5
    const v1, 23

    mul-int v2, v0, v1

    const/16 v1, 115
    invoke-static {v1,v2}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_div-int()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 5
    const v1, 23

    div-int v2, v1, v0

    const/16 v1, 4
    invoke-static {v1,v2}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_rem-int()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 5
    const v1, 23

    rem-int v2, v1, v0

    const/16 v1, 3
    invoke-static {v1,v2}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_and-int()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 5
    const v1, 23

    and-int v2, v0, v1

    const/16 v1, 5
    invoke-static {v1,v2}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_or-int()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 5
    const v1, 23

    or-int v2, v0, v1

    const/16 v1, 23
    invoke-static {v1,v2}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_xor-int()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 5
    const v1, 23

    xor-int v2, v0, v1

    const/16 v1, 18
    invoke-static {v1,v2}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_shl-int()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 23
    const v1, 5

    shl-int v2, v0, v1

    const/16 v1, 736
    invoke-static {v1,v2}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_shr-int()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, -736
    const v1, 5

    shr-int v2, v0, v1

    const/16 v1, -23
    invoke-static {v1,v2}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_ushr-int()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, -736
    const v1, 5

    ushr-int v2, v0, v1

    const v1, 134217705
    invoke-static {v1,v2}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_add-long()V
    .registers 6
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 5L
    const-wide v2, 23L

    add-long v4, v0, v2

    const-wide v2, 28
    invoke-static {v2, v3, v4, v5}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_sub-long()V
    .registers 6
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 5L
    const-wide v2, 23L

    sub-long v4, v0, v2

    const-wide v2, -18
    invoke-static {v2, v3, v4, v5}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_mul-long()V
    .registers 6
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 5L
    const-wide v2, 23L

    mul-long v4, v0, v2

    const-wide v2, 115
    invoke-static {v2, v3, v4, v5}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_div-long()V
    .registers 6
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 5L
    const-wide v2, 23L

    div-long v4, v2, v0

    const-wide v2, 4
    invoke-static {v2, v3, v4, v5}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_rem-long()V
    .registers 6
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 5L
    const-wide v2, 23L

    rem-long v4, v2, v0

    const-wide v2, 3
    invoke-static {v2, v3, v4, v5}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_and-long()V
    .registers 6
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 5L
    const-wide v2, 23L

    and-long v4, v0, v2

    const-wide v2, 5
    invoke-static {v2, v3, v4, v5}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_or-long()V
    .registers 6
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 5L
    const-wide v2, 23L

    or-long v4, v0, v2

    const-wide v2, 23
    invoke-static {v2, v3, v4, v5}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_xor-long()V
    .registers 6
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 5L
    const-wide v2, 23L

    xor-long v4, v0, v2

    const-wide v2, 18
    invoke-static {v2, v3, v4, v5}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_shl-long()V
    .registers 6
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 23L
    const v2, 5

    shl-long v4, v0, v2

    const-wide v2, 736
    invoke-static {v2, v3, v4, v5}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_shr-long()V
    .registers 6
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, -736L
    const v2, 5

    shr-long v4, v0, v2

    const-wide v2, -23
    invoke-static {v2, v3, v4, v5}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_ushr-long()V
    .registers 6
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, -736L
    const v2, 5

    ushr-long v4, v0, v2

    const-wide v2, 576460752303423465L
    invoke-static {v2, v3, v4, v5}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_add-float()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 3.14159f
    const v1, .11f

    add-float v2, v0, v1

    const v1, 3.25159f
    invoke-static {v1, v2}, LAssert;->assertEquals(FF)V
    return-void
.end method

.method public test_sub-float()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 3.14159f
    const v1, .11f

    sub-float v2, v0, v1

    const v1, 3.03159f
    invoke-static {v1, v2}, LAssert;->assertEquals(FF)V
    return-void
.end method

.method public test_mul-float()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 3.14159f
    const v1, .11f

    mul-float v2, v0, v1

    const v1, .3455749f
    invoke-static {v1, v2}, LAssert;->assertEquals(FF)V
    return-void
.end method

.method public test_div-float()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 3.14159f
    const v1, .11f

    div-float v2, v0, v1

    const v1, 28.55990909f
    invoke-static {v1, v2}, LAssert;->assertEquals(FF)V
    return-void
.end method

.method public test_rem-float()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 3.14159f
    const v1, .11f

    rem-float v2, v0, v1

    const v1, .06159999f
    invoke-static {v1, v2}, LAssert;->assertEquals(FF)V
    return-void
.end method

.method public test_add-double()V
    .registers 6
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 3.14159
    const-wide v2, .11

    add-double v4, v0, v2

    const-wide v2, 3.25159
    invoke-static {v2, v3, v4, v5}, LAssert;->assertEquals(DD)V
    return-void
.end method

.method public test_sub-double()V
    .registers 6
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 3.14159
    const-wide v2, .11

    sub-double v4, v0, v2

    const-wide v2, 3.03159
    invoke-static {v2, v3, v4, v5}, LAssert;->assertEquals(DD)V
    return-void
.end method

.method public test_mul-double()V
    .registers 6
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 3.14159
    const-wide v2, .11

    mul-double v4, v0, v2

    const-wide v2, .3455749
    invoke-static {v2, v3, v4, v5}, LAssert;->assertEquals(DD)V
    return-void
.end method

.method public test_div-double()V
    .registers 6
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 3.14159
    const-wide v2, .11

    div-double v4, v0, v2

    const-wide v2, 28.55990909
    invoke-static {v2, v3, v4, v5}, LAssert;->assertEquals(DD)V
    return-void
.end method

.method public test_rem-double()V
    .registers 6
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 3.14159
    const-wide v2, .11

    rem-double v4, v0, v2

    const-wide v2, .06159999
    invoke-static {v2, v3, v4, v5}, LAssert;->assertEquals(DD)V
    return-void
.end method