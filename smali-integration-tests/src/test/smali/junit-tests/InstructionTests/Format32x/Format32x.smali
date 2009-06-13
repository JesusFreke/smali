.class public LFormat32x;
.super Ljava/lang/Object;
.source "Format32x.smali"

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public test_move-16()V
    .registers 500
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 123
    move/16 v323, v0
    move/16 v400, v323
    move/16 v1, v400

    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_move-wide-16()V
    .registers 500
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 0x200000000L
    move-wide/16 v400, v0
    move-wide/16 v402, v400
    move-wide/16 v2, v402

    invoke-static {v0, v1, v2, v3}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_move-object-16()V
    .registers 500
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-string v0, "123"
    move-object/16 v323, v0
    move-object/16 v400, v323
    move-object/16 v1, v400

    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V
    return-void
.end method