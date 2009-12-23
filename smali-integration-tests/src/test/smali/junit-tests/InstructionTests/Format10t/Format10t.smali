.class public LFormat10t;
.super Ljava/lang/Object;
.source "Format10t.smali"

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public test_goto()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation


    const v0, 5

    goto :label

    :label2


    const v1, 6
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void



    :label
    const v0, 6
    goto :label2


    invoke-static {}, Lorg/junit/Assert;->fail()V
    return-void
.end method