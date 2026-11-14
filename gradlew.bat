@rem Gradle wrapper script
@if "%DEBUG%"=="" @echo off
set APP_HOME=%~dp0
set GRADLE_USER_HOME=%GRADLE_USER_HOME:~1,-1%
if "%GRADLE_USER_HOME%"=="" set GRADLE_USER_HOME=%USERPROFILE%\.gradle
set WRAPPER_DIR=%GRADLE_USER_HOME%\wrapper\dists
set WRAPPER_ZIP=%WRAPPER_DIR%\gradle-8.2-bin.zip

for /d %%d in ("%WRAPPER_DIR%\*") do (
    if exist "%%d\gradle-wrapper.jar" set WRAPPER_JAR=%%d\gradle-wrapper.jar
)

if "%WRAPPER_JAR%"=="" (
    echo Downloading Gradle 8.2...
    mkdir "%WRAPPER_DIR%" 2>nul
    powershell -Command "(new-object System.Net.WebClient).DownloadFile('https://services.gradle.org/distributions/gradle-8.2-bin.zip', '%WRAPPER_ZIP%')"
    set EXTRACT_DIR=%WRAPPER_DIR%\gradle-8.2
    mkdir "%EXTRACT_DIR%" 2>nul
    powershell -Command "Expand-Archive -Path '%WRAPPER_ZIP%' -DestinationPath '%EXTRACT_DIR%' -Force"
    for /d %%d in ("%EXTRACT_DIR%\*") do (
        if exist "%%d\gradle-wrapper.jar" set WRAPPER_JAR=%%d\gradle-wrapper.jar
    )
    del "%WRAPPER_ZIP%"
)

java -jar "%WRAPPER_JAR%" %*
