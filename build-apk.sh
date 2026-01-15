#!/bin/bash

echo "======================================"
echo "Auto Translator - APK Build Script"
echo "======================================"
echo ""

# Check if Android SDK is installed
if [ -z "$ANDROID_HOME" ]; then
    echo "❌ Error: ANDROID_HOME not set"
    echo ""
    echo "Please install Android Studio or Android SDK Command-line Tools"
    echo "Download from: https://developer.android.com/studio"
    echo ""
    exit 1
fi

echo "✅ Android SDK found at: $ANDROID_HOME"
echo ""

# Check if gradlew exists
if [ ! -f "./gradlew" ]; then
    echo "⚠️  gradlew not found. Creating gradle wrapper..."
    gradle wrapper
fi

# Make gradlew executable
chmod +x ./gradlew

echo "🔧 Building debug APK..."
echo ""

# Build the APK
./gradlew assembleDebug

# Check if build was successful
if [ $? -eq 0 ]; then
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

    if [ -f "$APK_PATH" ]; then
        echo ""
        echo "======================================"
        echo "✅ Build Successful!"
        echo "======================================"
        echo ""
        echo "APK Location: $APK_PATH"
        echo "APK Size: $(du -h $APK_PATH | cut -f1)"
        echo ""
        echo "To install on device:"
        echo "  adb install $APK_PATH"
        echo ""
        echo "Or copy the APK to your phone and install manually"
        echo ""
    else
        echo "❌ APK not found at expected location"
        exit 1
    fi
else
    echo ""
    echo "❌ Build failed. Check errors above."
    exit 1
fi
