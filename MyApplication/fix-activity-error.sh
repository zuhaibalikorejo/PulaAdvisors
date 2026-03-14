#!/bin/bash

# Quick Fix Script for "Activity class does not exist" Error
# Run this script to fix the deployment issue

set -e  # Exit on error

echo "=================================="
echo "SurveySync - Activity Fix Script"
echo "=================================="
echo ""

# Check if we're in the right directory
if [ ! -f "settings.gradle.kts" ]; then
    echo "❌ ERROR: Please run this script from the project root directory"
    exit 1
fi

echo "Step 1: Stopping all Gradle daemons..."
./gradlew --stop
echo "✅ Done"
echo ""

echo "Step 2: Cleaning project..."
./gradlew clean
echo "✅ Done"
echo ""

echo "Step 3: Checking for connected devices..."
DEVICES=$(adb devices | grep -v "List" | grep "device" | wc -l)
if [ "$DEVICES" -eq 0 ]; then
    echo "⚠️  WARNING: No Android devices found"
    echo "   Please connect a device or start an emulator"
else
    echo "✅ Found $DEVICES device(s)"
    echo ""

    echo "Step 5: Uninstalling current app (com.pula.surveysync) if exists..."
    adb uninstall com.pula.surveysync 2>/dev/null && echo "✅ Current app uninstalled" || echo "ℹ️  Current app not found (OK)"
    echo ""
fi

echo "Step 6: Building debug APK..."
./gradlew assembleDebug
echo "✅ Done"
echo ""

if [ "$DEVICES" -gt 0 ]; then
    echo "Step 7: Installing new version..."
    ./gradlew installDebug
    echo "✅ Done"
    echo ""

    echo "Step 8: Verifying installation..."
    INSTALLED=$(adb shell pm list packages | grep "com.pula.surveysync" | wc -l)
    if [ "$INSTALLED" -gt 0 ]; then
        echo "✅ SUCCESS! App is installed as: com.pula.surveysync"
        echo ""
        echo "Step 9: Launching app..."
        adb shell am start -n com.pula.surveysync/.MainActivity
        echo ""
        echo "=================================="
        echo "✅ ALL DONE! App should be running"
        echo "=================================="
    else
        echo "❌ ERROR: App installation verification failed"
        echo "   Try installing manually from Android Studio"
    fi
else
    echo "=================================="
    echo "⚠️  Build successful but no device connected"
    echo "   Please connect a device and run:"
    echo "   ./gradlew installDebug"
    echo "=================================="
fi

