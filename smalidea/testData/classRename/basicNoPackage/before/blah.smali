.class public Lblah;
.super Lblah;
.implements Lblah;

.annotation build Lblah;
    value = .subannotation Lblah;
                value = Lblah;
            .end subannotation
.end annotation

.field static public blah:Lblah; = Lblah;

.method public blah(Lblah;)Lblah;
    .registers 2
    .local p0, "this":Lblah;

    :start
        iget-object v0, v0, Lblah;->blah:Lblah;

        invoke-virtual {v0}, Lblah;->blah(Lblah;)Lblah;

        instance-of v0, v0, Lblah;
        check-cast v0, Lblah;
        new-instance v0, Lblah;
        const-class v0, Lblah;
        throw-verification-error generic-error, Lblah;

        filled-new-array {v0, v0, v0, v0, v0}, Lblah;
        new-array v0, v0, Lblah;
        filled-new-array/range {v0}, Lblah;
    :end

    .catch Lblah; { :start .. :end } :handler
    :handler
    return-void
.end method