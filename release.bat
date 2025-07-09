@echo off
setlocal enabledelayedexpansion

echo SQL Plugin Release Script
echo ========================

REM Check if version parameter is provided
if "%1"=="" (
    echo Usage: release.bat [version]
    echo Example: release.bat 1.0.0
    exit /b 1
)

set VERSION=%1
echo Preparing release for version: %VERSION%

echo.
echo Step 1: Updating version in pom.xml...
REM Update version in pom.xml (you might want to use a more sophisticated method)
echo Please manually update the version in pom.xml to %VERSION%
pause

echo.
echo Step 2: Building the plugin...
call mvn clean package -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    exit /b 1
)

echo.
echo Step 3: Running tests...
call mvn test

if %ERRORLEVEL% NEQ 0 (
    echo Tests failed!
    exit /b 1
)

echo.
echo Step 4: Creating Git tag...
git add .
git commit -m "Release v%VERSION%"
git tag -a v%VERSION% -m "Release v%VERSION%"

echo.
echo Step 5: Pushing to GitHub...
git push origin main
git push origin v%VERSION%

echo.
echo Release v%VERSION% completed!
echo.
echo GitHub Actions will now:
echo 1. Build the plugin
echo 2. Run tests
echo 3. Create a GitHub release
echo 4. Attach the .hpi file to the release
echo.
echo Check the Actions tab on GitHub to monitor the release process.
echo Release will be available at: https://github.com/YOUR_USERNAME/sql-plugin/releases
echo.
pause
