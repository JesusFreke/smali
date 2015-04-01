.class public Lblah;
.super Ljava/lang/Object;

.annotation runtime Lblah;
    element = Lblah;->blah()V;
.end annotation

.method public blah()V
    .registers 2

    invoke-direct {v0}, Lblah;->blah()V
    invoke-direct/empty {v0}, Lblah;->blah()V
    invoke-direct/range {v0}, Lblah;->blah()V
    invoke-interface {v0}, Lblah;->blah()V
    invoke-interface/range {v0}, Lblah;->blah()V
    invoke-object-init/range {v0}, Lblah;->blah()V
    invoke-static {v0}, Lblah;->blah()V
    invoke-static/range {v0}, Lblah;->blah()V
    invoke-super {v0}, Lblah;->blah()V
    invoke-super/range {v0}, Lblah;->blah()V
    invoke-virtual {v0}, Lblah;->blah()V
    invoke-virtual/range {v0}, Lblah;->blah()V

    throw-verification-error generic-error, Lblah;->blah()V

    return-void
.end method
