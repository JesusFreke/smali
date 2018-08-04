.class LConstMethodHandle;
.super Ljava/lang/Object;


# static fields
.field public static staticField:Ljava/lang/Object;


# instance fields
.field public instanceField:Ljava/lang/Object;


# direct methods
.method public static constMethodHandle()V
    .registers 15

    const-method-handle v0, invoke-static@Ljava/lang/Integer;->toString(I)Ljava/lang/String;

    const-method-handle v0, invoke-instance@Ljava/lang/Integer;->toString()Ljava/lang/String;

    const-method-handle v0, static-put@LConstMethodHandle;->instanceField:Ljava/lang/Object;

    const-method-handle v0, static-put@LConstMethodHandle;->instanceField:Ljava/lang/Object;

    const-method-handle v0, static-put@LConstMethodHandle;->staticField:Ljava/lang/Object;

    const-method-handle v0, static-put@LConstMethodHandle;->staticField:Ljava/lang/Object;

    return-void
.end method
