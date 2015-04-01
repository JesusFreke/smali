.class public Lblah;
.super Ljava/lang/Object;

.annotation runtime Lblah;
    element = Lblah;->blort()V;
.end annotation

.method public blort()V
    .registers 2

    invoke-direct {v0}, Lblah;->blort()V
    invoke-direct/empty {v0}, Lblah;->blort()V
    invoke-direct/range {v0}, Lblah;->blort()V
    invoke-interface {v0}, Lblah;->blort()V
    invoke-interface/range {v0}, Lblah;->blort()V
    invoke-object-init/range {v0}, Lblah;->blort()V
    invoke-static {v0}, Lblah;->blort()V
    invoke-static/range {v0}, Lblah;->blort()V
    invoke-super {v0}, Lblah;->blort()V
    invoke-super/range {v0}, Lblah;->blort()V
    invoke-virtual {v0}, Lblah;->blort()V
    invoke-virtual/range {v0}, Lblah;->blort()V

    throw-verification-error generic-error, Lblah;->blort()V

    return-void
.end method
