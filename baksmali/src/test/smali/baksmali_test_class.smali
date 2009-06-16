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

    0:
    const-string v0, "testing\n123"

    2:
    goto switch:
    
    3:
    sget v0, Lbaksmali/test/class;->staticField:I

    5:

    switch:
    packed-switch v0, pswitch:

    8:

    const/4 v0, 2

    9:


    Label10:
    Label11:
    Label12:
    Label13:
    return-object v0

    A:

    .array-data 4
        1 2 3 4 5 6
    .end array-data

    1A:

    pswitch:
    .packed-switch 10
        Label10:
        Label11:
        Label12:
        Label13:
    .end packed-switch

.end method

