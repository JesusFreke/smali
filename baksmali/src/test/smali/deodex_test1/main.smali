.class public Lmain;

.super Ljava/lang/Object;

.method public static main([Ljava/lang/String;)V
    .registers 3

    :here4
    const v0, 0

    :here3

    new-instance v2, Lsuperclass;
    invoke-direct {v2}, Lsuperclass;-><init>()V

    if-eqz v0, :here2


    #this is the unresolvable instruction. v0 is always null,
    #and this will always throw an exception. Everything below
    #here, until the here2: label is dead code, and should be
    #commented out. This instruction itself should be be replaced
    #with a call to Ljava/lang/Object;->hashCode()I, followed
    #by a goto/32 0, which is just there to prevent dexopt from
    #thinking that execution will fall off the end of the method
    #i.e. if all the code following this was dead (and thus commented
    #out)
    invoke-virtual {v0}, Lrandomclass;->getSuperclass()Lsuperclass;
    move-result-object v1

    
    #a branch to outside the dead code. The branch label should not
    #be commented out, because there is a non-dead instruction
    #that branches to it
    if-eqz v0, :here2

    
    #a branch to inside the dead code. the branch label should be
    #commented out
    if-eqz v0, :here

    #another branch to outside the dead code. In this case, the "dead"
    #instruction is the first instruction that references the label.
    #the label should not be commented out, because it is referenced
    #be a non-dead instruction
    if-eqz v0, :here3

    #one more branch to out the dead code. the branch label should be
    #commented out, because there are no other non-dead instructions
    #referenceding it
    if-eqz v0, :here4

    #another odexed instruction that uses the result of the
    #first unresolveable odex instruction. this should
    #appear as a commented invoke-virtual-quick instruction
    invoke-virtual {v1}, Lsuperclass;->somemethod()V

    :here

    #a resolveable odex instruction in the dead code. It should be resolved,
    #but still commented out
    invoke-virtual {v2}, Lsuperclass;->somemethod()V

    
    :here2

    #and we're back to the non-dead code
    invoke-virtual {v2}, Lsuperclass;->somemethod()V

    if-nez v0, :here3
    

    return-void
.end method

.method public static UnresolvedInstructionTest1()Lsuperclass;
    .registers 2
    const v0, 0

    #this is an unresolvable instruction, due to v0 always being null
    #this instruction should be replaced with "throw v0", followed by
    #a "goto/32 0", since it would otherwise be the last instruction
    #in the method, which isn't allowed
    invoke-virtual/range {v0 .. v0}, Lrandomclass;->getSuperclass()Lsuperclass;

    #the following instructions should be commented out
    move-result-object v1
    return-object v1
.end method

.method public static UnresolvedInstructionTest2()Lsuperclass;
    .registers 2
    const v0, 0

    if-eqz v0, :here

    #this is an unresolvable instruction, due to v0 always being null
    #this instruction should be replaced with "throw v0". There shouldn't
    #be a "goto/32 0" afterwards, since it won't be the last instruction
    #in the method.
    invoke-virtual/range {v0 .. v0}, Lrandomclass;->getSuperclass()Lsuperclass;

    #the following instructions should be commented out
    move-result-object v1
    return-object v1

    #and now back to our normal programming
    :here
    return-object v0
.end method