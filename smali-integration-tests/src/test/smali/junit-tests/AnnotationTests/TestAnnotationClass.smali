.class public abstract interface annotation LTestAnnotationClass;
.super Ljava/lang/Object;
.implements Ljava/lang/annotation/Annotation;

.method public abstract anotherStringValue()Ljava/lang/String;
.end method

.method public abstract stringValue()Ljava/lang/String;
.end method

.annotation system Ldalvik/annotation/AnnotationDefault;
    value = .subannotation LAnnotationWithValues;
                anotherStringValue = "Another String Value"
                stringValue = "Test Annotation String Value"
            .end subannotation
.end annotation