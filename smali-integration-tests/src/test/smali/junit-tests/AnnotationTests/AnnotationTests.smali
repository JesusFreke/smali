.class public LAnnotationTests;
.super Ljava/lang/Object;
.source "AnnotationTests.smali"

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.annotation runtime LTestAnnotationClass;
    stringValue = "Class Annotation Test"
.end annotation


.field public testField:I
    .annotation runtime LTestAnnotationClass;
        stringValue = "Field Annotation Test"
    .end annotation
.end field

.method public testClassAnnotation()V
    .registers 2

    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-class v0, LAnnotationTests;
    const-class v1, LTestAnnotationClass;

    invoke-virtual {v0, v1}, Ljava/lang/Class;->getAnnotation(Ljava/lang/Class;)Ljava/lang/annotation/Annotation;
    move-result-object v0

    check-cast v0, LTestAnnotationClass;

    invoke-interface {v0}, LTestAnnotationClass;->stringValue()Ljava/lang/String;
    move-result-object v0

    const-string v1, "Class Annotation Test"

    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    return-void
.end method

.method public testFieldAnnotation()V
    .registers 4

    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-class v0, LAnnotationTests;
    const-class v1, LTestAnnotationClass;
    const-string v2, "testField"

    invoke-virtual {v0, v2}, Ljava/lang/Class;->getField(Ljava/lang/String;)Ljava/lang/reflect/Field;
    move-result-object v3

    invoke-virtual {v3, v1}, Ljava/lang/reflect/Field;->getAnnotation(Ljava/lang/Class;)Ljava/lang/annotation/Annotation;
    move-result-object v0

    check-cast v0, LTestAnnotationClass;

    invoke-interface {v0}, LTestAnnotationClass;->stringValue()Ljava/lang/String;
    move-result-object v0

    const-string v1, "Field Annotation Test"

    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    return-void
.end method

.method public testMethodAnnotation()V
    .registers 4

    .annotation runtime Lorg/junit/Test;
    .end annotation

    .annotation runtime LTestAnnotationClass;
        stringValue = "Method Annotation Test"
    .end annotation


    const-class v0, LAnnotationTests;
    const-class v1, LTestAnnotationClass;
    const-string v2, "testMethodAnnotation"

    const v3, 0
    new-array v3, v3, [Ljava/lang/Class;

    invoke-virtual {v0, v2, v3}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;
    move-result-object v3

    invoke-virtual {v3, v1}, Ljava/lang/reflect/Method;->getAnnotation(Ljava/lang/Class;)Ljava/lang/annotation/Annotation;
    move-result-object v0

    check-cast v0, LTestAnnotationClass;

    invoke-interface {v0}, LTestAnnotationClass;->stringValue()Ljava/lang/String;
    move-result-object v0

    const-string v1, "Method Annotation Test"

    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    return-void
.end method

.method public testMethodWithParameterAnnotation(Ljava/lang/String;)V
    .registers 2

    .parameter "test"
        .annotation runtime LTestAnnotationClass;
            stringValue = "Parameter Annotation Test"
        .end annotation
    .end parameter

    return-void
.end method

.method public testParameterAnnotation()V
    .registers 6

    .annotation runtime Lorg/junit/Test;
    .end annotation

    const-class v0, LAnnotationTests;
    const-class v1, LTestAnnotationClass;
    const-string v2, "testMethodWithParameterAnnotation"

    const v3, 1
    new-array v3, v3, [Ljava/lang/Class;

    const v4, 0

    const-class v5, Ljava/lang/String;

    aput-object v5, v3, v4

    invoke-virtual {v0, v2, v3}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;
    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/reflect/Method;->getParameterAnnotations()[[Ljava/lang/annotation/Annotation;
    move-result-object v0

    const v1, 0

    aget-object v2, v0, v1
    aget-object v0, v2, v1

    check-cast v0, LTestAnnotationClass;

    invoke-interface {v0}, LTestAnnotationClass;->stringValue()Ljava/lang/String;
    move-result-object v0

    const-string v1, "Parameter Annotation Test"

    invoke-static {v0, v1}, Lorg/junit/Assert;->assertEquals(Ljava/lang/Object;Ljava/lang/Object;)V

    return-void
.end method