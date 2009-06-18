.class public Lbaksmali/test/class;
.super Ljava/lang/Object;

.implements Lsome/interface;
.implements Lsome/other/interface;


.annotation build Lsome/annotation;
    value1 = "test"
    value2 = .subannotation Lsome/annotation;
        value1 = "test2"
        value2 = Lsome/enum;
    .end subannotation
.end annotation

.annotation system Lsome/annotation;
.end annotation



.field public static aStaticFieldWithoutAnInitializer:I

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

.field public static typeStaticField:Ljava/lang/Class; = Lbaksmali/test/class;

.field public static stringStaticField:Ljava/lang/String; = "test"
.field public static stringEscapedStaticField:Ljava/lang/String; = "test\ntest"


.field public static fieldStaticField:Ljava/lang/reflect/Field; = Lbaksmali/test/class;->fieldStaticField:Ljava/lang/reflect/Field;

.field public static methodStaticField:Ljava/lang/reflect/Method; = Lbaksmali/test/class;->teshMethod(ILjava/lang/String;)Ljava/lang/String;

.field public static arrayStaticField:[I = {1, 2, 3, {1, 2, 3, 4}}

.field public static enumStaticField:Lsome/enum; = .enum Lsome/enum;->someEnumValue:Lsome/enum;

.field public static annotationStaticField:Lsome/annotation; = .subannotation Lsome/annotation;
    value1 = "test"
    value2 = .subannotation Lsome/annotation;
        value1 = "test2"
        value2 = Lsome/enum;
    .end subannotation
.end subannotation

.field public instanceField:Ljava/lang/String;



.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public testMethod(ILjava/lang/String;)Ljava/lang/String;
    .registers 3
    .annotation runtime Lorg/junit/Test;
    .end annotation
    .annotation system Lyet/another/annotation;
        somevalue = 1234
        anothervalue = 3.14159
    .end annotation

    const-string v0, "testing\n123"

    goto switch:
    
    sget v0, Lbaksmali/test/class;->staticField:I

    switch:
    packed-switch v0, pswitch:

    const/4 v0, 7
    const v0, 10

    Label10:
    Label11:
    Label12:
    Label13:
    return-object v0

    .array-data 4
        1 2 3 4 5 6 200
    .end array-data

    pswitch:
    .packed-switch 10
        Label10:
        Label11:
        Label12:
        Label13:
    .end packed-switch

.end method

.method public abstract testMethod2()V
    .annotation runtime Lsome/annotation;
        subannotation = .subannotation Lsome/other/annotation;
            value = "value"
        .end subannotation
    .end annotation
    .annotation runtime Lorg/junit/Test;
    .end annotation
.end method

