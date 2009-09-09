.class public Lmain;

.super Ljava/lang/Object;

.method public static main([Ljava/lang/String;)V
    .registers 3

    const v2, 0


    new-instance v0, Lsubclass;
    invoke-direct {v0}, Lsubclass;-><init>()V

    invoke-virtual {v0}, Lsubclass;->somemethod()V

    goto here2:

    here:
    const v2, 1

    here2:

    #this instruction is tricky for the deodexer, because once everything gets
    #odexed, then static inspection shows the type of v0 to be Lsubclass;
    #from the above new-instance instruction.
    #so the first pass at deodexing this instruction will incorrectly resolve it
    #to Lsubclass->somemethod()V
    #It's not until the following invoke-virtual call to
    #Lrandomclass;->getSuperclass()Lsuperclass is deodexed that it will be able to
    #determine that the type of v0 is actually Lsuperclass;, and not Lsubclass;, and
    #so it has to be re-deodexed when it discovers the new register type information
    invoke-virtual {v0}, Lsuperclass;->somemethod()V
    

    new-instance v1, Lrandomclass;
    invoke-direct {v1}, Lrandomclass;-><init>()V

    invoke-virtual {v1}, Lrandomclass;->getSuperclass()Lsuperclass;
    move-result-object v0

    if-eqz v2, here:

    return-void
.end method