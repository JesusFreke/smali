.class public LRangeMethodsSuper;
.super Ljava/lang/Object;

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public superMethodTest(IIIIII)I
    .registers 7

    add-int v0, v1, v2
    add-int v0, v0, v3
    add-int v0, v0, v4
    add-int v0, v0, v5
    add-int v0, v0, v6

    return v0
.end method

.method public virtualMethodTest(IIIIII)I
    .registers 7

    add-int v0, v1, v2
    add-int v0, v0, v3
    add-int v0, v0, v4
    add-int v0, v0, v5
    add-int v0, v0, v6

    #add something extra, to make the test fail if this method is called instead of the subclasses's method
    const v1, 1
    add-int v0, v0, v1

    return v0
.end method