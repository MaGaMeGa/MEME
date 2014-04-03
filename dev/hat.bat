@echo off
rem
rem  This is a *really* minimal launch program for Win32. 
rem  You probably want to modify it  with an absolute path
rem  to where you install the hat classes.
rem

REM java -jar hat.jar %1 %2 %3 %4 %5 %6 %7 %8 %9

setlocal
cd /D "%~dp0"
java -Xmx256m -jar hat.jar memory.hprof %*
REM compare more
REM java -Xmx256m -jar hat.jar -baseline memory.hprof#1 memory.hprof#2 %*
endlocal
