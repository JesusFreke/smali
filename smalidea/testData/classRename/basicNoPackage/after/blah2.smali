.class public Lblah2;
.super Lblah2;
.implements Lblah2;

.annotation build Lblah2;
    value = .subannotation Lblah2;
                value = Lblah2;
            .end subannotation
.end annotation

.field static public blah:Lblah2; = Lblah2;

.method public blah(Lblah2;)Lblah2;
    .registers 2
    .local p0, "this":Lblah2;

    :start
        iget-object v0, v0, Lblah2;->blah:Lblah2;

        invoke-virtual {v0}, Lblah2;->blah(Lblah2;)Lblah2;

        instance-of v0, v0, Lblah2;
        check-cast v0, Lblah2;
        new-instance v0, Lblah2;
        const-class v0, Lblah2;
        throw-verification-error generic-error, Lblah2;

        filled-new-array {v0, v0, v0, v0, v0}, Lblah2;
        new-array v0, v0, Lblah2;
        filled-new-array/range {v0}, Lblah2;
    :end

    .catch Lblah2; { :start .. :end } :handler
    :handler
    return-void
.end method