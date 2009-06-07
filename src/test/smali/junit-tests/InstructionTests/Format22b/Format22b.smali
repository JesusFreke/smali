.class public LFormat22b;
.super Ljava/lang/Object;
.source "Format22b.smali"

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public test_add-int-lit8()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 5

    add-int/lit8 v1, v0, 23

    const v0, 28
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_add-sub-lit8()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 23

    rsub-int/lit8 v1, v0, 5

    const v0, -18
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_mul-int-lit8()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 5

    mul-int/lit8 v1, v0, 23

    const v0, 115
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_div-int-lit8()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 23

    div-int/lit8 v1, v0, 5

    const v0, 4
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_rem-int-lit8()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 23

    rem-int/lit8 v1, v0, 5

    const v0, 3
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_and-int-lit8()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 5

    and-int/lit8 v1, v0, 23

    const v0, 5
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_or-int-lit8()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 5

    or-int/lit8 v1, v0, 23

    const v0, 23
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_xor-int-lit8()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 5

    xor-int/lit8 v1, v0, 23

    const v0, 18
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_shl-int-lit8()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 23

    shl-int/lit8 v1, v0, 5

    const v0, 736
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_shr-int-lit8()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, -736

    shr-int/lit8 v1, v0, 5

    const v0, -23
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_ushr-int-lit8()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, -736

    ushr-int/lit8 v1, v0, 5

    const v0, 134217705
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method