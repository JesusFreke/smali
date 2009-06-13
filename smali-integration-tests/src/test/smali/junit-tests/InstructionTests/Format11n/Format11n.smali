.class public LFormat11n;
.super Ljava/lang/Object;
.source "Format11n.smali"

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public test_const-4()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const/4 v0, 6

    const v1, 6
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method