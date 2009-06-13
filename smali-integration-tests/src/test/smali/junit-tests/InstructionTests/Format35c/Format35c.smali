.class public LFormat35c;
.super LMethodsSuper;
.source "Format35c.smali"

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, LMethodsSuper;-><init>()V
    return-void
.end method

.method public superMethodTest()I
    .registers 1
    const v0, 123
    return v0
.end method

.method private directMethodTest()I
    .registers 1
    const v0, 23
    return v0
.end method


.method public test_invoke-virtual()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    new-instance v0, LMethods;
    invoke-direct {v0}, LMethods;-><init>()V

    invoke-virtual {v0}, LMethods;->virtualMethodTest()I
    move-result v0

    const v1, 23
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_invoke-super()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    invoke-super {p0}, LFormat35c;->superMethodTest()I
    move-result v0

    const v1, 23
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_invoke-direct()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    invoke-direct {p0}, LFormat35c;->directMethodTest()I
    move-result v0

    const v1, 23
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_invoke-static()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    invoke-static {}, LMethods;->staticMethodTest()I
    move-result v0

    const v1, 23
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_invoke-interface()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    new-instance v0, LMethods;
    invoke-direct {v0}, LMethods;-><init>()V

    invoke-interface {v0}, LMethodsInterface;->interfaceMethodTest()I
    move-result v0

    const v1, 23
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_filled-new-array()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 10
    const v1, 20
    const v2, 30

    filled-new-array {v0, v1, v2}, [I
    move-result-object v0

    const v1, 0
    aget v2, v0, v1
    const v1, 10
    invoke-static {v1, v2}, LAssert;->assertEquals(II)V

    const v1, 1
    aget v2, v0, v1
    const v1, 20
    invoke-static {v1, v2}, LAssert;->assertEquals(II)V

    const v1, 2
    aget v2, v0, v1
    const v1, 30
    invoke-static {v1, v2}, LAssert;->assertEquals(II)V

    return-void
.end method

