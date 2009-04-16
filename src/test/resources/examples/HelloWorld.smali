.class public HelloWorld
.super java/lang/Object

.method public <init>()V
   .registers 1

   invoke-direct	{v0} java/lang/Object.<init>()V

   return-void
.end method

.method public static main([Ljava/lang/String;)V
   .registers 4

   sget-object	v0 java/lang/System.out Ljava/io/PrintStream;
   const-string v1 "Hello World!"

   invoke-virtual {v0, v1} java/io/PrintStream.print(Ljava/Lang/Stream;)V

   return-void
.end method
