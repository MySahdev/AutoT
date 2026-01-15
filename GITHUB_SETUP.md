# GitHub Repository Setup - Step by Step

## Step 1: Create GitHub Account (if needed)

1. Go to: **https://github.com/signup**
2. Enter your email
3. Create password
4. Choose username
5. Verify email

---

## Step 2: Create New Repository

### Via Web Browser:

1. **Go to GitHub**: https://github.com
2. **Sign in** with your account
3. **Click the "+" icon** (top right corner)
4. **Select "New repository"**

5. **Fill in details**:
   ```
   Repository name: auto-translator
   Description: Android app for automatic screen text translation

   ⚪ Public (required for free GitHub Actions)
   ⚫ Private (if you prefer, but Actions have limits)

   ☐ Add a README file (leave unchecked - we have one)
   ☐ Add .gitignore (leave unchecked - we have one)
   ☐ Choose a license (optional)
   ```

6. **Click "Create repository"**

---

## Step 3: Upload Code to GitHub

You'll see a page with instructions. Choose one method:

### Method A: Using Terminal (Recommended)

Copy and paste these commands **one by one**:

```bash
# Navigate to project
cd /Users/Sahdev/autoT/android-translator

# Initialize git (if not already done)
git init

# Add all files
git add .

# Commit files
git commit -m "Initial commit - Auto Translator Android app"

# Add remote repository (REPLACE 'YOUR_USERNAME' with your GitHub username!)
git remote add origin https://github.com/YOUR_USERNAME/auto-translator.git

# Rename branch to main
git branch -M main

# Push to GitHub
git push -u origin main
```

**Important**: Replace `YOUR_USERNAME` with your actual GitHub username!

Example:
```bash
# If your username is "john_doe"
git remote add origin https://github.com/john_doe/auto-translator.git
```

### Method B: Using GitHub Desktop (GUI)

1. Download **GitHub Desktop**: https://desktop.github.com
2. Install and sign in
3. Click **"Add"** → **"Add Existing Repository"**
4. Browse to: `/Users/Sahdev/autoT/android-translator`
5. Click **"Publish repository"**
6. Check **"Keep this code private"** or leave unchecked for public
7. Click **"Publish repository"**

### Method C: Upload via Web (Easiest but slower)

1. On the repository page, click **"uploading an existing file"**
2. Open Finder → navigate to `/Users/Sahdev/autoT/android-translator`
3. Select **ALL files and folders**
4. **Drag and drop** into the GitHub upload area
5. Add commit message: "Initial commit"
6. Click **"Commit changes"**

---

## Step 4: Watch the Build

1. Go to your repository: `https://github.com/YOUR_USERNAME/auto-translator`
2. Click the **"Actions"** tab
3. You'll see **"Build Android APK"** workflow running ⚙️
4. Wait **3-5 minutes** for it to complete
5. Build status will turn **green ✓**

---

## Step 5: Download Your APK

1. Click on the **completed workflow** (green checkmark)
2. Scroll down to **"Artifacts"** section
3. Click **"auto-translator-debug"** to download
4. Unzip the downloaded file
5. You have your **app-debug.apk**! 🎉

---

## Step 6: Install on Your Phone

### Transfer APK to Phone:

**Option 1: USB Cable**
```bash
# Connect phone via USB
# Enable USB debugging on phone
adb install app-debug.apk
```

**Option 2: Cloud Storage**
- Upload APK to Google Drive / Dropbox
- Download on phone
- Open and install

**Option 3: Email**
- Email APK to yourself
- Open on phone
- Download and install

### Install:
1. Open the APK file on your phone
2. If prompted, enable **"Install from Unknown Sources"**
3. Tap **"Install"**
4. Tap **"Open"** when done!

---

## Troubleshooting

### "Permission denied" when pushing
```bash
# You may need to authenticate
# GitHub will prompt for username/password or token
```

### "Git command not found"
```bash
# Install git
brew install git
```

### Build failed on GitHub Actions
- Check the "Actions" tab for error logs
- Common fix: Make sure all files were uploaded

### Can't find "Actions" tab
- Make sure repository is **public**
- Free Actions only work on public repos
- Or upgrade to GitHub Pro for private repos

---

## Quick Command Reference

```bash
# Navigate to project
cd /Users/Sahdev/autoT/android-translator

# Check status
git status

# Add files
git add .

# Commit
git commit -m "Your message here"

# Push to GitHub
git push

# View remote URL
git remote -v
```

---

## Need Your GitHub Username?

After creating your account, your username will be in the URL:
```
https://github.com/YOUR_USERNAME
                    ^^^^^^^^^^^^
                    This is your username
```

---

## Next Steps After Upload

✅ Code is on GitHub
✅ APK builds automatically
✅ Download from Artifacts
✅ Install on phone
✅ Start translating!

Every time you push new code, GitHub will automatically rebuild the APK!
