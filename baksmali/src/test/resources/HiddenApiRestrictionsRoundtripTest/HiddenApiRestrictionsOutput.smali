.class public LHiddenApiRestrictions;
.super Ljava/lang/Object;


# static fields
.field public static whitelist staticField:I


# instance fields
.field public whitelist core-platform-api domainSpecificFlagTest:I

.field public blacklist instanceField:I

.field public whitelist test-api testApiField:I


# direct methods
.method private greylist-max-q core-platform-api corePlatformApiAndHiddenApiTest()V
    .registers 1

    return-void
.end method

.method private whitelist core-platform-api corePlatformApiTest()V
    .registers 1

    return-void
.end method

.method private greylist-max-o directMethod()V
    .registers 1

    return-void
.end method

.method private greylist-max-q core-platform-api test-api testAndCorePlatformApiMethod()V
    .registers 1
    return-void
.end method

.method private greylist-max-q test-api testApiMethod()V
    .registers 1
    return-void
.end method

# virtual methods
.method public blacklist virtualMethod()V
    .registers 1

    return-void
.end method
