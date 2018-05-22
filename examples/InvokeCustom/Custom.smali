.class public LCustom;

.super Ljava/lang/Object;

.method public doSomething(Ljava/lang/String;)Ljava/lang/String;
    .registers 15

    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;

    invoke-virtual {v0, p1}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V

    const-string v1, "Custom"

    return-object v1
.end method

.method public gnihtemoSod(Ljava/lang/String;)Ljava/lang/String;
    .registers 15

    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;

    invoke-virtual {v0, p1}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V

    const-string v1, "motsuC"

    return-object v1
.end method