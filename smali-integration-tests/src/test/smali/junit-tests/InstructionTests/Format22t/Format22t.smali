.class public LFormat22t;
.super Ljava/lang/Object;
.source "Format22t.smali"

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method


.method public test_if-eq()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 23
    const v1, 23

    if-eq v0, v1, :label1

    invoke-static {}, Lorg/junit/Assert;->fail()V

    :label1
    const v1, 24

    if-eq v0, v1, :label2
    return-void

    :label2
    invoke-static {}, Lorg/junit/Assert;->fail()V

    return-void
.end method

.method public test_if-ne()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 23
    const v1, 24

    if-ne v0, v1, :label1

    invoke-static {}, Lorg/junit/Assert;->fail()V

    :label1
    const v1, 23

    if-ne v0, v1, :label2
    return-void

    :label2
    invoke-static {}, Lorg/junit/Assert;->fail()V

    return-void
.end method

.method public test_if-lt()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 23
    const v1, 24

    if-lt v0, v1, :label1

    invoke-static {}, Lorg/junit/Assert;->fail()V

    :label1
    const v1, 23

    if-lt v0, v1, :label2
    return-void

    :label2
    invoke-static {}, Lorg/junit/Assert;->fail()V

    return-void
.end method

.method public test_if-ge()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 23
    const v1, 23

    if-ge v0, v1, :label1

    invoke-static {}, Lorg/junit/Assert;->fail()V

    :label1
    const v1, 24

    if-ge v0, v1, :label2
    return-void

    :label2
    invoke-static {}, Lorg/junit/Assert;->fail()V

    return-void
.end method

.method public test_if-gt()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 23
    const v1, 22

    if-gt v0, v1, :label1

    invoke-static {}, Lorg/junit/Assert;->fail()V

    :label1
    const v1, 23

    if-gt v0, v1, :label2
    return-void

    :label2
    invoke-static {}, Lorg/junit/Assert;->fail()V

    return-void
.end method

.method public test_if-le()V
    .registers 2
    .annotation runtime Lorg/junit/Test;
    .end annotation

    const v0, 23
    const v1, 23

    if-le v0, v1, :label1

    invoke-static {}, Lorg/junit/Assert;->fail()V

    :label1
    const v1, 22

    if-le v0, v1, :label2
    return-void

    :label2
    invoke-static {}, Lorg/junit/Assert;->fail()V

    return-void
.end method