@echo off
chcp 65001 >nul
setlocal

cd /d "%~dp0\.."

set APK_DIR=app\build\outputs\apk\experimental\debug
set APK_PATH=%APK_DIR%\app-experimental-debug.apk
set PKG=com.diary.app.experimental
set ACT=com.diary.app.MainActivity

echo.
echo ====================================
echo   DiaryApp Experimental Debug Build
echo ====================================
echo.

echo [1/3] 增量编译 Experimental Debug...
call gradlew.bat :app:assembleExperimentalDebug -q
if %errorlevel% neq 0 (
  echo.
  echo [X] BUILD FAILED
  pause
  exit /b 1
)

if not exist "%APK_PATH%" (
  echo.
  echo [X] APK not found: %APK_PATH%
  pause
  exit /b 1
)

echo [2/3] 检查设备连接...
adb devices | findstr /R "device$" >nul
if %errorlevel% neq 0 (
  echo.
  echo [X] 未检测到设备，请确认 USB 已连接 + USB 调试已开
  pause
  exit /b 1
)

echo [3/3] 安装并启动 App...
adb install -r "%APK_PATH%"
if %errorlevel% neq 0 (
  echo.
  echo [X] INSTALL FAILED
  pause
  exit /b 1
)

adb shell am start -n %PKG%/%ACT% >nul

echo.
echo ====================================
echo   OK - App 已启动
echo ====================================
echo.
endlocal
