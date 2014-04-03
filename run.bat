@echo off
rem Runs MEME.
setlocal

REM Set Look & Feel style
set LAF=
REM Metal
REM set LAF=-Dswing.defaultlaf=javax.swing.plaf.metal.MetalLookAndFeel
REM set LAF=-Dswing.defaultlaf=javax.swing.plaf.metal.MetalLookAndFeel -Dswing.metalTheme=steel
REM - set LAF=-Dswing.defaultlaf=javax.swing.plaf.multi.MultiLookAndFeel
REM - set LAF=-Dswing.defaultlaf=javax.swing.plaf.synth.SynthLookAndFeel
REM set LAF=-Dswing.defaultlaf=com.sun.java.swing.plaf.motif.MotifLookAndFeel
REM - set LAF=-Dswing.defaultlaf=com.sun.java.swing.plaf.gtk.GTKLookAndFeel
REM set LAF=-Dswing.defaultlaf=com.sun.java.swing.plaf.windows.WindowsLookAndFeel 
REM set LAF=-Dswing.defaultlaf=com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel

cd /D "%~dp0"
set MEME=%CD%

set DUMP=
if not "-dump" == "%~1" goto :nodump
set DUMP="-agentlib:hprof=file=%MEME%\dev\memory.hprof,format=b"
shift 1
:nodump
goto :main

:appendclspath
set CLASSPATH=%CLASSPATH%;%~1
exit /b

:main
set CLASSPATH=%MEME%/MEMEApp;%MEME%/3rdParty/Activation;%MEME%/3rdParty/Activation/lib/jug-lgpl-2.0.0.jar;%MEME%/3rdParty/hsqldb/lib/hsqldb.jar
for %%i in (%MEME%\3rdParty\*.jar) do call :appendclspath %%i
call :appendclspath %MEME%\Plugins\BeanShell\bsh-2.0b4.jar
REM start "MEME"
java -version:1.5+ %DUMP% %LAF% -ea -Xmx256m ai.aitia.meme.MEMEApp -javaagent:%MEME%\lib\aspectjweaver.jar %1 %2 %3 %4 %5 %6 %7 %8 %9

endlocal
