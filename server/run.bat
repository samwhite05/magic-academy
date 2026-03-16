@echo off
set "JAVA=c:\Users\Sam\Documents\antigravity saves\mc\magic-academy\.tools\jdk21\jdk-21.0.10+7\bin\java.exe"
if not exist "%JAVA%" (
  echo JDK not found at %JAVA%
  pause
  exit /b 1
)
"%JAVA%" -Xms4G -Xmx6G -jar paper-1.21.4-232.jar nogui
pause
