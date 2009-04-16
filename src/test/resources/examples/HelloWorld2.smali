.class public org/JesusFreke/HelloWorld2/HelloWorld2
.super android/app/Activity

.field private helloWorld Ljava/lang/String;
.field private static helloWorldStatic Ljava/lang/String;

.field private static helloWorldStatic2 Ljava/lang/String; = "Static Initializer Hello World!"

.method static constructor <clinit>()V
    .registers 1

    const-string v0 "Static Hello World!"
    sput-object v0 org/JesusFreke/HelloWorld2/HelloWorld2.helloWorldStatic Ljava/lang/String;

    return-void
.end method

.method public constructor <init>()V
	.registers 2
	invoke-direct	{v1} android/app/Activity.<init>()V

	const-string	v0 "Hello World!"
	iput-object v0 v1 org/JesusFreke/HelloWorld2/HelloWorld2.helloWorld Ljava/lang/String;
	 
	return-void
.end method

.method public onCreate(Landroid/os/Bundle;)V
	.registers 5

	invoke-super	{v3,v4} android/app/Activity.onCreate(Landroid/os/Bundle;)V

	new-instance	v0 android/widget/TextView
	invoke-direct	{v0,v3} android/widget/TextView.<init>(Landroid/content/Context;)V

	iget-object v1 v3 org/JesusFreke/HelloWorld2/HelloWorld2.helloWorld Ljava/lang/String;

    sget-object v2 org/JesusFreke/HelloWorld2/HelloWorld2.helloWorldStatic Ljava/lang/String;	
	invoke-virtual {v1, v2} java/lang/String.concat(Ljava/lang/String;)Ljava/lang/String;

	move-result-object v1

	sget-object v2 org/JesusFreke/HelloWorld2/HelloWorld2.helloWorldStatic2 Ljava/lang/String;
	invoke-virtual/range {v1 .. v2} java/lang/String.concat(Ljava/lang/String;)Ljava/lang/String;

	move-result-object v1

	invoke-virtual	{v0,v1} android/widget/TextView.setText(Ljava/lang/CharSequence;)V
	invoke-virtual	{v3,v0} org/JesusFreke/HelloWorld2/HelloWorld2.setContentView(Landroid/view/View;)V

	return-void
.end method


