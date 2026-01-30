# Khaohom's Savings - Native Android App

A native Android app built with Kotlin and Jetpack Compose for tracking savings across multiple assets (Bitcoin, Gold, USD, etc.) with future value projections.

## Features

- 📱 **Native Android Experience**
  - Material Design 3 (Material You)
  - Smooth animations and transitions
  - Bottom navigation with 4 screens
  - Swipe gestures support
  
- 📊 **4 Main Screens**
  - Screen 1: Assets list + Portfolio summary
  - Screen 2: Portfolio value chart
  - Screen 3: Transaction history
  - Screen 4: Future value prognosis

- 💱 **Dual Currency Support**: Toggle between USD and THB
- 🔄 **Live Data**: Connects to your Railway backend API
- 📈 **Charts**: Interactive charts using MPAndroidChart
- 🎨 **Modern UI**: Jetpack Compose with Material 3

## Tech Stack

- **Language**: Kotlin 1.9.20
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (ViewModel + Repository)
- **Networking**: Retrofit + OkHttp
- **Charts**: MPAndroidChart
- **Navigation**: Accompanist Pager
- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)

## Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 8 or higher
- Android device or emulator running Android 8.0+

## Setup Instructions

### 1. Clone and Open Project

```bash
git clone https://github.com/YOUR-USERNAME/khaohom-android.git
cd khaohom-android
```

Open the project in Android Studio.

### 2. Configure Backend URL

**IMPORTANT**: Before building the app, you must update the backend URL.

Open `app/src/main/java/com/khaohom/savings/data/remote/ApiConfig.kt`:

```kotlin
object ApiConfig {
    // TODO: Replace with your actual Railway backend URL
    const val BASE_URL = "https://your-backend-xyz.railway.app/"
}
```

Replace `https://your-backend-xyz.railway.app/` with your actual Railway backend URL from the deployment guide.

### 3. Sync Gradle

Click "Sync Now" in the banner at the top of Android Studio, or:
- Go to File → Sync Project with Gradle Files

### 4. Build and Run

**Option A: Using Android Device**
1. Enable Developer Options on your phone
2. Enable USB Debugging
3. Connect phone via USB
4. Click the "Run" button (green triangle) in Android Studio
5. Select your device from the list

**Option B: Using Emulator**
1. Click Tools → Device Manager
2. Create a new virtual device (recommended: Pixel 6, API 34)
3. Click the "Run" button
4. Select the emulator

## Project Structure

```
app/src/main/java/com/khaohom/savings/
├── data/
│   ├── model/
│   │   └── Models.kt              # Data models
│   ├── remote/
│   │   ├── ApiConfig.kt           # API configuration
│   │   ├── ApiService.kt          # Retrofit interfaces
│   │   └── RetrofitClient.kt      # Retrofit client
│   └── repository/
│       └── SavingsRepository.kt   # Data repository
├── ui/
│   ├── screens/
│   │   ├── AssetsScreen.kt        # Screen 1: Assets + Summary
│   │   ├── ChartScreen.kt         # Screen 2: Portfolio Chart
│   │   ├── HistoryScreen.kt       # Screen 3: Transaction History
│   │   └── PrognosisScreen.kt     # Screen 4: Future Prognosis
│   ├── theme/
│   │   ├── Theme.kt               # Material 3 theme
│   │   └── Type.kt                # Typography
│   └── viewmodel/
│       └── SavingsViewModel.kt    # Main ViewModel
└── MainActivity.kt                # Main Activity

app/src/main/res/
├── values/
│   ├── strings.xml
│   └── themes.xml
└── xml/
    ├── backup_rules.xml
    └── data_extraction_rules.xml
```

## Key Components

### ApiConfig.kt
Contains API configuration:
- Backend URL (update this!)
- CoinGecko API URL
- Exchange Rate API URL
- Timeout settings

### Models.kt
Data models:
- `Asset` - Asset information with CAGR
- `Transaction` - Transaction data
- `AssetHolding` - Aggregated holdings
- `PortfolioSummary` - Summary calculations
- `AssetProjection` - Future projections
- `ChartDataPoint` - Chart data

### SavingsRepository.kt
Data layer that handles:
- API calls to backend
- Current price fetching
- Portfolio calculations
- APY calculations
- Chart data generation
- Future projections

### SavingsViewModel.kt
Manages app state:
- UI state (Loading, Success, Error)
- Currency toggle (USD/THB)
- Data refresh
- Formatting utilities

### Screens
Four main screens:
1. **AssetsScreen** - Shows asset holdings and summary
2. **ChartScreen** - Interactive line chart
3. **HistoryScreen** - Transaction list with details
4. **PrognosisScreen** - Future value projections

## API Integration

The app connects to three APIs:

### 1. Your Backend API (Railway)
```
GET /api/assets          - Get all assets
GET /api/payments        - Get all transactions
GET /api/current-exchange-rate - Get USD/THB rate
GET /health              - Health check
```

