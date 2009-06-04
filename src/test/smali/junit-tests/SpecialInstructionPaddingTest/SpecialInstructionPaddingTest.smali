.class public LSpecialInstructionPaddingTest;
.super Ljava/lang/Object;
.source "InstructionPaddingTest.smali"

.method public constructor <init>()V
    .registers 1
    invoke-direct {v0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public test()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation
    
    const v0, 0
    invoke-static {}, LSpecialInstructionPaddingTest;->paddingTest()I
    move-result v0
    int-to-long v0, v0


    const-wide/16 v2, 12

    #the real test is that dalvik loaded and verified this class. This is
    #mostly just to make sure that the method was actually called
    invoke-static {v0, v1, v2, v3}, Lorg/junit/Assert;->assertEquals(JJ)V

    return-void
.end method

.method public static paddingTest()I
    .registers  2

    const v0, 12

switch:
    packed-switch v0, PackedSwitch:

Label10:
    const v1, 10
    return v1

Label11:
    const v1, 11
    return v1

Label12:
    const v1, 12
    return v1

Label13:
    const v1, 13
    return v1

    #this nop de-aligns the following packed-switch data
    #smali should generate another nop before the packed-switch
    #data to force alignment
    nop

PackedSwitch:
    .packed-switch switch: 10
        Label10:
        Label11:
        Label12:
        Label13:
    .end packed-switch

.end method