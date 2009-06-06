.class public LFormat21s;
.super Ljava/lang/Object;
.source "Format21s.smali"

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public test_const-16()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const/16 v0, 15000

    const v1, 15000
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_const-wide-16()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide/16 v0, 15000

    const-wide v2, 15000
    invoke-static {v0, v1, v2, v3}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method