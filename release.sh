#!/bin/bash

set -e

echo "SQL Plugin Release Script"
echo "========================"

if [ $# -eq 0 ]; then
    echo "Usage: ./release.sh [version]"
    echo "Example: ./release.sh 1.0.0"
    exit 1
fi

VERSION=$1
echo "Preparing release for version: $VERSION"

echo
echo "Step 1: Updating version in pom.xml..."
# Update version in pom.xml using sed
sed -i.bak "s/<version>\${changelist}<\/version>/<version>$VERSION<\/version>/" pom.xml
echo "Version updated to $VERSION"

echo
echo "Step 2: Building the plugin..."
mvn clean package -DskipTests

echo
echo "Step 3: Running tests..."
mvn test

echo
echo "Step 4: Creating Git tag..."
git add .
git commit -m "Release v$VERSION"
git tag -a "v$VERSION" -m "Release v$VERSION"

echo
echo "Step 5: Pushing to GitHub..."
git push origin main
git push origin "v$VERSION"

echo
echo "Release v$VERSION completed!"
echo
echo "GitHub Actions will now:"
echo "1. Build the plugin"
echo "2. Run tests" 
echo "3. Create a GitHub release"
echo "4. Attach the .hpi file to the release"
echo
echo "Check the Actions tab on GitHub to monitor the release process."
echo "Release will be available at: https://github.com/YOUR_USERNAME/sql-plugin/releases"
