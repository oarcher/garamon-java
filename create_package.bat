@echo off
setlocal

:: Find the script's directory
set "SCRIPT_DIR=%~dp0"
set "JAR_PATH=%SCRIPT_DIR%build\libs\garamon-java.jar"

:: Build the project if the JAR doesn't exist
if not exist "%JAR_PATH%" (
    echo "Building create-package tool..."
    call "%SCRIPT_DIR%gradlew.bat" build
)

:: Run the Java application
java --enable-native-access=ALL-UNNAMED -jar "%JAR_PATH%" %*
