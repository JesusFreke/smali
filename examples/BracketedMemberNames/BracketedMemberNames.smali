.class public LBracketedMemberNames;

.super Ljava/lang/Object;

.field public static <test_field>:Ljava/lang/String; = "Hello World!"

.method public static main([Ljava/lang/String;)V
    .registers 2

    invoke-static {}, LBracketedMemberNames;->test_method()V

    return-void
.end method

.method public static test_method()V
    .registers 2

    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;

    sget-object v1, LBracketedMemberNames;-><test_field>:Ljava/lang/String;

    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V

    return-void
.end method

.method public static <test_method>()V
    .registers 2

    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;

    sget-object v1, LBracketedMemberNames;-><test_field>:Ljava/lang/String;

    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V

    # this will cause a verification error
    invoke-static {}, LBracketedMemberNames;-><test_method>()V

    return-void
.end method