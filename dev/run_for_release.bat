@echo off
setlocal
cd /D %~dp0
if not exist MEME.jar java -version:1.5+ -cp . Unpacker
java -version:1.5+ -Xmx256m -jar MEME.jar

endlocal