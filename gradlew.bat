@echo off
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  gradle %*
  exit /b %ERRORLEVEL%
)
echo Gradle is not installed. Install Gradle 8.10.2 or use GitHub Actions to build this project.
exit /b 1
