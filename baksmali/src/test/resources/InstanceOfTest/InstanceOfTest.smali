.class public LInstanceOfTest;
.super Ljava/lang/Object;


# virtual methods
.method public testInstanceOfEqz(Ljava/lang/Object;)I
    .registers 3

    #v0=(Uninit);p0=(Reference,LInstanceOfTest;);p1=(Reference,Ljava/lang/Object;);
    instance-of v0, p1, Ljava/lang/String;
    #v0=(Boolean);p0=(Reference,LInstanceOfTest;);p1=(Reference,Ljava/lang/Object;);

    #v0=(Boolean);p0=(Reference,LInstanceOfTest;);p1=(Reference,Ljava/lang/Object;);
    if-eqz v0, :cond_9
    #v0=(Boolean);p0=(Reference,LInstanceOfTest;);p1=(Unknown);

    #v0=(Boolean);p0=(Reference,LInstanceOfTest;);p1=(Reference,Ljava/lang/String;);
    invoke-virtual {p1}, Ljava/lang/String;->length()I
    #v0=(Boolean);p0=(Reference,LInstanceOfTest;);p1=(Reference,Ljava/lang/String;);

    #v0=(Boolean);p0=(Reference,LInstanceOfTest;);p1=(Reference,Ljava/lang/String;);
    move-result v0
    #v0=(Integer);p0=(Reference,LInstanceOfTest;);p1=(Reference,Ljava/lang/String;);

    #v0=(Integer);p0=(Reference,LInstanceOfTest;);p1=(Reference,Ljava/lang/String;);
    return v0
    #v0=(Integer);p0=(Reference,LInstanceOfTest;);p1=(Reference,Ljava/lang/String;);

    :cond_9
    #v0=(Boolean);p0=(Reference,LInstanceOfTest;);p1=(Reference,Ljava/lang/Object;);
    const v0, -0x1
    #v0=(Byte);p0=(Reference,LInstanceOfTest;);p1=(Reference,Ljava/lang/Object;);

    #v0=(Byte);p0=(Reference,LInstanceOfTest;);p1=(Reference,Ljava/lang/Object;);
    return v0
    #v0=(Byte);p0=(Reference,LInstanceOfTest;);p1=(Reference,Ljava/lang/Object;);
.end method

.method public testInstanceOfNez(Ljava/lang/Object;)I
    .registers 3

    #v0=(Uninit);p0=(Reference,LInstanceOfTest;);p1=(Reference,Ljava/lang/Object;);
    instance-of v0, p1, Ljava/lang/String;
    #v0=(Boolean);p0=(Reference,LInstanceOfTest;);p1=(Reference,Ljava/lang/Object;);

    #v0=(Boolean);p0=(Reference,LInstanceOfTest;);p1=(Reference,Ljava/lang/Object;);
    if-nez v0, :cond_8
    #v0=(Boolean);p0=(Reference,LInstanceOfTest;);p1=(Unknown);

    #v0=(Boolean);p0=(Reference,LInstanceOfTest;);p1=(Reference,Ljava/lang/Object;);
    const v0, -0x1
    #v0=(Byte);p0=(Reference,LInstanceOfTest;);p1=(Reference,Ljava/lang/Object;);

    #v0=(Byte);p0=(Reference,LInstanceOfTest;);p1=(Reference,Ljava/lang/Object;);
    return v0
    #v0=(Byte);p0=(Reference,LInstanceOfTest;);p1=(Reference,Ljava/lang/Object;);

    :cond_8
    #v0=(Boolean);p0=(Reference,LInstanceOfTest;);p1=(Reference,Ljava/lang/String;);
    invoke-virtual {p1}, Ljava/lang/String;->length()I
    #v0=(Boolean);p0=(Reference,LInstanceOfTest;);p1=(Reference,Ljava/lang/String;);

    #v0=(Boolean);p0=(Reference,LInstanceOfTest;);p1=(Reference,Ljava/lang/String;);
    move-result v0
    #v0=(Integer);p0=(Reference,LInstanceOfTest;);p1=(Reference,Ljava/lang/String;);

    #v0=(Integer);p0=(Reference,LInstanceOfTest;);p1=(Reference,Ljava/lang/String;);
    return v0
    #v0=(Integer);p0=(Reference,LInstanceOfTest;);p1=(Reference,Ljava/lang/String;);
