.class public LFormat31i;
.super Ljava/lang/Object;
.source "Format31i.smali"

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public test_const()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 0x10000000

    invoke-static {v0}, Ljava/lang/Integer;->toString(I)Ljava/lang/String;
    move-result-object v1

    const-string v0, "268435456"

    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V
    return-void
.end method

.method public test_const-wide-32()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide/32 v0, 0x10000000

    invoke-static {v0, v1}, Ljava/lang/Long;->toString(J)Ljava/lang/String;
    move-result-object v1

    const-string v0, "268435456"

    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V
    return-void
.end method