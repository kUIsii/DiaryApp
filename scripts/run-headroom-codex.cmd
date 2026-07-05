@echo off
setlocal

echo Starting Codex through Headroom...
echo.
echo This will:
echo   1. start the Headroom proxy
echo   2. enable the Codex wrapper
echo   3. keep your current global Codex config untouched
echo.

headroom wrap codex

endlocal
