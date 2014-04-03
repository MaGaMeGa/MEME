@echo off
REM Starts the HSQLDB server. 
REM The database location is specified below in the LOC variable.
REM The datbase will be available as "jdbc:hsqldb:hsql://localhost/"

setlocal
cd /D "%~dp0"
set LOC=db/meme
REM set LOC=../../temp/test

start "Server %LOC%" java -Xmx256m -cp 3rdParty/hsqldb/lib/hsqldb.jar org.hsqldb.Server -dbname.0 "" -database.0 "%LOC%" %*
endlocal
