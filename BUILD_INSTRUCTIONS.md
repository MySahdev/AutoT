# How to Build the APK

You have **3 options** to get the APK:

---

## Option 1: Use Android Studio (Easiest) ⭐

### Steps:

1. **Install Android Studio**
   - Download from: https://developer.android.com/studio
   - Install and open it

2. **Open the Project**
   ```bash
   # Open Android Studio
   # Click "Open"
   # Navigate to: /Users/Sahdev/autoT/android-translator
   # Click "OK"
   ```

3. **Wait for Gradle Sync**
   - Android Studio will automatically download dependencies
   - Wait for "Gradle sync finished" message

4. **Build APK**
   - Click `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
   - Wait for build to complete (~2-5 minutes)
   - Click "locate" in the popup

5. **APK Location**
   ```
   android-translator/app/build/outputs/apk/debug/app-debug.apk
   ```

6. **Install on Phone**
   - Transfer APK to your phone
   - Open file and tap "Install"
   - Enable "Install from Unknown Sources" if prompted

---

## Option 2: Command Line (If you have Android SDK)

### Requirements:
- Android SDK installed
- `ANDROID_HOME` environment variable set

### Steps:

1. **Navigate to project**
   ```bash
   cd /Users/Sahdev/autoT/android-translator
   ```

2. **Run build script**
   ```bash
   chmod +x build-apk.sh
   ./build-apk.sh
   ```

3. **Or manually**
   ```bash
   chmod +x gradlew
   ./gradlew assembleDebug
   ```

4. **Install via ADB**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

---

## Option 3: Online Build Service (No installation needed)

If you don't want to install Android Studio:

### Using GitHub Actions (Free)

1. **Create GitHub repository**
   ```bash
   cd /Users/Sahdev/autoT/android-translator
   git init
   git add .
   git commit -m "Initial commit"
   ```

2. **Push to GitHub**
   - Create a new repo at github.com
   - Follow the instructions to push

3. **Add GitHub Actions workflow**
   - I can create a workflow file that auto-builds APK
   - APK will be available as an artifact download

### Using AppCenter or Firebase

1. **Sign up** at appcenter.ms (free)
2. **Connect repository**
3. **Configure build**
4. **Download APK** from build artifacts

---

## Quick Setup: Android Studio Installation

### macOS:
1. Download: https://developer.android.com/studio
2. Drag to Applications folder
3. Open Android Studio
4. Follow setup wizard
5. Install SDK (select defaults)

### Install Time: ~20 minutes
### Disk Space: ~5GB

---

## Troubleshooting

### "ANDROID_HOME not set"
```bash
# Add to ~/.zshrc or ~/.bash_profile
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

### "Gradle build failed"
```bash
# Clean and rebuild
./gradlew clean
./gradlew assembleDebug
```

### "SDK not found"
- Open Android Studio
- Go to Preferences → Appearance & Behavior → System Settings → Android SDK
- Install SDK Platform 34

---

## APK Details

**File**: `app-debug.apk`
**Size**: ~15-20 MB
**Min Android**: 7.0 (API 24)
**Target Android**: 14 (API 34)

---

## What I Recommend

**Best for you**: **Option 1 (Android Studio)**

Why?
✅ Most reliable
✅ Easy to use
✅ Can make changes later
✅ Official Google tool
✅ One-time 20-min setup

Just download Android Studio, open the project, and click Build → Build APK(s). Done!
