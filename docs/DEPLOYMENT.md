# Fidan Deployment System

This document describes the build artifact system for deploying Fidan apps to both Android phones and Garmin devices.

## Quick Start

```bash
# Deploy both apps with default settings (debug Android + fr965 Garmin)
make deploy

# Deploy release versions
make deploy-release

# Deploy only to Android
make deploy-android

# Deploy only to Garmin
make deploy-garmin
```

## Prerequisites

### Android
- Android SDK with ADB in PATH
- Android device connected via USB
- USB debugging enabled on device
- Device authorized for debugging

### Garmin 965
- Garmin Connect IQ SDK installed
- SDK binaries (`monkeyc`, `monkeydo`) in PATH
- Garmin device connected or simulator running

## Build Artifacts

The deployment system creates the following artifacts in `build-artifacts/`:

- `fidan-debug.apk` / `fidan-release.apk` - Android APK files
- `fidan-fr965.iq` - Garmin Connect IQ app for Forerunner 965
- `fidan-fr965.prg` - Garmin simulator executable

## Installation Methods

### Android
- **Automatic**: Connected devices are detected and app installed via ADB
- **Manual**: Install APK file from `build-artifacts/` using file manager

### Garmin 965
- **Connect IQ App**: Open app → Apps → Install from file → Select `.iq` file
- **Device Copy**: Copy `.iq` file to `/GARMIN/Apps/` folder on device
- **Simulator**: Automatically launched if available

## Deployment Commands

| Command | Description |
|---------|-------------|
| `make deploy` | Build & deploy both apps (debug + fr965) |
| `make deploy-debug` | Build & deploy debug versions |
| `make deploy-release` | Build & deploy release versions |
| `make deploy-android` | Deploy Android app only |
| `make deploy-garmin` | Deploy Garmin app only |

## Advanced Usage

### Custom Device Target
```bash
# Deploy to specific Garmin device
./scripts/deploy.sh debug fenix7
./scripts/deploy.sh release venu3
```

### Supported Garmin Devices
- `fr965` - Forerunner 965 (default)
- `fenix7` - Fenix 7 series
- `venu3` - Venu 3 series
- `vivoactive5` - Vivoactive 5

### Build Types
- `debug` - Development build with debugging enabled (default)
- `release` - Production build optimized and signed

## SDK Installation

### Garmin Connect IQ SDK
1. Download from [Garmin Developer Portal](https://developer.garmin.com/connect-iq/sdk/)
2. Extract to desired location (e.g., `~/connectiq-sdk`)
3. Add to PATH:
   ```bash
   export PATH="$PATH:~/connectiq-sdk/bin"
   ```

### Android SDK
1. Install via Android Studio or command line tools
2. Ensure `adb` is in PATH
3. Enable USB debugging on device

## Troubleshooting

### Android Issues
- **Device not detected**: Check USB debugging is enabled and device is authorized
- **Installation fails**: Try `adb kill-server && adb start-server`
- **Permission denied**: Ensure device is unlocked during installation

### Garmin Issues  
- **monkeyc not found**: Install Connect IQ SDK and add to PATH
- **Build fails**: Check `manifest.xml` and `monkey.jungle` syntax
- **Device not detected**: Use simulator or manually copy `.iq` file

### Common Solutions
```bash
# Reset Android connection
adb kill-server && adb start-server && adb devices

# Check Garmin SDK installation
which monkeyc
monkeyc --version

# Verify device connections
adb devices                    # Android devices
connectiq                      # Garmin simulator
```

## Development Workflow

1. **Code Changes**: Make changes to Android or Garmin app code
2. **Build & Test**: Run `make deploy` to build and install both apps
3. **Test Sync**: Verify session sync between Android app and Garmin watch
4. **Release**: Use `make deploy-release` for production builds

## CI/CD Integration

The deployment scripts can be integrated into CI/CD pipelines:

```yaml
# Example GitHub Actions step
- name: Deploy Fidan Apps
  run: |
    make deploy-release
    # Upload artifacts to releases
    gh release upload ${{ github.ref_name }} build-artifacts/*
```

## Security Notes

- Release builds require proper signing certificates
- Keep developer keys secure and out of version control
- Test thoroughly on target devices before production deployment