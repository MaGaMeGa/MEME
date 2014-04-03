@echo off
setlocal
cd /D "%~dp0"

set _7z="%ProgramFiles%\7-Zip\7z.exe"
set _7zsfx="..\..\dev\7zSD.sfx"

set TARGET=MASS_act_file_gen

%_7z% a -mx=9 -mmt=off %TARGET% ai\aitia\FileGen.class ai\aitia\CmdLineFileGen.class
copy /b %_7zsfx% + 7zsfx.script + %TARGET%.7z %TARGET%.exe
del %TARGET%.7z

endlocal
