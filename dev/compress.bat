@echo off
REM Creates .\MEME??????-????.rar
REM (source code archive)
REM
setlocal
set RAR="%ProgramFiles%\WinRAR\Rar.exe"
goto :main

:timestr
  for /F "delims=@ tokens=2 usebackq" %%i in (`%RAR% a -ag%1 -tn0s none@.rar %~s0`) do set TIMESTR=%%~ni
  exit /b

:main
call :timestr YYMMDD-HHMM
set ARCHIVE="%~dp0MEME%TIMESTR%.rar"
set PASS=
REM Comment out the following line if you want to encrypt the archive for email transmission
REM set PASS=-hpMEME.rar

cd /D "%~dp0.."
%RAR% a -s -m5 -r -x@"%~dp0exclude.txt" %PASS% %ARCHIVE%
%RAR% a -s -m5 -r %PASS% %ARCHIVE% @"%~dp0include.txt"

REM %RAR% a -s -m5 -r -t -x*.pack* -x*.jar -x*.zip %PASS% %ARCHIVE% 3rdParty\visu

endlocal
