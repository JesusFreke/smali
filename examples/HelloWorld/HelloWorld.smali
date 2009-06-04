.class public LHelloWorld;

;Ye olde hello world application
;To assemble and run this on a phone or emulator:
;
;java -jar smali.jar --dex HelloWorld.smali
;zip HelloWorld.zip classes.dex
;adb push HelloWorld.zip /data/local
;adb shell dalvikvm -cp /data/local/HelloWorld.zip HelloWorld

.super Ljava/lang/Object;

.method public static main([Ljava/lang/String;)V
    .registers 2

    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;

    const-string	v1, "Hello World!"

    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V

	return-void
.end method