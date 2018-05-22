.class public LBootstrapLinker;

.super Ljava/lang/Object;

.method public static normalLink(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Object;)Ljava/lang/invoke/CallSite;
    .registers 15

    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;
    const-string v3, "BootstrapLinker.normalLink - "
    invoke-virtual {v0, v3}, Ljava/io/PrintStream;->print(Ljava/lang/String;)V
    invoke-virtual {p3}, Ljava/lang/Object;->toString()Ljava/lang/String;
    move-result-object v1
    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V

    const v0, 0
    const v1, 1
    invoke-virtual {p2, v0, v1}, Ljava/lang/invoke/MethodType;->dropParameterTypes(II)Ljava/lang/invoke/MethodType;
    move-result-object p2

    const-class v1, LCustom;

    invoke-virtual {p0, v1, p1, p2}, Ljava/lang/invoke/MethodHandles$Lookup;->findVirtual(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;
    move-result-object v2

    new-instance v0, Ljava/lang/invoke/ConstantCallSite;
    invoke-direct {v0, v2}, Ljava/lang/invoke/ConstantCallSite;-><init>(Ljava/lang/invoke/MethodHandle;)V

    return-object v0
.end method


.method public static backwardsLink(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;)Ljava/lang/invoke/CallSite;
    .registers 15

    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;
    const-string v3, "BootstrapLinker.backwardsLink - "
    invoke-virtual {v0, v3}, Ljava/io/PrintStream;->print(Ljava/lang/String;)V
    invoke-virtual {v0, p3}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V

    const v0, 0
    const v1, 1
    invoke-virtual {p2, v0, v1}, Ljava/lang/invoke/MethodType;->dropParameterTypes(II)Ljava/lang/invoke/MethodType;
    move-result-object p2

    new-instance v0, Ljava/lang/StringBuffer;
    invoke-direct {v0, p1}, Ljava/lang/StringBuffer;-><init>(Ljava/lang/String;)V
    invoke-virtual {v0}, Ljava/lang/StringBuffer;->reverse()Ljava/lang/StringBuffer;
    invoke-virtual {v0}, Ljava/lang/StringBuffer;->toString()Ljava/lang/String;
    move-result-object p1

    const-class v1, LCustom;

    invoke-virtual {p0, v1, p1, p2}, Ljava/lang/invoke/MethodHandles$Lookup;->findVirtual(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;
    move-result-object v2

    new-instance v0, Ljava/lang/invoke/ConstantCallSite;
    invoke-direct {v0, v2}, Ljava/lang/invoke/ConstantCallSite;-><init>(Ljava/lang/invoke/MethodHandle;)V

    return-object v0
.end method
