.class public LRangeMethods;
.super LRangeMethodsSuper;
.implements LRangeMethodsInterface;

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, LRangeMethodsSuper;-><init>()V
    return-void
.end method

.method public virtualMethodTest(IIIIII)I
    .registers 7

    add-int v0, v1, v2
    add-int v0, v0, v3
    add-int v0, v0, v4
    add-int v0, v0, v5
    add-int v0, v0, v6

    return v0
.end method

.method public static staticMethodTest(IIIIII)I
    .registers 7

    add-int v0, v1, v2
    add-int v0, v0, v3
    add-int v0, v0, v4
    add-int v0, v0, v5
    add-int v0, v0, v6

    return v0
.end method

.method public interfaceMethodTest(IIIIII)I
    .registers 7

    add-int v0, v1, v2
    add-int v0, v0, v3
    add-int v0, v0, v4
    add-int v0, v0, v5
    add-int v0, v0, v6

    return v0
.end method