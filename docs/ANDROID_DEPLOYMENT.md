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
3. Installs the APK to the connected device
4. Launches the app

## Build Output

APK files are saved to `build-artifacts/`:
- `fidan-debug.apk` - Debug build
- `fidan-release.apk` - Release build (unsigned)

## Troubleshooting

### No device detected
- Check USB connection
- Ensure USB debugging is enabled: Settings > Developer Options > USB Debugging
- Authorize the computer when prompted on device

### Build fails
- Run `./gradlew clean` in android-app directory
- Check for build errors: `./gradlew assembleDebug --stacktrace`

### Installation fails
- Uninstall existing app: `adb uninstall com.erdalgunes.fidan.debug`
- Check available space on device