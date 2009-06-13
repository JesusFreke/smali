.class public LMethodsSuper;
.super Ljava/lang/Object;

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public superMethodTest()I
    .registers 1
    const v0, 23
    return v0
.end method

.method public virtualMethodTest()I
    .registers 1
    const v0, 123
    return v0
.end method