@echo off
rem Runs MEME.

javaw.exe -Xmx512m -javaagent:lib/aspectjweaver.jar -Djava.library.path=./lib/j3d -jar MEME.jar