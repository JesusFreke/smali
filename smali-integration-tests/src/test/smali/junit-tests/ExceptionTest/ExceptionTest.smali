.class public LExceptionTest;
.super Ljava/lang/Object;
.source "ExceptionTest.smali"

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public singleCatchTest()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    :try_start
    new-instance v0, Ljava/lang/Exception;
    const-string v1, "This is an error message"
    invoke-direct {v0, v1}, Ljava/lang/Exception;-><init>(Ljava/lang/String;)V
    throw v0
    :try_end
    .catch Ljava/lang/Exception; {:try_start .. :try_end} :handler

    :handler
    #no need to test anything. If it didn't catch the exception, the test would fail
	return-void
.end method


.method public nestedCatchTest()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    :try_start_outer
    nop
    nop

    :try_start_inner
    new-instance v0, Ljava/lang/RuntimeException;
    const-string v1, "This is an error message"
    invoke-direct {v0, v1}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V
    throw v0
    :try_end_inner
    .catch Ljava/lang/RuntimeException; {:try_start_inner .. :try_end_outer} :handler_inner

    nop
    nop
    :try_end_outer

    .catch Ljava/lang/Exception; {:try_start_outer .. :try_end_outer} :handler_outer

    :handler_outer
    invoke-static {}, Lorg/junit/Assert;->fail()V

    :handler_inner


	return-void
.end method



.method public catchAllTest()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    :try_start
    new-instance v0, Ljava/lang/Exception;
    const-string v1, "This is an error message"
    invoke-direct {v0, v1}, Ljava/lang/Exception;-><init>(Ljava/lang/String;)V
    throw v0
    :try_end
    .catchall {:try_start .. :try_end} :handler

    :handler
    #no need to test anything. If it didn't catch the exception, the test would fail
	return-void
.end method