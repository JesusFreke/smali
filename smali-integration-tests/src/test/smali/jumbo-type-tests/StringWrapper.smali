.class public LStringWrapper;
.super Ljava/lang/Object;

#the type_id_item index for this class should be <64k

.field private stringValue:Ljava/lang/String;

.method public constructor <init>()V
    .registers 2
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    const-string v0, "test"

    iput-object v0, p0, LStringWrapper;->stringValue:Ljava/lang/String;

    return-void
.end method

.method public constructor <init>(Ljava/lang/String;)V
    .registers 2
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, LStringWrapper;->stringValue:Ljava/lang/String;

    return-void
.end method

.method public getValue()Ljava/lang/String;
    .registers 2

    iget-object v0, p0, LStringWrapper;->stringValue:Ljava/lang/String;

    return-object v0
.end method