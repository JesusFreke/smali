.class public LFormat11x;
.super Ljava/lang/Object;
.source "Format11x.smali"

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public testMethodI()I
    .registers 1

    const v0, 23
    return v0
.end method

.method public testMethodJ()J
    .registers 2

    const-wide v0, 0x200000000L
    return-wide v0
.end method

.method public testMethodStr()Ljava/lang/String;
    .registers 1

    const-string v0, "in testMethodStr()"
    return-object v0
.end method


.method public test_move-result__return()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    invoke-virtual {p0}, LFormat11x;->testMethodI()I
    move-result v0

    const v1, 23
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test_move-result-wide__return-wide()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    invoke-virtual {p0}, LFormat11x;->testMethodJ()J
    move-result-wide v0

    const-wide v2, 0x200000000L
    invoke-static {v0, v1, v2, v3}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test_move-result-object__return-object()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    invoke-virtual {p0}, LFormat11x;->testMethodStr()Ljava/lang/String;
    move-result-object v0

    const-string v1, "in testMethodStr()"
    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V
    return-void
.end method

.method public test_move-exception__throw()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    :startTry
    const-string v0, "This is an exception message"
    new-instance v1, Ljava/lang/Exception;
    invoke-direct {v1, v0}, Ljava/lang/Exception;-><init>(Ljava/lang/String;)V
    throw v1
    :endTry

    .catch Ljava/lang/Exception; {:startTry .. :endTry} :handler

    :handler
    move-exception v0
    invoke-virtual {v0}, Ljava/lang/Exception;->getMessage()Ljava/lang/String;
    move-result-object v1

    const-string v0, "This is an exception message"
    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V
    return-void
.end method


.method public test_monitor-enter__monitor_leave()V
    .registers 1
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-string v0, "this is a string"

    monitor-enter v0

    nop
    nop
    nop

    monitor-exit v0

    #TODO: need to write a multi-threaded test to test monitor-enter and monitor-exit
    return-void    
.end method