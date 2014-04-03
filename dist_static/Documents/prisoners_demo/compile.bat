@echo off
setlocal

cd /D %~dp0
set SAVEDIR="%CD%"
cd /D %ProgramFiles%\Java\jdk*
set JAVA_HOME=%CD%
cd /D %SAVEDIR%
set REPAST=%ProgramFiles%\Repast 3\Repast J

set JAVAC="%JAVA_HOME%\bin\javac"
echo on
%JAVAC% -cp "%REPAST%\repast.jar;%REPAST%\lib\colt.jar;." demo\prisoners\PrisonersAgent.java
%JAVAC% -cp "%REPAST%\repast.jar;%REPAST%\lib\colt.jar;." demo\prisoners\PrisonersModel.java
%JAVAC% -cp "%REPAST%\repast.jar;%REPAST%\lib\colt.jar;." demo\prisoners\PrisonersModelGUI.java

@endlocal
