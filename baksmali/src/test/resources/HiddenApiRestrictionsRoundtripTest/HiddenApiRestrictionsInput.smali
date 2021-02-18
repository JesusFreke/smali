.class public LHiddenApiRestrictions;
.super Ljava/lang/Object;

.field public static whitelist staticField:I

.field public core-platform-api domainSpecificFlagTest:I

.field public blacklist instanceField:I

.field public test-api testApiField:I

.method public blacklist virtualMethod()V
    .registers 1
    return-void
.end method

.method private greylist-max-o directMethod()V
    .registers 1
    return-void
.end method

.method private core-platform-api corePlatformApiTest()V
    .registers 1
    return-void
.end method

.method greylist-max-q private core-platform-api corePlatformApiAndHiddenApiTest()V
    .registers 1
    return-void
.end method

.method greylist-max-q private test-api testApiMethod()V
    .registers 1
    return-void
.end method

.method greylist-max-q private test-api core-platform-api testAndCorePlatformApiMethod()V
    .registers 1
    return-void
.end method

