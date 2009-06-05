.class public LFormat12x;
.super Ljava/lang/Object;
.source "Format12x.smali"

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public test_move()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const/4 v0, 6
    move v1, v0

    invoke-static {v0, v1}, LAssert;->assertEquals(II)V

    return-void
.end method

.method public test_move-wide()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 0x200000000L

    move-wide v2, v0

    invoke-static {v0, v1, v2, v3}, Lorg/junit/Assert;->assertEquals(JJ)V

    return-void
.end method

.method public test_move-object()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-string v0, "random string value"

    move-object v1, v0

    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    return-void
.end method

.method public test_array-length()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const/4 v0, 3
    new-array v1, v0, [I

    array-length v2, v1

    invoke-static {v0, v2}, LAssert;->assertEquals(II)V

    return-void
.end method

.method public test_neg-int()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const/16 v0, 23
    neg-int v1, v0

    const/16 v2, -23
    invoke-static {v1, v2}, LAssert;->assertEquals(II)V
    return-void
.end method

 .method public test_not-int()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const/16 v0, 23
    not-int v1, v0

    const/16 v2, -24
    invoke-static {v1, v2}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_neg-long()V
    .registers 6
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 0x200000000L
    neg-long v2, v0

    const-wide v4, -0x200000000L
    invoke-static {v2, v3, v4, v5}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_not-long()V
    .registers 6
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 0x200000000L
    not-long v2, v0

    const-wide v4, -0x200000001L
    invoke-static {v2, v3, v4, v5}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_neg-float()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 3.14159f
    neg-float v1, v0

    const v2, -3.14159f
    invoke-static {v1, v2}, LAssert;->assertEquals(FF)V
    return-void
.end method

.method public test_neg-double()V
    .registers 6
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 3.14159
    neg-double v2, v0

    const-wide v4, -3.14159
    invoke-static {v2, v3, v4, v5}, LAssert;->assertEquals(DD)V
    return-void
.end method

.method public test_int-to-long()V
    .registers 5
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 23
    int-to-long v1, v0

    const-wide v3, 23L
    invoke-static {v1, v2, v3, v4}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_int-to-float()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 23
    int-to-float v1, v0

    const v2, 23f
    invoke-static {v1, v2}, LAssert;->assertEquals(FF)V
    return-void
.end method

.method public test_int-to-double()V
    .registers 5
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 23
    int-to-double v1, v0

    const-wide v3, 23.0
    invoke-static {v1, v2, v3, v4}, LAssert;->assertEquals(DD)V
    return-void
.end method

.method public test_long-to-int()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 23L
    long-to-int v2, v0

    const v3, 23
    invoke-static {v2, v3}, LAssert;->assertEquals(II)V
    return-void
.end method


.method public test_long-to-float()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 0x200000000L
    long-to-float v2, v0

    const v3, 8.589934592e9f
    invoke-static {v2, v3}, LAssert;->assertEquals(FF)V
    return-void
.end method

.method public test_long-to-double()V
    .registers 6
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 0x200000000L
    long-to-double v2, v0

    const-wide v4, 8.589934592e9
    invoke-static {v2, v3, v4, v5}, LAssert;->assertEquals(DD)V
    return-void
.end method

.method public test_float-to-int()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 3.14159f
    float-to-int v1, v0

    const v2, 3
    invoke-static {v1, v2}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_float-to-long()V
    .registers 5
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 3.14159f
    float-to-long v1, v0

    const-wide v3, 3L
    invoke-static {v1, v2, v3, v4}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_float-to-double()V
    .registers 5
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 3.14159f
    float-to-double v1, v0

    const-wide v3, 3.14159
    invoke-static {v1, v2, v3, v4}, LAssert;->assertEquals(DD)V
    return-void
.end method

.method public test_double-to-int()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 3.1415926535897932384626433832795028841971
    double-to-int v2, v0

    const v3, 3
    invoke-static {v2, v3}, LAssert;->assertEquals(II)V
    return-void
.end method


.method public test_double-to-long()V
    .registers 6
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 3.14159265358e11
    double-to-long v2, v0

    const-wide v4, 314159265358L
    invoke-static {v2, v3, v4, v5}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_double-to-float()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 3.1415926535897932384626433832795028841971
    double-to-float v2, v0

    const v3, 3.141592653589f
    invoke-static {v2, v3}, LAssert;->assertEquals(FF)V
    return-void
.end method

.method public test_int-to-byte()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 0x10010

    int-to-byte v1, v0

    const/16 v2, 0x10
    invoke-static {v1,v2}, LAssert;->assertEquals(II)V
    return-void
.end method


.method public test_int-to-char()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 0x11234

    int-to-char v1, v0

    const/16 v2, '\u1234'
    invoke-static {v1,v2}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_int-to-short()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 0x11234

    int-to-short v1, v0

    const/16 v2, 0x1234
    invoke-static {v1,v2}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_add-int-2addr()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const/4 v0,5
    const/16 v1, 23

    add-int/2addr v0, v1

    const/16 v1, 28
    invoke-static {v0,v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_sub-int-2addr()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const/4 v0,5
    const/16 v1, 23

    sub-int/2addr v0, v1

    const/16 v1, -18
    invoke-static {v0,v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_mul-int-2addr()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const/4 v0,5
    const/16 v1, 23

    mul-int/2addr v0, v1

    const/16 v1, 115
    invoke-static {v0,v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_div-int-2addr()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const/4 v0,5
    const/16 v1, 23

    div-int/2addr v1, v0

    const/16 v0, 4
    invoke-static {v0,v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_rem-int-2addr()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const/4 v0,5
    const/16 v1, 23

    rem-int/2addr v1, v0

    const/16 v0, 3
    invoke-static {v0,v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_and-int-2addr()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const/4 v0,5
    const/16 v1, 23

    and-int/2addr v0, v1

    const/16 v1, 5
    invoke-static {v0,v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_or-int-2addr()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const/4 v0,5
    const/16 v1, 23

    or-int/2addr v0, v1

    const/16 v1, 23
    invoke-static {v0,v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_xor-int-2addr()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const/4 v0,5
    const/16 v1, 23

    xor-int/2addr v0, v1

    const/16 v1, 18
    invoke-static {v0,v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_shl-int-2addr()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const/16 v0,23
    const/4 v1, 5

    shl-int/2addr v0, v1

    const/16 v1, 736
    invoke-static {v0,v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_shr-int-2addr()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const/16 v0, -736
    const/4 v1, 5

    shr-int/2addr v0, v1

    const/16 v1, -23
    invoke-static {v0,v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_ushr-int-2addr()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const/16 v0, -736
    const/4 v1, 5

    ushr-int/2addr v0, v1

    const v1, 134217705
    invoke-static {v0,v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_add-long-2addr()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 5L
    const-wide v2, 23L

    add-long/2addr v0, v2

    const-wide v2, 28
    invoke-static {v0, v1, v2, v3}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_sub-long-2addr()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0,5
    const-wide v2, 23

    sub-long/2addr v0, v2

    const-wide v2, -18
    invoke-static {v0, v1, v2, v3}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_mul-long-2addr()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0,5
    const-wide v2, 23

    mul-long/2addr v0, v2

    const-wide v2, 115
    invoke-static {v0, v1, v2, v3}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_div-long-2addr()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0,5
    const-wide v2, 23

    div-long/2addr v2, v0

    const-wide v0, 4
    invoke-static {v0, v1, v2, v3}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_rem-long-2addr()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0,5
    const-wide v2, 23

    rem-long/2addr v2, v0

    const-wide v0, 3
    invoke-static {v0, v1, v2, v3}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_and-long-2addr()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0,5
    const-wide v2, 23

    and-long/2addr v0, v2

    const-wide v2, 5
    invoke-static {v0, v1, v2, v3}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_or-long-2addr()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0,5
    const-wide v2, 23

    or-long/2addr v0, v2

    const-wide v2, 23
    invoke-static {v0, v1, v2, v3}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_xor-long-2addr()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0,5
    const-wide v2, 23

    xor-long/2addr v0, v2

    const-wide v2, 18
    invoke-static {v0, v1, v2, v3}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_shl-long-2addr()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 23
    const v2, 5

    shl-long/2addr v0, v2

    const-wide v2, 736
    invoke-static {v0, v1, v2, v3}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_shr-long-2addr()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, -736
    const v2, 5

    shr-long/2addr v0, v2

    const-wide v2, -23
    invoke-static {v0, v1, v2, v3}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_ushr-long-2addr()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, -736
    const v2, 5

    ushr-long/2addr v0, v2

    const-wide v2, 576460752303423465L
    invoke-static {v0, v1, v2, v3}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_add-float-2addr()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 3.14159f
    const v1, .11f

    add-float/2addr v0, v1

    const v1, 3.25159f
    invoke-static {v0, v1}, LAssert;->assertEquals(FF)V
    return-void
.end method

.method public test_sub-float-2addr()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 3.14159f
    const v1, .11f

    sub-float/2addr v0, v1

    const v1, 3.03159f
    invoke-static {v0, v1}, LAssert;->assertEquals(FF)V
    return-void
.end method

.method public test_mul-float-2addr()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 3.14159f
    const v1, .11f

    mul-float/2addr v0, v1

    const v1, .3455749f
    invoke-static {v0, v1}, LAssert;->assertEquals(FF)V
    return-void
.end method

.method public test_div-float-2addr()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 3.14159f
    const v1, .11f

    div-float/2addr v0, v1

    const v1, 28.55990909f
    invoke-static {v0, v1}, LAssert;->assertEquals(FF)V
    return-void
.end method

.method public test_rem-float-2addr()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 3.14159f
    const v1, .11f

    rem-float/2addr v0, v1

    const v1, .06159999f
    invoke-static {v0, v1}, LAssert;->assertEquals(FF)V
    return-void
.end method

.method public test_add-double-2addr()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 3.14159
    const-wide v2, .11

    add-double/2addr v0, v2

    const-wide v2, 3.25159
    invoke-static {v0, v1, v2, v3}, LAssert;->assertEquals(DD)V
    return-void
.end method

.method public test_sub-double-2addr()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 3.14159
    const-wide v2, .11

    sub-double/2addr v0, v2

    const-wide v2, 3.03159
    invoke-static {v0, v1, v2, v3}, LAssert;->assertEquals(DD)V
    return-void
.end method

.method public test_mul-double-2addr()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 3.14159
    const-wide v2, .11

    mul-double/2addr v0, v2

    const-wide v2, .3455749
    invoke-static {v0, v1, v2, v3}, LAssert;->assertEquals(DD)V
    return-void
.end method

.method public test_div-double-2addr()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 3.14159
    const-wide v2, .11

    div-double/2addr v0, v2

    const-wide v2, 28.55990909
    invoke-static {v0, v1, v2, v3}, LAssert;->assertEquals(DD)V
    return-void
.end method

.method public test_rem-double-2addr()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 3.14159
    const-wide v2, .11

    rem-double/2addr v0, v2

    const-wide v2, .06159999
    invoke-static {v0, v1, v2, v3}, LAssert;->assertEquals(DD)V
    return-void
.end method