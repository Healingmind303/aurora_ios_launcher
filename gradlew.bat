@echo off
setlocal
set "GRADLE_VERSION=8.7"
set "BOOTSTRAP=%USERPROFILE%\.gradle\aurora-bootstrap"
set "GRADLE_HOME=%BOOTSTRAP%\gradle-%GRADLE_VERSION%"
if exist "%GRADLE_HOME%\bin\gradle.bat" goto run_gradle
if not exist "%BOOTSTRAP%" mkdir "%BOOTSTRAP%"
echo Downloading Gradle %GRADLE_VERSION% for the Aurora build...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $base=Join-Path $env:USERPROFILE '.gradle\aurora-bootstrap'; $zip=Join-Path $base 'gradle-%GRADLE_VERSION%-bin.zip'; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile $zip; Expand-Archive -Path $zip -DestinationPath $base -Force"
if errorlevel 1 exit /b 1
:run_gradle
call "%GRADLE_HOME%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
