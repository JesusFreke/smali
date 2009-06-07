.class public LMethods;
.super LMethodsSuper;
.implements LMethodsInterface;

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, LMethodsSuper;-><init>()V
    return-void
.end method

.method public virtualMethodTest()I
    .registers 1
    const v0, 23
    return v0
.end method

.method public static staticMethodTest()I
    .registers 1
    const v0, 23
    return v0
.end method

.method public interfaceMethodTest()I
    .registers 1
    const v0, 23
    return v0
.end method