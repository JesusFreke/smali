.class public LFormat21c;
.super Ljava/lang/Object;
.source "Format21c.smali"

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public test_const-string()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-string v0, "test"

    const-string v1, "test"
    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V
    return-void
.end method

.method public test_const-class()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-class v0, Ljava/io/PrintStream;

    invoke-virtual {v0}, Ljava/lang/Class;->toString()Ljava/lang/String;
    move-result-object v1

    const-string v0, "class java.io.PrintStream"

    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    return-void
.end method

.method public test_check-cast-fail()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    :tryStart
    const-string v0, "test"

    check-cast v0, Ljava/io/PrintStream;
    :tryEnd
    .catch Ljava/lang/ClassCastException; {:tryStart .. :tryEnd} :handler

    #the check-cast didn't throw an exception as expected
    invoke-static {}, Lorg/junit/Assert;->fail()V

    :handler

    return-void
.end method

.method public test_check-cast-success()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    :tryStart
    const-string v0, "test"

    check-cast v0, Ljava/lang/Object;
    :tryEnd
    .catch Ljava/lang/ClassCastException; {:tryStart .. :tryEnd} :handler

    return-void

    :handler

    #the check-cast incorrectlly threw an exception as expected    
    invoke-static {}, Lorg/junit/Assert;->fail()V
    return-void
.end method

.method public test_new-instance()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-string v0, "test"

    new-instance v1, LStringWrapper;
    invoke-direct {v1, v0}, LStringWrapper;-><init>(Ljava/lang/String;)V

    invoke-virtual {v1}, LStringWrapper;->getValue()Ljava/lang/String;
    move-result-object v2

    invoke-static {v0, v2}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    return-void
.end method


.method public test-sput__sget()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 23
    sput v0, LStaticFields;->staticField:I

    sget v1, LStaticFields;->staticField:I

    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test-sput-object__sget-object()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-string v0, "a string"
    sput-object v0, LStaticFields;->staticObjectField:Ljava/lang/String;

    sget-object v1, LStaticFields;->staticObjectField:Ljava/lang/String;

    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V
    return-void
.end method

.method public test-sput-wide__sget-wide()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-wide v0, 0x200000000L
    sput-wide v0, LStaticFields;->staticWideField:J

    sget-wide v2, LStaticFields;->staticWideField:J

    invoke-static {v0, v1, v2, v3}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test-sput-boolean__sget-boolean_true()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 1
    sput-boolean v0, LStaticFields;->staticBooleanField:Z

    sget-boolean v1, LStaticFields;->staticBooleanField:Z

    invoke-static {v1}, Lorg/junit/Assert;->assertTrue(Z)V
    return-void
.end method

.method public test-sput-boolean__sget-boolean_false()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 0
    sput-boolean v0, LStaticFields;->staticBooleanField:Z

    sget-boolean v1, LStaticFields;->staticBooleanField:Z

    invoke-static {v1}, Lorg/junit/Assert;->assertFalse(Z)V
    return-void
.end method

.method public test-sput-byte__sget-byte()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 120T
    sput-byte v0, LStaticFields;->staticByteField:B

    sget-byte v1, LStaticFields;->staticByteField:B

    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test-sput-char__sget-char()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 'a'
    sput-char v0, LStaticFields;->staticCharField:C

    sget-char v1, LStaticFields;->staticCharField:C

    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test-sput-short__sget-short()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 1234S
    sput-short v0, LStaticFields;->staticShortField:S

    sget-short v1, LStaticFields;->staticShortField:S

    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method