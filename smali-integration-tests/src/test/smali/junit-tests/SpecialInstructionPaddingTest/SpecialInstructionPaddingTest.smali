.class public LSpecialInstructionPaddingTest;
.super Ljava/lang/Object;
.source "InstructionPaddingTest.smali"

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public test()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 0
    invoke-static {}, LSpecialInstructionPaddingTest;->paddingTest()I
    move-result v0



    const/16 v1, 12

    #the real test is that dalvik loaded and verified this class. This is
    #mostly just to make sure that the method was actually called
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V

    return-void
.end method

.method public static paddingTest()I
    .registers  2

    const v0, 12

    packed-switch v0, :PackedSwitch

:Label10
    const v1, 10
    return v1

:Label11
    const v1, 11
    return v1

:Label12
    const v1, 12
    return v1

:Label13
    const v1, 13
    return v1

    #this nop de-aligns the following packed-switch data
    #smali should generate another nop before the packed-switch
    #data to force alignment
    nop

:PackedSwitch
    .packed-switch 10
        :Label10
        :Label11
        :Label12
        :Label13
    .end packed-switch
.end method



.method public test2()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 0
    invoke-static {}, LSpecialInstructionPaddingTest;->paddingTest()I
    move-result v0



    const/16 v1, 12

    #the real test is that dalvik loaded and verified this class. This is
    #mostly just to make sure that the method was actually called
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V

    return-void
.end method

.method public static paddingTest()I
    .registers  2

    const v0, 12

    packed-switch v0, :PackedSwitch

    packed-switch v0, :PackedSwitch2

:Label10
    const v1, 10
    return v1

:Label11
    const v1, 11
    return v1

:Label12
    const v1, 12
    return v1

:Label13
    const v1, 13
    return v1

:PackedSwitch
    .packed-switch 10
        :Label10
        :Label11
        :Label12
        :Label13
    .end packed-switch

    #this tests out an issue that occurred where the prior packed switch data starts at a byte offset
    #that is 4 byte aligned but isn't 8 byte aligned, which caused the code to report the size of the
    #instruction to be 2 bytes more than it should be.
    #
    #Normally, if things are working correctly, this nop will cause the following packed-switch data
    #to be unaligned. But with the above issue, the logic will *think* that the packed switch data
    #is aligned, and won't output the extra nop to align it, thus causing a verification error.
    nop

:PackedSwitch2
    .packed-switch 10
        :Label10
        :Label11
        :Label12
        :Label13
    .end packed-switch


.end method