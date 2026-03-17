@echo off
setlocal enabledelayedexpansion

echo ========================================
echo Building Magic Academy Plugins
echo ========================================
echo.

cd /d "%~dp0"

set JAVA_HOME=.tools\jdk21\jdk-21.0.10+7
set GRADLE=.tools\gradle-8.11.1\bin\gradle.bat

if not exist "%JAVA_HOME%" (
    echo ERROR: JDK 21 not found at %JAVA_HOME%
    echo Please ensure the .tools folder is set up correctly
    pause
    exit /b 1
)

if not exist "%GRADLE%" (
    echo ERROR: Gradle not found at %GRADLE%
    pause
    exit /b 1
)

echo Building plugins...
"%GRADLE%" -p . shadowJar

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Build failed!
    pause
    exit /b 1
)

echo.
echo ========================================
echo Deploying to dev server
echo ========================================
echo.

set PLUGINS_DIR=server\plugins
set BUILD_DIR=build\libs

if not exist "%PLUGINS_DIR%" (
    echo ERROR: Plugins directory not found: %PLUGINS_DIR%
    pause
    exit /b 1
)

echo Copying JARs to server/plugins...

copy /y "%BUILD_DIR%\api-1.0.0-SNAPSHOT.jar" "%PLUGINS_DIR%\api-1.0.0-SNAPSHOT.jar" >nul 2>&1
copy /y "%BUILD_DIR%\magic-core-1.0.0-SNAPSHOT-all.jar" "%PLUGINS_DIR%\magic-core-1.0.0-SNAPSHOT-all.jar" >nul 2>&1
copy /y "%BUILD_DIR%\magic-items-1.0.0-SNAPSHOT-all.jar" "%PLUGINS_DIR%\magic-items-1.0.0-SNAPSHOT-all.jar" >nul 2>&1
copy /y "%BUILD_DIR%\magic-npcs-1.0.0-SNAPSHOT-all.jar" "%PLUGINS_DIR%\magic-npcs-1.0.0-SNAPSHOT-all.jar" >nul 2>&1
copy /y "%BUILD_DIR%\magic-spells-1.0.0-SNAPSHOT-all.jar" "%PLUGINS_DIR%\magic-spells-1.0.0-SNAPSHOT-all.jar" >nul 2>&1
copy /y "%BUILD_DIR%\magic-dungeons-1.0.0-SNAPSHOT-all.jar" "%PLUGINS_DIR%\magic-dungeons-1.0.0-SNAPSHOT-all.jar" >nul 2>&1
copy /y "%BUILD_DIR%\magic-hideouts-1.0.0-SNAPSHOT-all.jar" "%PLUGINS_DIR%\magic-hideouts-1.0.0-SNAPSHOT-all.jar" >nul 2>&1
copy /y "%BUILD_DIR%\magic-academy-1.0.0-SNAPSHOT-all.jar" "%PLUGINS_DIR%\magic-academy-1.0.0-SNAPSHOT-all.jar" >nul 2>&1
copy /y "%BUILD_DIR%\magic-world-1.0.0-SNAPSHOT-all.jar" "%PLUGINS_DIR%\magic-world-1.0.0-SNAPSHOT-all.jar" >nul 2>&1

echo.
echo Plugins deployed:
dir /b "%PLUGINS_DIR%\magic-*.jar" 2>nul
dir /b "%PLUGINS_DIR%\api-*.jar" 2>nul

echo.
echo ========================================
echo Build and deploy complete!
echo ========================================
echo.
echo Run 'server\run.bat' to start the dev server
echo.

pause
