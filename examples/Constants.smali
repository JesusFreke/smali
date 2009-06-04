.class public org/JesusFreke/HelloWorld2/HelloWorld2
.super android/app/Activity

.field private static final stringConstant1 Ljava/lang/String; = "Hello World!"
.field private static final stringConstant2 Ljava/lang/String; = ""
.field private static final stringConstant3 Ljava/lang/String; = "a\b\n\f\r\"\'\\\u1234\u0000\u000a\u000d"

.field private static final charConstant1 C = 'a'
.field private static final charConstant2 C = '\b' ;backspace
.field private static final charConstant3 C = '\n'
.field private static final charConstant4 C = '\f' ;formfeed
.field private static final charConstant5 C = '\r'
.field private static final charConstant6 C = '\"'
.field private static final charConstant7 C = '\''
.field private static final charConstant8 C = '\\'
.field private static final charConstant9 C = '\0'
.field private static final charConstant10 C = '\7'
.field private static final charConstant11 C = '\77'
.field private static final charConstant12 C = '\377'

.field private static final intDecConstant1 I = 0
.field private static final intDecConstant2 I = 1
.field private static final intDecConstant3 I = 1000
.field private static final intDecConstant4 I = 1024
.field private static final intDecConstant5 I = 2147483647
.field private static final intDecConstant6 I = -0
.field private static final intDecConstant7 I = -1
.field private static final intDecConstant8 I = -1000
.field private static final intDecConstant9 I = -1024
.field private static final intDecConstant10 I = -2147483648

.field private static final intHexConstant1 I = 0x0
.field private static final intHexConstant2 I = 0x00
.field private static final intHexConstant3 I = 0x1
.field private static final intHexConstant4 I = 0x01
.field private static final intHexConstant5 I = 0x3E8 ;1000
.field private static final intHexConstant6 I = 0x400 ;1024
.field private static final intHexConstant7 I = 0x7fffffff ;2147483647
.field private static final intHexConstant8 I = 0xFFFFFFFF ;-1
.field private static final intHexConstant9 I = 0xFFFFFC18 ;-1000
.field private static final intHexConstant10 I = 0xFFFFFC00 ;-1024
.field private static final intHexConstant11 I = 0x80000000 ;-2147483648

.field private static final longDecConstant1 J = 0L
.field private static final longDecConstant2 J = 1L
.field private static final longDecConstant3 J = 1000L
.field private static final longDecConstant4 J = 1024L
.field private static final longDecConstant5 J = 2147483647L
.field private static final longDecConstant5 J = 2147483648L
.field private static final longDecConstant5 J = 9223372036854775807L
.field private static final longDecConstant6 J = -0L
.field private static final longDecConstant7 J = -1L
.field private static final longDecConstant8 J = -1000L
.field private static final longDecConstant9 J = -1024L
.field private static final longDecConstant10 J = -2147483648L
.field private static final longDecConstant10 J = -2147483649L
.field private static final longDecConstant10 J = -9223372036854775808L

.field private static final longHexConstant1 J = 0x0L
.field private static final longHexConstant2 J = 0x00L
.field private static final longHexConstant3 J = 0x1L
.field private static final longHexConstant4 J = 0x01L
.field private static final longHexConstant5 J = 0x3E8L ;1000
.field private static final longHexConstant6 J = 0x400L ;1024
.field private static final longHexConstant7 J = 0x7fffffffL ;2147483647
.field private static final longHexConstant7 J = 0x80000000L ;2147483648
.field private static final longHexConstant7 J = 0x7fffffffffffffffL ;9223372036854775807
.field private static final longHexConstant8 J = 0xFFFFFFFFFFFFFFFFL ;-1
.field private static final longHexConstant9 J = 0xFFFFFFFFFFFFFC18L ;-1000
.field private static final longHexConstant10 J = 0xFFFFFFFFFFFFFC00L ;-1024
.field private static final longHexConstant11 J = 0xFFFFFFFF80000000L ;-2147483648
.field private static final longHexConstant11 J = 0xFFFFFFFF7FFFFFFFL ;-2147483649
.field private static final longHexConstant12 J = 0x8000000000000000L ;-9223372036854775808

.method public constructor <init>()V
	.registers 1
	invoke-direct	{v0} android/app/Activity.<init>()V
	return-void
.end method

.method public onCreate(Landroid/os/Bundle;)V
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


