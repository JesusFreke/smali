.class public LFormat22s;
.super Ljava/lang/Object;
.source "Format22s.smali"

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

    add-int/lit16 v1, v0, 23

    const v0, 28
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_add-sub-lit16()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 23

    rsub-int v1, v0, 5

    const v0, -18
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_mul-int-lit16()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 5

    mul-int/lit16 v1, v0, 23

    const v0, 115
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_div-int-lit16()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 23

    div-int/lit16 v1, v0, 5

    const v0, 4
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_rem-int-lit16()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 23

    rem-int/lit16 v1, v0, 5

    const v0, 3
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_and-int-lit16()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 5

    and-int/lit16 v1, v0, 23

    const v0, 5
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_or-int-lit16()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 5

    or-int/lit16 v1, v0, 23

    const v0, 23
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_xor-int-lit16()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 5

    xor-int/lit16 v1, v0, 23

    const v0, 18
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method