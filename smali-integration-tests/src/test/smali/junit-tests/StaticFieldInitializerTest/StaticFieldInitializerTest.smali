.class public LStaticFieldInitializerTest;
.super Ljava/lang/Object;


.field public static longStaticField:J = 0x300000000L
.field public static longNegStaticField:J = -0x300000000L

.field public static intStaticField:I = 0x70000000
.field public static intNegStaticField:I = -500

.field public static shortStaticField:S = 500s
.field public static shortNegStaticField:S = -500s

.field public static byteStaticField:B = 123t
.field public static byteNegStaticField:B = 0xAAt

.field public static floatStaticField:F = 3.1415926f

.field public static doubleStaticField:D = 3.141592653589793

.field public static charStaticField:C = 'a'
.field public static charEscapedStaticField:C = '\n'

.field public static boolTrueStaticField:Z = true
.field public static boolFalseStaticField:Z = false

.field public static typeStaticField:Ljava/lang/Class; = LStaticFieldInitializerTest;

.field public static aStaticFieldWithoutAnInitializer:I


.method public constructor <init>()V
    .registers 1

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public testLongStaticField()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    sget-wide v0, LStaticFieldInitializerTest;->longStaticField:J

    const-string v2, "12884901888"

    invoke-static {v0, v1}, Ljava/lang/Long;->toString(J)Ljava/lang/String;
    move-result-object v0

    invoke-static {v0, v2}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    return-void
.end method

.method public testLongNegStaticField()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    sget-wide v0, LStaticFieldInitializerTest;->longNegStaticField:J

    const-string v2, "-12884901888"

    invoke-static {v0, v1}, Ljava/lang/Long;->toString(J)Ljava/lang/String;
    move-result-object v0

    invoke-static {v0, v2}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    return-void
.end method

.method public testIntStaticField()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    sget v0, LStaticFieldInitializerTest;->intStaticField:I

    const-string v1, "1879048192"

    invoke-static {v0}, Ljava/lang/Integer;->toString(I)Ljava/lang/String;
    move-result-object v0

    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    return-void
.end method

.method public testIntNegStaticField()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    sget v0, LStaticFieldInitializerTest;->intNegStaticField:I

    const-string v1, "-500"

    invoke-static {v0}, Ljava/lang/Integer;->toString(I)Ljava/lang/String;
    move-result-object v0

    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    return-void
.end method

.method public testShortStaticField()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    sget-short v0, LStaticFieldInitializerTest;->shortStaticField:S

    const-string v1, "500"

    invoke-static {v0}, Ljava/lang/Integer;->toString(I)Ljava/lang/String;
    move-result-object v0

    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    return-void
.end method

.method public testShortNegStaticField()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    sget-short v0, LStaticFieldInitializerTest;->shortNegStaticField:S

    const-string v1, "-500"

    invoke-static {v0}, Ljava/lang/Integer;->toString(I)Ljava/lang/String;
    move-result-object v0

    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    return-void
.end method

.method public testByteStaticField()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    sget-byte v0, LStaticFieldInitializerTest;->byteStaticField:B

    const-string v1, "123"

    invoke-static {v0}, Ljava/lang/Integer;->toString(I)Ljava/lang/String;
    move-result-object v0

    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    return-void
.end method

.method public testByteNegStaticField()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    sget-byte v0, LStaticFieldInitializerTest;->byteNegStaticField:B

    const-string v1, "-86"

    invoke-static {v0}, Ljava/lang/Integer;->toString(I)Ljava/lang/String;
    move-result-object v0

    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    return-void
.end method

.method public testFloatStaticField()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    sget v0, LStaticFieldInitializerTest;->floatStaticField:F

    const-string v1, "3.1415925"

    invoke-static {v0}, Ljava/lang/Float;->toString(F)Ljava/lang/String;
    move-result-object v0

    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    return-void
.end method

.method public testDoubleStaticField()V
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation

    sget-wide v0, LStaticFieldInitializerTest;->doubleStaticField:D

    const-string v2, "3.141592653589793"

    invoke-static {v0, v1}, Ljava/lang/Double;->toString(D)Ljava/lang/String;
    move-result-object v0

    invoke-static {v0, v2}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    return-void
.end method

.method public testCharStaticField()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    sget-char v0, LStaticFieldInitializerTest;->charStaticField:C

    const-string v1, "a"

    invoke-static {v0}, Ljava/lang/Character;->toString(C)Ljava/lang/String;
    move-result-object v0

    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    return-void
.end method

.method public testCharEscapedStaticField()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    sget-char v0, LStaticFieldInitializerTest;->charEscapedStaticField:C

    const-string v1, "\n"

    invoke-static {v0}, Ljava/lang/Character;->toString(C)Ljava/lang/String;
    move-result-object v0

    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    return-void
.end method

.method public testBoolTrueStaticField()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    sget-boolean v0, LStaticFieldInitializerTest;->boolTrueStaticField:Z

    const-string v1, "true"

    invoke-static {v0}, Ljava/lang/Boolean;->toString(Z)Ljava/lang/String;
    move-result-object v0

    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    return-void
.end method

.method public testBoolFalseStaticField()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    sget-boolean v0, LStaticFieldInitializerTest;->boolFalseStaticField:Z

    const-string v1, "false"

    invoke-static {v0}, Ljava/lang/Boolean;->toString(Z)Ljava/lang/String;
    move-result-object v0

    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    return-void
.end method

.method public testTypeStaticField()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    sget-object v0, LStaticFieldInitializerTest;->typeStaticField:Ljava/lang/Class;

    const-string v1, "class StaticFieldInitializerTest"

    invoke-virtual {v0}, Ljava/lang/Class;->toString()Ljava/lang/String;
    move-result-object v0

    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    return-void
.end method

.method public testAStaticFieldWithoutAnInitializer()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    sget v0, LStaticFieldInitializerTest;->aStaticFieldWithoutAnInitializer:I

    const-string v1, "0"

    invoke-static {v0}, Ljava/lang/Integer;->toString(I)Ljava/lang/String;
    move-result-object v0

    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    return-void
.end method