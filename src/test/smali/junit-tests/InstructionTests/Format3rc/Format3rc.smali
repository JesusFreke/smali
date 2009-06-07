.class public LFormat3rc;
.super LRangeMethodsSuper;
.source "Format3rc.smali"

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, LRangeMethodsSuper;-><init>()V
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

    new-instance v0, LRangeMethods;
    invoke-direct {v0}, LRangeMethods;-><init>()V

    const v1, 1
    const v2, 2
    const v3, 3
    const v4, 4
    const v5, 5
    const v6, 6

    invoke-virtual/range {v0 .. v6}, LRangeMethods;->virtualMethodTest(IIIIII)I
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

    invoke-super/range {v0 .. v6}, LFormat3rc;->superMethodTest(IIIIII)I
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

    invoke-direct/range {v0 .. v6}, LFormat3rc;->directMethodTest(IIIIII)I
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

    invoke-static/range {v1 .. v6}, LRangeMethods;->staticMethodTest(IIIIII)I
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

    new-instance v0, LRangeMethods;
    invoke-direct {v0}, LRangeMethods;-><init>()V

    invoke-interface/range {v0 .. v6}, LRangeMethodsInterface;->interfaceMethodTest(IIIIII)I
    move-result v0

    const v1, 21
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_filled-new-array-range()V
    .registers 6
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 1
    const v1, 2
    const v2, 3
    const v3, 4
    const v4, 5
    const v5, 6


    filled-new-array/range {v0 .. v5}, [I
    move-result-object v0

    const v1, 0
    aget v2, v0, v1
    const v1, 1
    invoke-static {v1, v2}, LAssert;->assertEquals(II)V

    const v1, 1
    aget v2, v0, v1
    const v1, 2
    invoke-static {v1, v2}, LAssert;->assertEquals(II)V

    const v1, 2
    aget v2, v0, v1
    const v1, 3
    invoke-static {v1, v2}, LAssert;->assertEquals(II)V

    const v1, 3
    aget v2, v0, v1
    const v1, 4
    invoke-static {v1, v2}, LAssert;->assertEquals(II)V

    const v1, 4
    aget v2, v0, v1
    const v1, 5
    invoke-static {v1, v2}, LAssert;->assertEquals(II)V

    const v1, 5
    aget v2, v0, v1
    const v1, 6
    invoke-static {v1, v2}, LAssert;->assertEquals(II)V

    return-void
.end method

