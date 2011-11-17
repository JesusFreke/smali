.class public LAssert;
.super Ljava/lang/Object;
.source "Assert.smali"

#junit's Assert doesn't have an AssertEquals method for ints, only longs
.method public static assertEquals(II)V
    .registers 4

    int-to-long v0, p1
    int-to-long p0, p0

    invoke-static {v0, v1, p0, p1}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

#junit's Assert doesn't have an AssertEquals method for floats, only doubles
.method public static assertEquals(FF)V
    .registers 6

    float-to-double v0, p0
    float-to-double v2, p1

    const-wide v4, .00001

    invoke-static/range {v0..v5}, Lorg/junit/Assert;->assertEquals(DDD)V
    return-void
.end method

#convenience method that supplies a default "Delta" argument
.method public static assertEquals(DD)V
    .registers 6

    move-wide v0, p0
    move-wide v2, p2

    const-wide v4, .00001

    invoke-static/range {v0..v5}, Lorg/junit/Assert;->assertEquals(DDD)V

    return-void
.end method