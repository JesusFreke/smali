.class public Lbaksmali/test/class;
.super Ljava/lang/Object;

.implements Lsome/interface;
.implements Lsome/other/interface;

.field public static staticField:I
.field public instanceField:Ljava/lang/String;

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public testMethod(ILjava/lang/String;)Ljava/lang/String;
    .registers 3
    const-string v0, "testing\n123"

    sget v0, Lbaksmali/test/class;->staticField:I

    return-object v0
.end method

