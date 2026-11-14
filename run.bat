@echo off
REM ===========================================================================
REM  FITOS - Chay he thong
REM  Nhap dup: run.bat            (build lai neu can)
REM            run.bat -SkipBuild (chay ngay, khong build)
REM ===========================================================================
setlocal
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run.ps1" %*
endlocal
pause
