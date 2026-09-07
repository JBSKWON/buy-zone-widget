# Buy Zone Widget

An Android home-screen widget for evaluating configurable buy-zone strategies across US stock and ETF tickers.

## Current slice

- Kotlin Android application targeting SDK 36 with a minimum SDK of 26.
- Wilder RSI, SMA/EMA deviation, moving-average spread, MACD histogram, and Bollinger %B calculation engine.
- Per-stage OR/AND strategy evaluation and orange → red → purple stage colors.
- Tiingo end-of-day client boundary using each user’s own API token.
- Encrypted local token storage.
- Configurable one-ticker-per-widget binding.
- App-owned ticker and strategy configuration; widgets remain display surfaces.
- Approximate 12-hour periodic WorkManager schedule.
- Ticker-scoped manual refresh work with duplicate-request coalescing.

## Build

The project uses the Gradle wrapper and requires JDK 17 for the Android Gradle Plugin.

```bash
export JAVA_HOME=/path/to/jdk-17
./gradlew testDebugUnitTest assembleDebug
```

The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

After installing, open the app to save your Tiingo token and add/configure tickers. Add a widget from the Android launcher; its setup flow opens the app so you can choose which configured ticker it displays.

## Data handling

The Tiingo token is stored using Android Keystore-backed encrypted preferences. Raw market bars are kept in memory for calculation; the planned persistence layer stores only derived snapshots and synchronization metadata.
