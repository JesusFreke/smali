.class public LLineTest;
.super Ljava/lang/Object;
.source "LineTest.smali"

#this class tests line debug info

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public largerThanSignedShort()V
    .registers 1
    .annotation runtime Lorg/junit/Test;
    .end annotation

    #a line number that just barely doesn't fit in a signed short
    .line 0x8000

	return-void
.end method

.method public largerThanUnsignedShort()V
    .registers 1
    .annotation runtime Lorg/junit/Test;
    .end annotation

    #a line number that is way bigger than a signed short
    .line 0x10000

	return-void
.end method

.method public largerThanSignedInt()V
    .registers 1
    .annotation runtime Lorg/junit/Test;
    .end annotation

    #a line number that just barely doesn't fit in a signed int
    .line 0x80000000

	return-void
.end method