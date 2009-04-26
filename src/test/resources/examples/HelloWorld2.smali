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
;But this should!
;But this should!


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
    const-string v1,"But this should!"

    invoke-virtual {v2, v1}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v2

    invoke-virtual {v2, v3}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
	move-result-object v2


	;test format10t with an offset
	goto +3

    const-string	v1, "This shouldn't be displayed!"

    const-string v1,"But this should!"

    invoke-virtual {v2, v1}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v2

    invoke-virtual {v2, v3}, java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
	move-result-object v2



    check-cast  v4, Landroid/app/Activity;

	invoke-virtual	{v0,v2}, android/widget/TextView/setText(Ljava/lang/CharSequence;)V
	invoke-virtual	{v4,v0}, org/JesusFreke/HelloWorld2/HelloWorld2/setContentView(Landroid/view/View;)V

	return-void
.end method


