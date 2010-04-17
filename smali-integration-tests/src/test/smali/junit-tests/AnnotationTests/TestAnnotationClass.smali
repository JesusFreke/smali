.class public abstract interface annotation LTestAnnotationClass;
.super Ljava/lang/Object;
.implements Ljava/lang/annotation/Annotation;

.method public abstract stringValue()Ljava/lang/String;
.end method

.annotation system Ldalvik/annotation/AnnotationDefault;
    value = .subannotation LAnnotationWithValues;
                stringValue = "Test Annotation String Value"
            .end subannotation
.end annotation