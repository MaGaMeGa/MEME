@echo off
setlocal

cd /D %~dp0
set REPAST=%ProgramFiles%\Repast 3\Repast J

java -cp ".;%REPAST%\repast.jar" uchicago.src.sim.engine.SimInit demo.prisoners.PrisonersModelGUI

endlocal
