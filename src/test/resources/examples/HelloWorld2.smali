.class public Lorg/JesusFreke/HelloWorld2/HelloWorld2;
.super Landroid/app/Activity;

.field private helloWorld Ljava/lang/String;
.field private static helloWorldStatic Ljava/lang/String;

.field private static helloWorldStatic2 Ljava/lang/String; = "Static Initializer Hello World!"

;This class should display the following text to the screen:
;
;Hello World!
;Static Hello World!
;Static Initializer Hello World!
;org/JesusFreke/HelloWorld2/HelloWorld2
;[Lorg/JesusFreke/HelloWorld2/HelloWorld2;
;[I
;0
;-8
;7
;Format10t with a label
;Format10t with an offset
;Format20t with a label
;Format30t with a label
;Testing Format22x and Format32x
;Testing Format21t
;-32768



.method static constructor <clinit>()V ;test
    .registers 1

    const-string v0, "Static Hello World!"
    sput-object v0, org/JesusFreke/HelloWorld2/HelloWorld2/helloWorldStatic Ljava/lang/String; ;test

    return-void
.end method

.method public constructor <init>()V
	.registers 2
	invoke-direct	{v1}, android/app/Activity/<init>()V

	const-string	v0, "Hello World!"
	iput-object v0, v1, org/JesusFreke/HelloWorld2/HelloWorld2/helloWorld Ljava/lang/String;;test

	return-void
.end method


.method public largeRegisterTest()Ljava/lang/String;
    .registers 1235

    const-string v1, "Testing Format22x and Format32x"
	move-object/16 v1234, v1

    const-string v1, "This shouldn't be displayed!"
    move-object/from16 v1, v1234

    return-object v1
.end method

.method public testFormat21t()Ljava/lang/String;
    .registers 3

    const-string v0, "Testing Format21t"
    const-string v1, "This shouldn't be displayed!"

    const/4 v2, 0

    if-eqz v2, HERE:

    return-object v1

HERE:
    return-object v0    
.end method


.method public testFormat21s()Ljava/lang/String;
    .registers 2

    const/16 v0, -32768

    invoke-static	{v0}, java/lang/Integer/toString(I)Ljava/lang/String;
    move-result-object v1

    return-object v1
.end method

.method public onCreate(Landroid/os/Bundle;)V
	.registers 6

	invoke-super	{v4,v5}, android/app/Activity/onCreate(Landroid/os/Bundle;)V

	const-string    v3, "\n"

	new-instance	v0, Landroid/widget/TextView;
	invoke-direct	{v0,v4}, android/widget/TextView/<init>(Landroid/content/Context;)V

	iget-object v1, v4, org/JesusFreke/HelloWorld2/HelloWorld2/helloWorld Ljava/lang/String;

    invoke-virtual {v1, v3}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
	move-result-object v1

    sget-object v2, org/JesusFreke/HelloWorld2/HelloWorld2/helloWorldStatic Ljava/lang/String;
	invoke-virtual {v1, v2}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
	move-result-object v1

	invoke-virtual {v1, v3}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
	move-result-object v1

	sget-object v2, org/JesusFreke/HelloWorld2/HelloWorld2/helloWorldStatic2 Ljava/lang/String;
	invoke-virtual/range {v1 .. v2}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v1


    invoke-virtual {v1, v3}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
	move-result-object v1

	const-class v2, Lorg/JesusFreke/HelloWorld2/HelloWorld2;
    invoke-virtual	{v2}, java/lang/Class/getName()Ljava/lang/String;
    move-result-object v2

    invoke-virtual/range {v1 .. v2}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v1



    invoke-virtual {v1, v3}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
	move-result-object v1

	const-class v2, [Lorg/JesusFreke/HelloWorld2/HelloWorld2;
    invoke-virtual	{v2}, java/lang/Class/getName()Ljava/lang/String;
    move-result-object v2

    invoke-virtual/range {v1 .. v2}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v1



    invoke-virtual {v1, v3}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
	move-result-object v1

	const-class v2, [I
    invoke-virtual	{v2}, java/lang/Class/getName()Ljava/lang/String;
    move-result-object v2

    invoke-virtual/range {v1 .. v2}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v1

    move-object v2, v1

    invoke-virtual {v2, v3}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
	move-result-object v2


    ;test out Format11n, with various literals
    ;with 0
    const/4 v1, 0
    invoke-static	{v1}, java/lang/Integer/toString(I)Ljava/lang/String;
    move-result-object v1

    invoke-virtual {v2, v1}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v2

    invoke-virtual {v2, v3}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
	move-result-object v2


    ;with -8
    const/4 v1, -8
    invoke-static	{v1}, java/lang/Integer/toString(I)Ljava/lang/String;
    move-result-object v1

    invoke-virtual {v2, v1}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v2

    invoke-virtual {v2, v3}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
	move-result-object v2


    ;with 7
    const/4 v1, 7
    invoke-static	{v1}, java/lang/Integer/toString(I)Ljava/lang/String;
    move-result-object v1

    invoke-virtual {v2, v1}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v2

    invoke-virtual {v2, v3}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
	move-result-object v2



    ;test format10t with a label
    goto SKIP:

    const-string	v1, "This shouldn't be displayed!"

    SKIP:
    const-string v1,"Format10t with a label"

    invoke-virtual {v2, v1}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v2

    invoke-virtual {v2, v3}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
	move-result-object v2


	;test format10t with an offset
	goto +3

    const-string	v1, "This shouldn't be displayed!"

    const-string v1,"Format10t with an offset"

    invoke-virtual {v2, v1}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v2

    invoke-virtual {v2, v3}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
	move-result-object v2



    ;test format20t with a label
    goto/16 SKIP2:

    const-string	v1, "This shouldn't be displayed!"

    SKIP2:
    const-string v1,"Format20t with a label"

    invoke-virtual {v2, v1}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v2

    invoke-virtual {v2, v3}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
	move-result-object v2


    ;test format30t with a label
    goto/32 SKIP3:

    const-string	v1, "This shouldn't be displayed!"

    SKIP3:
    const-string v1,"Format30t with a label"

    invoke-virtual {v2, v1}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v2

    invoke-virtual {v2, v3}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
	move-result-object v2



	;test format22x and format32x
    invoke-virtual {v4}, org/JesusFreke/HelloWorld2/HelloWorld2/largeRegisterTest()Ljava/lang/String;
    move-result-object v1

	invoke-virtual {v2, v1}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v2

    invoke-virtual {v2, v3}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
	move-result-object v2



    ;test format21t
    invoke-virtual {v4}, org/JesusFreke/HelloWorld2/HelloWorld2/testFormat21t()Ljava/lang/String;
    move-result-object v1

	invoke-virtual {v2, v1}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v2

    invoke-virtual {v2, v3}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
	move-result-object v2



    ;test format21s
    invoke-virtual {v4}, org/JesusFreke/HelloWorld2/HelloWorld2/testFormat21s()Ljava/lang/String;
    move-result-object v1

	invoke-virtual {v2, v1}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v2

    invoke-virtual {v2, v3}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
	move-result-object v2


    check-cast  v4, Landroid/app/Activity;

	invoke-virtual	{v0,v2}, android/widget/TextView/setText(Ljava/lang/CharSequence;)V
	invoke-virtual	{v4,v0}, org/JesusFreke/HelloWorld2/HelloWorld2/setContentView(Landroid/view/View;)V

	return-void
.end method


