.class public org/JesusFreke/HelloWorld2/HelloWorld2
.super android/app/Activity

.field private static final final Ljava/lang/String;
.field private static final static I
.field private static final 1234 I
.field private static final 1234-5678 I
.field private static final 1E1000 I
.field private static final 1E-1000 I
.field private static final return I
.field private static final new-instance I
.field private static final <test> I
.field private static final <test$abcd> I
.field private static final test$abcd I


.method public constructor <init>()V
	.registers 1
	invoke-direct	{v0} android/app/Activity.<init>()V
	return-void
.end method

.method public 1E-2000(Landroid/os/Bundle;)V
	.registers 4

	sget-object v0 java/lang/System.out Ljava/io/PrintStream;

	invoke-super	{v2,v3} android/app/Activity.onCreate(Landroid/os/Bundle;)V

	new-instance	v0 android/widget/TextView
	invoke-direct	{v0,v2} android/widget/TextView.<init>(Landroid/content/Context;)V
	const-string	v1 "Hello World!"
	invoke-virtual	{v0,v1} android/widget/TextView.setText(Ljava/lang/CharSequence;)V
	invoke-virtual	{v2,v0} org/JesusFreke/HelloWorld2/HelloWorld2.setContentView(Landroid/view/View;)V

	return-void
.end method


