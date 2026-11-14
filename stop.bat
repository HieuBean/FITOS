@echo off
REM ===========================================================================
REM  FITOS - Dung he thong
REM ===========================================================================
setlocal
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop.ps1" %*
endlocal
pause
