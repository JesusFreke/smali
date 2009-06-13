.class public LFormat51l;
.super Ljava/lang/Object;
.source "Format51l.smali"

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public test_const-wide()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 6

    const-wide/16 v2, 6

    invoke-static {v0, v1, v2, v3}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method