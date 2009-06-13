.class public LFormat21h;
.super Ljava/lang/Object;
.source "Format21h.smali"

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public test_const-high16()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const/high16 v0, 0x3000

    const v1, 0x30000000
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_const-wide-high16()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide/high16 v0, 0x3000

    const-wide v2, 0x3000000000000000L
    invoke-static {v0, v1, v2, v3}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method