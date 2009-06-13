.class public LFormat22c;
.super Ljava/lang/Object;
.source "Format22c.smali"

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public test-iput__iget()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    new-instance v2, LInstanceFields;
    invoke-direct {v2}, LInstanceFields;-><init>()V

    const v0, 23
    iput v0, v2, LInstanceFields;->instanceField:I

    iget v1, v2, LInstanceFields;->instanceField:I

    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test-iput-object__iget-object()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    new-instance v2, LInstanceFields;
    invoke-direct {v2}, LInstanceFields;-><init>()V

    const-string v0, "a string"
    iput-object v0, v2, LInstanceFields;->instanceObjectField:Ljava/lang/String;

    iget-object v1, v2, LInstanceFields;->instanceObjectField:Ljava/lang/String;

    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V
    return-void
.end method

.method public test-iput-wide__iget-wide()V
    .registers 4
    .annotation runtime Lorg/junit/Test;
    .end annotation

    new-instance v2, LInstanceFields;
    invoke-direct {v2}, LInstanceFields;-><init>()V

    const-wide v0, 0x200000000L
    iput-wide v0, v2, LInstanceFields;->instanceWideField:J

    iget-wide v2, v2, LInstanceFields;->instanceWideField:J

    invoke-static {v0, v1, v2, v3}, Lorg/junit/Assert;->assertEquals(JJ)V
    return-void
.end method

.method public test-iput-boolean__iget-boolean_true()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    new-instance v2, LInstanceFields;
    invoke-direct {v2}, LInstanceFields;-><init>()V

    const v0, 1
    iput-boolean v0, v2, LInstanceFields;->instanceBooleanField:Z

    iget-boolean v1, v2, LInstanceFields;->instanceBooleanField:Z

    invoke-static {v1}, Lorg/junit/Assert;->assertTrue(Z)V
    return-void
.end method

.method public test-iput-boolean__iget-boolean_false()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    new-instance v2, LInstanceFields;
    invoke-direct {v2}, LInstanceFields;-><init>()V

    const v0, 0
    iput-boolean v0, v2, LInstanceFields;->instanceBooleanField:Z

    iget-boolean v1, v2, LInstanceFields;->instanceBooleanField:Z

    invoke-static {v1}, Lorg/junit/Assert;->assertFalse(Z)V
    return-void
.end method

.method public test-iput-byte__iget-byte()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    new-instance v2, LInstanceFields;
    invoke-direct {v2}, LInstanceFields;-><init>()V

    const v0, 120T
    iput-byte v0, v2, LInstanceFields;->instanceByteField:B

    iget-byte v1, v2, LInstanceFields;->instanceByteField:B

    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test-iput-char__iget-char()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    new-instance v2, LInstanceFields;
    invoke-direct {v2}, LInstanceFields;-><init>()V

    const v0, 'a'
    iput-char v0, v2, LInstanceFields;->instanceCharField:C

    iget-char v1, v2, LInstanceFields;->instanceCharField:C

    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method

.method public test-iput-short__iget-short()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    new-instance v2, LInstanceFields;
    invoke-direct {v2}, LInstanceFields;-><init>()V

    const v0, 1234S
    iput-short v0, v2, LInstanceFields;->instanceShortField:S

    iget-short v1, v2, LInstanceFields;->instanceShortField:S

    invoke-static {v0, v1}, LAssert;->assertEquals(II)V
    return-void
.end method            