### 2. CoinGecko API
```
GET /simple/price?ids=bitcoin,tether-gold&vs_currencies=usd
```

### 3. ExchangeRate-API
```
GET /v4/latest/USD
```

## Build Variants

**Debug Build**:
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

**Release Build**:
```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release-unsigned.apk`

## Signing the APK

### For Testing (Debug)
Debug builds are automatically signed with a debug keystore.

### For Release

1. Create keystore:
```bash
keytool -genkey -v -keystore khaohom-release.keystore -alias khaohom -keyalg RSA -keysize 2048 -validity 10000
```

2. Create `keystore.properties` in project root:
```properties
storePassword=YOUR_STORE_PASSWORD
keyPassword=YOUR_KEY_PASSWORD
keyAlias=khaohom
storeFile=../khaohom-release.keystore
```

3. Update `app/build.gradle.kts`:
```kotlin
signingConfigs {
    create("release") {
        val keystorePropertiesFile = rootProject.file("keystore.properties")
        val keystoreProperties = Properties()
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
        
        storeFile = file(keystoreProperties["storeFile"] as String)
        storePassword = keystoreProperties["storePassword"] as String
        keyAlias = keystoreProperties["keyAlias"] as String
        keyPassword = keystoreProperties["keyPassword"] as String
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        // ...
    }
}
```

4. Build:
```bash
./gradlew assembleRelease
```

## Troubleshooting

### "Failed to connect to backend"
- Check that `ApiConfig.BASE_URL` is correct
- Verify your backend is running on Railway
- Check internet connection on device/emulator
- Check Logcat for detailed error messages

### "No data showing"
- Make sure you've synced data from Google Sheets
- Check backend API is returning data (visit `/api/payments` in browser)
- Pull to refresh in the app

### Build errors
- Make sure Android Studio is up to date
- Sync Gradle files: File → Sync Project with Gradle Files
- Clean and rebuild: Build → Clean Project, then Build → Rebuild Project
- Invalidate caches: File → Invalidate Caches / Restart

### Chart not displaying
- Check Logcat for MPAndroidChart errors
- Ensure there's transaction data available
- Navigate between screens to trigger chart render

### Gradle sync failed
- Check internet connection (downloads dependencies)
- Try: File → Invalidate Caches / Restart
- Check `build.gradle.kts` for syntax errors

## Performance Tips

- App is optimized for Android 8.0+
- Uses Material You dynamic colors on Android 12+
- Smooth 60fps animations
- Efficient data loading with coroutines
- Local state caching

## Testing

### Manual Testing Checklist
- [ ] App launches successfully
- [ ] Data loads from backend
- [ ] Currency toggle works (USD ⇄ THB)
- [ ] All 4 screens navigate correctly
- [ ] Charts render properly
- [ ] Transaction details show on tap
- [ ] Refresh button updates data
- [ ] Projection years input works
- [ ] App handles network errors gracefully

### Test on Different Scenarios
- [ ] No internet connection
- [ ] Backend server down
- [ ] Empty data set
- [ ] Large data set (100+ transactions)
- [ ] Different screen sizes
- [ ] Light and dark themes

## Deployment to Google Play Store

1. Build signed release APK (see above)
2. Create Google Play Console account
3. Create new app listing
4. Upload APK
5. Complete store listing:
   - Title: "Khaohom Savings"
   - Short description: "Personal savings tracker"
   - Full description: (see below)
   - Screenshots: Required (create from app)
   - Feature graphic: 1024x500px
   - App icon: 512x512px

6. Set pricing (Free)
7. Complete content rating questionnaire
8. Submit for review

**Suggested Full Description**:
```
Track your savings across multiple assets with beautiful charts and future projections.

Features:
• Track Bitcoin, Gold, USD, and more
• View portfolio value over time
• See transaction history
• Project future values with CAGR
• Toggle between USD and THB
• Beautiful Material Design 3 interface

Your data stays private - connects only to your personal backend.
```

## Privacy & Security

- App requires internet permission only
- No data is stored locally (fetches from your backend)
- All communication over HTTPS
- No analytics or tracking
- No third-party SDKs except for charts

## Updates

To update the app:

1. Make code changes
2. Increment version in `app/build.gradle.kts`:
   ```kotlin
   versionCode = 2  // Increment
   versionName = "1.1"  // Update
   ```
3. Build new release APK
4. Upload to Play Store as update

## License

MIT License - Free to use for personal projects

## Support

For issues:
1. Check Logcat in Android Studio
2. Verify backend is accessible
3. Check this README
4. Open GitHub issue

## Acknowledgments

- Built with ❤️ for Khaohom
- Uses Jetpack Compose
- Charts by MPAndroidChart
- CoinGecko for price data

---

**Remember**: Update `ApiConfig.BASE_URL` before building!