.end method

.method public testRegisterAlias(Ljava/lang/Object;)I
    .registers 4

    #v0=(Uninit);v1=(Uninit);p0=(Reference,LInstanceOfTest;);p1=(Reference,Ljava/lang/Object;);
    move-object p0, p1
    #v0=(Uninit);v1=(Uninit);p0=(Reference,Ljava/lang/Object;);p1=(Reference,Ljava/lang/Object;);

    #v0=(Uninit);v1=(Uninit);p0=(Reference,Ljava/lang/Object;);p1=(Reference,Ljava/lang/Object;);
    instance-of v0, p0, Ljava/lang/String;
    #v0=(Boolean);v1=(Uninit);p0=(Reference,Ljava/lang/Object;);p1=(Reference,Ljava/lang/Object;);

    #v0=(Boolean);v1=(Uninit);p0=(Reference,Ljava/lang/Object;);p1=(Reference,Ljava/lang/Object;);
    if-eqz v0, :cond_f
    #v0=(Boolean);v1=(Uninit);p0=(Unknown);p1=(Unknown);

    :cond_5
    #v0=(Integer):merge{0x3:(Boolean),0xc:(Integer)}
    #v1=(Conflicted):merge{0x3:(Uninit),0xc:(Null)}
    #p0=(Reference,Ljava/lang/String;);p1=(Reference,Ljava/lang/String;);
    invoke-virtual {p1}, Ljava/lang/String;->length()I
    #v0=(Integer);v1=(Conflicted);p0=(Reference,Ljava/lang/String;);p1=(Reference,Ljava/lang/String;);

    #v0=(Integer);v1=(Conflicted);p0=(Reference,Ljava/lang/String;);p1=(Reference,Ljava/lang/String;);
    move-result v0
    #v0=(Integer);v1=(Conflicted);p0=(Reference,Ljava/lang/String;);p1=(Reference,Ljava/lang/String;);

    #v0=(Integer);v1=(Conflicted);p0=(Reference,Ljava/lang/String;);p1=(Reference,Ljava/lang/String;);
    const v1, 0x0
    #v0=(Integer);v1=(Null);p0=(Reference,Ljava/lang/String;);p1=(Reference,Ljava/lang/String;);

    #v0=(Integer);v1=(Null);p0=(Reference,Ljava/lang/String;);p1=(Reference,Ljava/lang/String;);
    if-le v0, v1, :cond_5
    #v0=(Integer);v1=(Null);p0=(Reference,Ljava/lang/String;);p1=(Reference,Ljava/lang/String;);

    #v0=(Integer);v1=(Null);p0=(Reference,Ljava/lang/String;);p1=(Reference,Ljava/lang/String;);
    return v0
    #v0=(Integer);v1=(Null);p0=(Reference,Ljava/lang/String;);p1=(Reference,Ljava/lang/String;);

    :cond_f
    #v0=(Boolean);v1=(Uninit);p0=(Reference,Ljava/lang/Object;);p1=(Reference,Ljava/lang/Object;);
    const v0, -0x1
    #v0=(Byte);v1=(Uninit);p0=(Reference,Ljava/lang/Object;);p1=(Reference,Ljava/lang/Object;);

    #v0=(Byte);v1=(Uninit);p0=(Reference,Ljava/lang/Object;);p1=(Reference,Ljava/lang/Object;);
    return v0
    #v0=(Byte);v1=(Uninit);p0=(Reference,Ljava/lang/Object;);p1=(Reference,Ljava/lang/Object;);
.end method
