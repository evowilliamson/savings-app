# Quick Setup Guide - Khaohom Android App

Get your native Android app running in 15 minutes!

## What You Need

- [ ] Android Studio installed
- [ ] Your Railway backend URL
- [ ] Android device or emulator
- [ ] 15 minutes

## Step-by-Step Setup

### 1️⃣ Open Project in Android Studio (2 min)

1. **Launch Android Studio**
2. **Open project**:
   - File → Open
   - Navigate to `khaohom-android` folder
   - Click "OK"
3. **Wait for Gradle sync** to complete (progress bar at bottom)

### 2️⃣ Configure Backend URL (1 min)

**CRITICAL STEP** - App won't work without this!

1. **Open** `app/src/main/java/com/khaohom/savings/data/remote/ApiConfig.kt`

2. **Find line 10**:
   ```kotlin
   const val BASE_URL = "https://your-backend-xyz.railway.app/"
   ```

3. **Replace** with your actual Railway backend URL:
   ```kotlin
   const val BASE_URL = "https://khaohom-backend-abc123.railway.app/"
   ```
   
   ⚠️ **Important**: Keep the trailing slash `/`

4. **Save** the file (Ctrl+S / Cmd+S)

### 3️⃣ Build the App (2 min)

1. **Sync Gradle** (if not auto-synced):
   - Click "Sync Now" banner, OR
   - File → Sync Project with Gradle Files

2. **Wait** for sync to complete
   - Watch progress at bottom of Android Studio
   - First sync downloads dependencies (~30-60 seconds)

### 4️⃣ Run on Device or Emulator (10 min)

**Option A: Physical Android Device** (Recommended)

1. **Enable Developer Mode** on your phone:
   - Settings → About Phone
   - Tap "Build Number" 7 times
   - Enter PIN if prompted

2. **Enable USB Debugging**:
   - Settings → Developer Options
   - Turn on "USB Debugging"

3. **Connect phone** to computer via USB

4. **Allow USB Debugging**:
   - Tap "Allow" on phone when prompted

5. **Run app**:
   - Click green "Run" button (▶) in Android Studio
   - Select your device from list
   - Wait for app to install and launch

**Option B: Android Emulator**

1. **Create emulator**:
   - Click Tools → Device Manager
   - Click "Create Device"
   - Select "Phone" → "Pixel 6" → "Next"
   - Select "Tiramisu" (API 33) → "Next"
   - Click "Finish"

2. **Start emulator**:
   - Click ▶ next to your virtual device in Device Manager
   - Wait for emulator to boot (~30 seconds)

3. **Run app**:
   - Click green "Run" button in Android Studio
   - Select emulator from list
   - Wait for app to install and launch

### 5️⃣ Test the App (2 min)

Once app launches:

1. **Wait for data to load** (5-10 seconds)
   - You should see loading spinner
   - Then your assets and summary

2. **Test navigation**:
   - Tap "Chart" at bottom → See portfolio chart
   - Tap "History" → See transaction list
   - Tap "Future" → See projections

3. **Test currency toggle**:
   - Tap "USD" button at top
   - Should change to "THB"
   - Values update to Thai Baht

4. **Test refresh**:
   - Tap refresh icon (↻) at top right
   - Data reloads from backend

✅ **Success!** Your app is running!

## Common Issues & Fixes

### ❌ "Failed to connect to backend"

**Cause**: Backend URL not configured or backend down

**Fix**:
1. Check `ApiConfig.kt` has correct URL
2. Visit your backend URL in browser
3. Should see: `{"status":"ok","timestamp":"..."}`
4. Make sure URL ends with `/`

### ❌ "No data showing"

**Cause**: Backend has no data or connection issue

**Fix**:
1. Open Google Sheet
2. Click "Savings Tools" → "Sync Payments"
3. Wait for "Synced successfully" message
4. Tap refresh (↻) in app

### ❌ Gradle sync failed

**Cause**: Internet connection or Gradle cache issue

**Fix**:
1. Check internet connection
2. File → Invalidate Caches / Restart
3. Wait for Android Studio to restart
4. File → Sync Project with Gradle Files

### ❌ "Installation failed"

**Cause**: Incompatible device or storage full

**Fix**:
1. Check device has Android 8.0+
2. Free up storage space
3. Try: Build → Clean Project
4. Then: Build → Rebuild Project

### ❌ "Charts not showing"

**Cause**: Not enough data or navigation issue

**Fix**:
1. Make sure you have transactions in backend
2. Navigate away and back to Chart screen
3. Check Logcat for errors (bottom panel)

## Verify Installation

Your app should have:
- ✅ App icon on device
- ✅ "Khaohom Savings" as app name
- ✅ 4 bottom navigation items
- ✅ Data loading from backend
- ✅ USD/THB toggle working
- ✅ All 4 screens accessible

## Next Steps

### Create App Icon (Optional)

Replace default launcher icon:

1. **Right-click** `app` folder
2. **Select** New → Image Asset
3. **Choose** foreground image (your icon)
4. **Click** "Next" → "Finish"
5. **Rebuild** app

### Build APK for Sharing

To share app with others:

```bash
# Debug APK (for testing)
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

Send this APK to anyone to install!

### Enable ProGuard (Optional)

For smaller, faster release builds:

In `app/build.gradle.kts`:
```kotlin
buildTypes {
    release {
        isMinifyEnabled = true  // Change to true
        // ...
    }
}
```

## Development Tips

### View Logs

**Logcat** (bottom panel in Android Studio):
- Filter by "khaohom" to see app logs
- Look for errors in red
- Check network requests

### Debug Network

Add breakpoint in `SavingsRepository.kt`:
```kotlin
suspend fun getAssets(): Result<List<Asset>> = withContext(Dispatchers.IO) {
    try {
        val assets = backendApi.getAssets()  // ← Add breakpoint here
        Result.success(assets)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### Hot Reload

Compose supports hot reload:
- Make UI changes
- App updates automatically (most cases)
- No need to rebuild

### Test Different Currencies

Toggle between USD and THB:
- USD: American dollars
- THB: Thai Baht
- Rate fetched from backend

## Performance Monitoring

Check app performance:
1. **Run** → **Profile 'app'**
2. **Select** device
3. **Choose** CPU, Memory, or Network profiler
4. **Monitor** while using app

## What's Next?

- [ ] Add app to device home screen
- [ ] Test with real backend data
- [ ] Customize colors/theme (see Theme.kt)
- [ ] Add more features
- [ ] Publish to Play Store (see README.md)

## Quick Reference

**Backend URL Location**:
```
app/src/main/java/com/khaohom/savings/data/remote/ApiConfig.kt
Line 10
```

**Run App**:
```
Click ▶ (Run) button in toolbar
```

**View Logs**:
```
View → Tool Windows → Logcat
```

**Rebuild**:
```
Build → Rebuild Project
```

**Clean**:
```
Build → Clean Project
```

## Need Help?

1. Check Logcat for errors
2. Verify backend URL is correct
3. Test backend in browser
4. Check README.md for details
5. Open GitHub issue

---

**Pro Tip**: Bookmark this guide for future reference!

🎉 **Enjoy your native Android app!**