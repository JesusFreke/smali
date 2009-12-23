.class public LFormat10x;
.super Ljava/lang/Object;
.source "Format10x.smali"

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public test_nop()V
    .registers 1
    .annotation runtime Lorg/junit/Test;
    .end annotation

    #yep, it's a nop. not much to test, other than that it runs
    nop

    return-void
.end method

.method public test_return-void()V
    .registers 1
    .annotation runtime Lorg/junit/Test;
    .end annotation
    
    return-void

    #if we get here, the return-void didn't work
    invoke-static {}, Lorg/junit/Assert;->fail()V
.end method

