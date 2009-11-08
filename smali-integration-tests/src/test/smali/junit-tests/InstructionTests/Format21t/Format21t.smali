.class public LFormat21t;
.super Ljava/lang/Object;
.source "Format21t.smali"

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public test_if-eqz()V
    .registers 1
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const/4 v0, 0
           
    if-eqz v0, :label1

    invoke-static {}, Lorg/junit/Assert;->fail()V

    :label1
    const/4 v0, 1

    if-eqz v0, :label2
    return-void

    :label2
    invoke-static {}, Lorg/junit/Assert;->fail()V

    return-void
.end method


.method public test_if-nez()V
    .registers 1
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const/4 v0, 1

    if-nez v0, :label1

    invoke-static {}, Lorg/junit/Assert;->fail()V

    :label1
    const/4 v0, 0

    if-nez v0, :label2
    return-void

    :label2
    invoke-static {}, Lorg/junit/Assert;->fail()V

    return-void
.end method

.method public test_if-ltz()V
    .registers 1
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const/4 v0, -1

    if-ltz v0, :label1

    invoke-static {}, Lorg/junit/Assert;->fail()V

    :label1
    const/4 v0, 0

    if-ltz v0, :label2
    return-void

    :label2
    invoke-static {}, Lorg/junit/Assert;->fail()V

    return-void
.end method

.method public test_if-gez()V
    .registers 1
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const/4 v0, 0

    if-gez v0, :label1

    invoke-static {}, Lorg/junit/Assert;->fail()V

    :label1
    const/4 v0, -1

    if-gez v0, :label2
    return-void

    :label2
    invoke-static {}, Lorg/junit/Assert;->fail()V

    return-void
.end method

.method public test_if-gtz()V
    .registers 1
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const/4 v0, 1

    if-gtz v0, :label1

    invoke-static {}, Lorg/junit/Assert;->fail()V

    :label1
    const/4 v0, 0

    if-gtz v0, :label2
    return-void

    :label2
    invoke-static {}, Lorg/junit/Assert;->fail()V

    return-void
.end method


.method public test_if-lez()V
    .registers 1
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const/4 v0, 0

    if-lez v0, :label1

    invoke-static {}, Lorg/junit/Assert;->fail()V

    :label1
    const/4 v0, 1

    if-lez v0, :label2
    return-void

    :label2
    invoke-static {}, Lorg/junit/Assert;->fail()V

    return-void
.end method