.class public LFieldTest;
.super Ljava/lang/Object;
.source "FieldTest.smali"

#this class tests that fields of various types are working

.method public constructor <init>()V
    .registers 4
    invoke-direct {v3}, Ljava/lang/Object;-><init>()V

    const-string v0, "publicStringFieldValue"
    iput-object v0, v3, LFieldTest;->publicStringField:Ljava/lang/String;

    const v0, 23
    iput v0, v3, LFieldTest;->publicIntegerField:I

    const-string v0, "publicObjectFieldValue"
    iput-object v0, v3, LFieldTest;->publicObjectField:Ljava/lang/Object;

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

    iput-object v0, v3, LFieldTest;->publicStringArrayField:[Ljava/lang/String;

    const-string v0, "privateStringFieldValue"
    iput-object v0, v3, LFieldTest;->privateStringField:Ljava/lang/String;

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
    iget-object v0, v4, LFieldTest;->publicStringField:Ljava/lang/String;
    const-string v1, "publicStringFieldValue"
    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    .line 2
    iget v0, v4, LFieldTest;->publicIntegerField:I
    int-to-long v0, v0
    const-wide/16 v2, 23
    invoke-static {v0, v1, v2, v3}, Lorg/junit/Assert;->assertEquals(JJ)V

    .line 3
    iget-object v0, v4, LFieldTest;->publicObjectField:Ljava/lang/Object;
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

    iget-object v1, v4, LFieldTest;->publicStringArrayField:[Ljava/lang/String;
    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals([Ljava/lang/Object;[Ljava/lang/Object;)V

    .line 5
    iget-object v0, v4, LFieldTest;->privateStringField:Ljava/lang/String;
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