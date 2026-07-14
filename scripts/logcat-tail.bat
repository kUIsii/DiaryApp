@echo off
chcp 65001 >nul
setlocal

cd /d "%~dp0\.."

echo.
echo === DiaryApp Logcat (Ctrl+C 退出) ===
echo.

adb logcat -v time -T 50 DiaryApp:V WeatherAlertWorker:V WeatherWorker:V ReminderManager:V AndroidRuntime:E *:S

endlocal
