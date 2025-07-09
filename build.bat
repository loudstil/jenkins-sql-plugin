@echo off
echo Building SQL Plugin for Jenkins...

echo.
echo Cleaning previous builds...
mvn clean

echo.
echo Compiling source code...
mvn compile

if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo.
echo Running tests...
mvn test

if %ERRORLEVEL% NEQ 0 (
    echo Tests failed!
    pause
    exit /b 1
)

echo.
echo Packaging plugin...
mvn package -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo Packaging failed!
    pause
    exit /b 1
)

echo.
echo Build completed successfully!
echo Plugin file: target\sql-plugin.hpi
echo.
echo To install:
echo 1. Go to Jenkins -> Manage Jenkins -> Plugin Manager
echo 2. Click "Advanced" tab
echo 3. Upload the .hpi file from the target directory
echo.
pause
