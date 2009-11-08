.class public LFormat31t;
.super Ljava/lang/Object;
.source "Format31t.smali"

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public test_fill-array-data()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 6
    new-array v0, v0, [I
    fill-array-data v0, :ArrayData

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

:ArrayData
    .array-data 4
        1 2 3 4 5 6
    .end array-data
.end method

.method public test_packed-switch()V
    .registers 1
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 12

:switch
    packed-switch v0, :PackedSwitch

:Label10
    invoke-static {}, Lorg/junit/Assert;->fail()V
    return-void

:Label11
    invoke-static {}, Lorg/junit/Assert;->fail()V
    return-void

:Label12
    return-void

:Label13
    invoke-static {}, Lorg/junit/Assert;->fail()V
    return-void

:PackedSwitch
    .packed-switch 10
        :Label10
        :Label11
        :Label12
        :Label13
    .end packed-switch
.end method


.method public test_sparse-switch()V
    .registers 1
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 13

:switch
    sparse-switch v0, :SparseSwitch

:Label10
    invoke-static {}, Lorg/junit/Assert;->fail()V
    return-void

:Label20
    invoke-static {}, Lorg/junit/Assert;->fail()V
    return-void

:Label15
    invoke-static {}, Lorg/junit/Assert;->fail()V
    return-void

:Label13
    return-void

:Label99
    invoke-static {}, Lorg/junit/Assert;->fail()V
    return-void

:SparseSwitch
    .sparse-switch
        10 -> :Label10
        13 -> :Label13
        15 -> :Label15
        20 -> :Label20
        99 -> :Label99
    .end sparse-switch
.end method

