.class public LFieldTest;
.super Ljava/lang/Object;
.source "FieldTest.smali"

#this class tests that fields of various types are working

.method public constructor <init>()V
    .registers 4
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    const-string v0, "publicStringFieldValue"
    iput-object v0, p0, LFieldTest;->publicStringField:Ljava/lang/String;

    const v0, 23
    iput v0, p0, LFieldTest;->publicIntegerField:I

    const-string v0, "publicObjectFieldValue"
    iput-object v0, p0, LFieldTest;->publicObjectField:Ljava/lang/Object;

    const/4 v0, 3
    new-array v0, v0, [Ljava/lang/String;

    const-string v1, "publicStringArrayFieldValue1"
    const/4 v2, 0
    aput-object v1, v0, v2

    const-string v1, "publicStringArrayFieldValue2"
    const/4 v2, 1
    aput-object v1, v0, v2

    const-string v1, "pubicStringArrayFieldValue3"
    const/4 v2, 2
    aput-object v1, v0, v2

    iput-object v0, p0, LFieldTest;->publicStringArrayField:[Ljava/lang/String;

    const-string v0, "privateStringFieldValue"
    iput-object v0, p0, LFieldTest;->privateStringField:Ljava/lang/String;

    const-string v0, "publicStaticStringFieldValue"
    sput-object v0, LFieldTest;->publicStaticStringField:Ljava/lang/String;

    const-string v0, "privateStaticStringFieldValue"
    sput-object v0, LFieldTest;->privateStaticStringField:Ljava/lang/String;

    return-void
.end method

.field public publicStringField:Ljava/lang/String;
.field public publicIntegerField:I
.field public publicObjectField:Ljava/lang/Object;
.field public publicStringArrayField:[Ljava/lang/String;
.field private privateStringField:Ljava/lang/String;
.field public static publicStaticStringField:Ljava/lang/String;
.field private static privateStaticStringField:Ljava/lang/String;


.method public test()V
    .registers 5
    .annotation runtime Lorg/junit/Test;
    .end annotation

    .line 1
    iget-object v0, p0, LFieldTest;->publicStringField:Ljava/lang/String;
    const-string v1, "publicStringFieldValue"
    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    .line 2
    iget v0, v4, LFieldTest;->publicIntegerField:I
    const/16 v1, 23
    invoke-static {v0, v1}, LAssert;->assertEquals(II)V

    .line 3
    iget-object v0, p0, LFieldTest;->publicObjectField:Ljava/lang/Object;
    const-string v1, "publicObjectFieldValue"
    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    .line 4
    const/4 v0, 3
    new-array v0, v0, [Ljava/lang/String;

    const-string v1, "publicStringArrayFieldValue1"
    const/4 v2, 0
    aput-object v1, v0, v2

    const-string v1, "publicStringArrayFieldValue2"
    const/4 v2, 1
    aput-object v1, v0, v2

    const-string v1, "pubicStringArrayFieldValue3"
    const/4 v2, 2
    aput-object v1, v0, v2

    iget-object v1, p0, LFieldTest;->publicStringArrayField:[Ljava/lang/String;
    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals([Ljava/lang/Object;[Ljava/lang/Object;)V

    .line 5
    iget-object v0, p0, LFieldTest;->privateStringField:Ljava/lang/String;
    const-string v1, "privateStringFieldValue"
    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    .line 6
    sget-object v0, LFieldTest;->publicStaticStringField:Ljava/lang/String;
    const-string v1, "publicStaticStringFieldValue"
    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    .line 7
    sget-object v0, LFieldTest;->privateStaticStringField:Ljava/lang/String;
    const-string v1, "privateStaticStringFieldValue"
    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

	return-void
.end method