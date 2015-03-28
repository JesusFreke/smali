.class public Lmy/blah;
.super Lmy/blah;
.implements Lmy/blah;

.annotation build Lmy/blah;
    value = .subannotation Lmy/blah;
                value = Lmy/blah;
            .end subannotation
.end annotation

.field static public blah:Lmy/blah; = Lmy/blah;

.method public blah(Lmy/blah;)Lmy/blah;
    .registers 2
    .local p0, "this":Lmy/blah;

    :start
        iget-object v0, v0, Lmy/blah;->blah:Lmy/blah;

        invoke-virtual {v0}, Lmy/blah;->blah(Lmy/blah;)Lmy/blah;

        instance-of v0, v0, Lmy/blah;
        check-cast v0, Lmy/blah;
        new-instance v0, Lmy/blah;
        const-class v0, Lmy/blah;
        throw-verification-error generic-error, Lmy/blah;

        filled-new-array {v0, v0, v0, v0, v0}, Lmy/blah;
        new-array v0, v0, Lmy/blah;
        filled-new-array/range {v0}, Lmy/blah;
    :end

    .catch Lmy/blah; { :start .. :end } :handler
    :handler
    return-void
.end method