# Android Deployment

Simple deployment system for the Fidan Android app.

## Prerequisites

- Android SDK installed with `adb` in PATH
- Android device connected via USB
- USB debugging enabled on device
- Device authorized for debugging

## Usage

```bash
# Deploy debug build (default)
./scripts/deploy-android.sh

# Deploy release build
./scripts/deploy-android.sh release
```

## What it does

1. Checks for connected Android device
2. Builds the Android app using Gradle
   - Shows warning for unsigned release builds
3. Checks if app is already installed
   - Prompts for confirmation before reinstalling
   - Preserves app data during reinstall
4. Installs the APK to the connected device
5. Launches the app with correct package variant

## Build Output

APK files are saved to `build-artifacts/`:
- `fidan-debug.apk` - Debug build (uses `com.erdalgunes.fidan.debug` package)
- `fidan-release.apk` - Release build (unsigned, uses `com.erdalgunes.fidan` package)

## Features

- **Smart Package Handling**: Automatically uses correct package name for debug/release variants
- **Safe Reinstallation**: Asks for confirmation before replacing existing app
- **Data Preservation**: Uses `-r` flag only when reinstalling to preserve user data
- **Build Warnings**: Alerts when building unsigned release APKs

## Troubleshooting

### No device detected
- Check USB connection
- Ensure USB debugging is enabled: Settings > Developer Options > USB Debugging
- Authorize the computer when prompted on device

### Build fails
- Run `./gradlew clean` in android-app directory
- Check for build errors: `./gradlew assembleDebug --stacktrace`

### Installation fails
- Uninstall existing app: 
  - Debug: `adb uninstall com.erdalgunes.fidan.debug`
  - Release: `adb uninstall com.erdalgunes.fidan`
- Check available space on device
- For release builds, ensure APK is properly signed