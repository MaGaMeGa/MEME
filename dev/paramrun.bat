@echo off
setlocal
cd /D %~dp0
if not exist MEME.jar java -version:1.5+ -cp . Unpacker
set heap=256
if not (%1)==() set heap=%1
java -version:1.5+ -Xmx%heap%m -jar MEME.jar

endlocal