.class public org/JesusFreke/HelloWorld2/HelloWorld2
.super android/app/Activity

.method public constructor <init>()V
	.registers 1
	invoke-direct	{v1} android/app/Activity.<init>()V

	return-void
.end method

.method public onCreate(Landroid/os/Bundle;)V
	.registers 5

	invoke-super	{v3,v4} android/app/Activity.onCreate(Landroid/os/Bundle;)V

	const-string	v1 "Hello World!"

	new-instance	v0 android/widget/TextView
	invoke-direct	{v0,v3} android/widget/TextView.<init>(Landroid/content/Context;)V


	invoke-virtual	{v0,v1} android/widget/TextView.setText(Ljava/lang/CharSequence;)V
	invoke-virtual	{v3,v0} org/JesusFreke/HelloWorld2/HelloWorld2.setContentView(Landroid/view/View;)V

	return-void
.end method


