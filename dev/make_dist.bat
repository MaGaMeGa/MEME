@echo off
REM Creates .\MEME_setup.exe
REM (for distribution to end-users)
REM
REM Precondition: ..\MEMEApp\build.xml and ..\Plugins\build.xml
REM + 7-Zip 4.42 is installed.
REM
setlocal

set _7z="%ProgramFiles%\7-Zip\7z.exe"

cd /D "%~dp0"
md releasetmp
md releasetmp\db
md releasetmp\Documents
md releasetmp\Plugins
md releasetmp\lib

copy ..\MEME.pack                       releasetmp
copy ..\3rdParty\Activation\lib\*.dll   releasetmp\lib
REM copy ..\3rdParty\Activation\lib\lib*.*  releasetmp\lib
copy ..\Plugins\*.pack*                 releasetmp\Plugins
copy ..\Documents\MEME_Manual.pdf   releasetmp\Documents
copy ..\Documents\MEME_Tutorial.pdf releasetmp\Documents
copy ..\Documents\MEME_Beanshell.pdf releasetmp\Documents
copy Unpacker.class                     releasetmp
copy launcher.exe                       releasetmp

copy ..\db\meme.* releasetmp\db

cd releasetmp

set TARGET=%~dp0MEME_demo
del "%TARGET%.7z" 2>nul

%_7z% a -r -mx=9 -mmt=off "%TARGET%" *

cd ..
rd /s/q releasetmp
copy /b 7zSD.sfx + 7zsfx.script + "%TARGET%.7z" "%TARGET%.exe"
del "%TARGET%.7z"

endlocal
