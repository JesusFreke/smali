.class LMain;

.super Ljava/lang/Object;

.method public static main([Ljava/lang/String;)V
    .registers 15

    new-instance v0, LCustom;
    invoke-direct {v0}, LCustom;-><init>()V
    
    const-string v1, "Arg to doSomething"

    invoke-custom {v0, v1}, normallyLinkedCallSite("doSomething", (LCustom;Ljava/lang/String;)Ljava/lang/String;, "just testing")@LBootstrapLinker;->normalLink(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;)Ljava/lang/invoke/CallSite;
    move-result-object v2
    sget-object v3, Ljava/lang/System;->out:Ljava/io/PrintStream;
    const-string v4, "got back - "
    invoke-virtual {v3, v4}, Ljava/io/PrintStream;->print(Ljava/lang/String;)V
    invoke-virtual {v3, v2}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V

    invoke-custom {v0, v1}, backwardsLinkedCallSite("doSomething", (LCustom;Ljava/lang/String;)Ljava/lang/String;, "just testing")@LBootstrapLinker;->backwardsLink(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;)Ljava/lang/invoke/CallSite;
    move-result-object v2
    sget-object v3, Ljava/lang/System;->out:Ljava/io/PrintStream;
    const-string v4, "got back - "
    invoke-virtual {v3, v4}, Ljava/io/PrintStream;->print(Ljava/lang/String;)V
    invoke-virtual {v3, v2}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
.end method