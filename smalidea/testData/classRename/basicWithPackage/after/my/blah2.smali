.class public Lmy/blah2;
.super Lmy/blah2;
.implements Lmy/blah2;

.annotation build Lmy/blah2;
    value = .subannotation Lmy/blah2;
                value = Lmy/blah2;
            .end subannotation
.end annotation

.field static public blah:Lmy/blah2; = Lmy/blah2;

.method public blah(Lmy/blah2;)Lmy/blah2;
    .registers 2
    .local p0, "this":Lmy/blah2;

    :start
        iget-object v0, v0, Lmy/blah2;->blah:Lmy/blah2;

        invoke-virtual {v0}, Lmy/blah2;->blah(Lmy/blah2;)Lmy/blah2;

        instance-of v0, v0, Lmy/blah2;
        check-cast v0, Lmy/blah2;
        new-instance v0, Lmy/blah2;
        const-class v0, Lmy/blah2;
        throw-verification-error generic-error, Lmy/blah2;

        filled-new-array {v0, v0, v0, v0, v0}, Lmy/blah2;
        new-array v0, v0, Lmy/blah2;
        filled-new-array/range {v0}, Lmy/blah2;
    :end

    .catch Lmy/blah2; { :start .. :end } :handler
    :handler
    return-void
.end method