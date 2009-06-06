.class public LFormat22x;
.super Ljava/lang/Object;
.source "Format22x.smali"

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public test_move-from16()V
    .registers 276
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 6
    move/16 v275, v0

    move/from16 v1, v275

    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_move-wide-from16()V
    .registers 276
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 0x200000000L
    move-wide/16 v274, v0

    move-wide/from16 v2, v274

    invoke-static {v0, v1, v2, v3}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_move-object-from16()V
    .registers 276
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-string v0, "6"
    move-object/16 v275, v0

    move-object/from16 v1, v275

    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V
    return-void
.end method