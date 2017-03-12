.class public LUninitRefIdentityTest;
.super Ljava/lang/Object;


# direct methods
.method public constructor <init>()V
    .registers 4

    #v0=(Uninit);v1=(Uninit);v2=(Uninit);p0=(UninitThis,LUninitRefIdentityTest;);
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    #v0=(Uninit);v1=(Uninit);v2=(Uninit);p0=(Reference,LUninitRefIdentityTest;);

    #v0=(Uninit);v1=(Uninit);v2=(Uninit);p0=(Reference,LUninitRefIdentityTest;);
    new-instance v0, Ljava/lang/String;
    #v0=(UninitRef,Ljava/lang/String;);v1=(Uninit);v2=(Uninit);p0=(Reference,LUninitRefIdentityTest;);

    #v0=(UninitRef,Ljava/lang/String;);v1=(Uninit);v2=(Uninit);p0=(Reference,LUninitRefIdentityTest;);
    if-eqz v0, :cond_9
    #v0=(UninitRef,Ljava/lang/String;);v1=(Uninit);v2=(Uninit);p0=(Reference,LUninitRefIdentityTest;);

    #v0=(UninitRef,Ljava/lang/String;);v1=(Uninit);v2=(Uninit);p0=(Reference,LUninitRefIdentityTest;);
    new-instance v0, Ljava/lang/String;
    #v0=(UninitRef,Ljava/lang/String;);v1=(Uninit);v2=(Uninit);p0=(Reference,LUninitRefIdentityTest;);

    :cond_9
    #v0=(Conflicted):merge{0x5:(UninitRef,Ljava/lang/String;),0x7:(UninitRef,Ljava/lang/String;)}
    #v1=(Uninit);v2=(Uninit);p0=(Reference,LUninitRefIdentityTest;);
    invoke-direct {v0}, Ljava/lang/String;-><init>()V
    #v0=(Conflicted);v1=(Uninit);v2=(Uninit);p0=(Reference,LUninitRefIdentityTest;);

    #v0=(Conflicted);v1=(Uninit);v2=(Uninit);p0=(Reference,LUninitRefIdentityTest;);
    return-void
    #v0=(Conflicted);v1=(Uninit);v2=(Uninit);p0=(Reference,LUninitRefIdentityTest;);
.end method

.method public constructor <init>(I)V
    .registers 2

    #p0=(UninitThis,LUninitRefIdentityTest;);p1=(Integer);
    move-object p1, p0
    #p0=(UninitThis,LUninitRefIdentityTest;);p1=(UninitThis,LUninitRefIdentityTest;);

    #p0=(UninitThis,LUninitRefIdentityTest;);p1=(UninitThis,LUninitRefIdentityTest;);
    invoke-direct {p1}, Ljava/lang/Object;-><init>()V
    #p0=(Reference,LUninitRefIdentityTest;);p1=(Reference,LUninitRefIdentityTest;);

    :cond_4
    #p0=(Reference,LUninitRefIdentityTest;);
    #p1=(Reference,LUninitRefIdentityTest;):merge{0x1:(Reference,LUninitRefIdentityTest;),0x7:(Null)}
    const p1, 0x0
    #p0=(Reference,LUninitRefIdentityTest;);p1=(Null);

    #p0=(Reference,LUninitRefIdentityTest;);p1=(Null);
    if-nez p1, :cond_4
    #p0=(Reference,LUninitRefIdentityTest;);p1=(Null);

    #p0=(Reference,LUninitRefIdentityTest;);p1=(Null);
    return-void
    #p0=(Reference,LUninitRefIdentityTest;);p1=(Null);
.end method

.method public constructor <init>(Ljava/lang/String;)V
    .registers 2

    #p0=(UninitThis,LUninitRefIdentityTest;);p1=(Reference,Ljava/lang/String;);
    move-object p1, p0
    #p0=(UninitThis,LUninitRefIdentityTest;);p1=(UninitThis,LUninitRefIdentityTest;);

    #p0=(UninitThis,LUninitRefIdentityTest;);p1=(UninitThis,LUninitRefIdentityTest;);
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    #p0=(Reference,LUninitRefIdentityTest;);p1=(Reference,LUninitRefIdentityTest;);

    #p0=(Reference,LUninitRefIdentityTest;);p1=(Reference,LUninitRefIdentityTest;);
    return-void
    #p0=(Reference,LUninitRefIdentityTest;);p1=(Reference,LUninitRefIdentityTest;);
.end method


# virtual methods
.method public overlappingInits()V
    .registers 3

    #v0=(Uninit);v1=(Uninit);p0=(Reference,LUninitRefIdentityTest;);
    new-instance v0, Ljava/lang/String;
    #v0=(UninitRef,Ljava/lang/String;);v1=(Uninit);p0=(Reference,LUninitRefIdentityTest;);

    #v0=(UninitRef,Ljava/lang/String;);v1=(Uninit);p0=(Reference,LUninitRefIdentityTest;);
    new-instance v1, Ljava/lang/String;
    #v0=(UninitRef,Ljava/lang/String;);v1=(UninitRef,Ljava/lang/String;);p0=(Reference,LUninitRefIdentityTest;);

    #v0=(UninitRef,Ljava/lang/String;);v1=(UninitRef,Ljava/lang/String;);p0=(Reference,LUninitRefIdentityTest;);
    new-instance p0, Ljava/lang/String;
    #v0=(UninitRef,Ljava/lang/String;);v1=(UninitRef,Ljava/lang/String;);p0=(UninitRef,Ljava/lang/String;);

    #v0=(UninitRef,Ljava/lang/String;);v1=(UninitRef,Ljava/lang/String;);p0=(UninitRef,Ljava/lang/String;);
    invoke-direct {p0}, Ljava/lang/String;-><init>()V
    #v0=(UninitRef,Ljava/lang/String;);v1=(UninitRef,Ljava/lang/String;);p0=(Reference,Ljava/lang/String;);

    #v0=(UninitRef,Ljava/lang/String;);v1=(UninitRef,Ljava/lang/String;);p0=(Reference,Ljava/lang/String;);
    invoke-direct {v1}, Ljava/lang/String;-><init>()V
    #v0=(UninitRef,Ljava/lang/String;);v1=(Reference,Ljava/lang/String;);p0=(Reference,Ljava/lang/String;);

    #v0=(UninitRef,Ljava/lang/String;);v1=(Reference,Ljava/lang/String;);p0=(Reference,Ljava/lang/String;);
    invoke-direct {v0}, Ljava/lang/String;-><init>()V
    #v0=(Reference,Ljava/lang/String;);v1=(Reference,Ljava/lang/String;);p0=(Reference,Ljava/lang/String;);

    #v0=(Reference,Ljava/lang/String;);v1=(Reference,Ljava/lang/String;);p0=(Reference,Ljava/lang/String;);
    return-void
    #v0=(Reference,Ljava/lang/String;);v1=(Reference,Ljava/lang/String;);p0=(Reference,Ljava/lang/String;);
.end method
