#!/bin/bash
# Deploy Fidan Android app to connected device
# Usage: ./scripts/deploy-android.sh [debug|release]

set -e

# Configuration
BUILD_TYPE=${1:-debug}
BUILD_DIR="build-artifacts"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_step() {
    echo -e "${BLUE}==> $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

# Check prerequisites
check_prerequisites() {
    print_step "Checking prerequisites..."
    
    if ! command -v adb &> /dev/null; then
        print_error "ADB not found. Please install Android SDK."
        exit 1
    fi
    
    if ! adb devices | grep -q "device$"; then
        print_error "No Android device connected"
        print_warning "Please connect a device with USB debugging enabled"
        exit 1
    fi
    
    print_success "Prerequisites met"
}

# Build Android app
build_android() {
    print_step "Building Android app ($BUILD_TYPE)..."
    
    if [ ! -d "android-app" ]; then
        print_error "android-app directory not found"
        exit 1
    fi
    
    cd android-app
    
    # Clean and build
    ./gradlew clean
    
    if [ "$BUILD_TYPE" = "release" ]; then
        ./gradlew assembleRelease
        APK_PATH="app/build/outputs/apk/release/app-release-unsigned.apk"
    else
        ./gradlew assembleDebug
        APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    fi
    
    if [ ! -f "$APK_PATH" ]; then
        print_error "Build failed: APK not found at $APK_PATH"
        exit 1
    fi
    
    # Create build directory and copy APK
    mkdir -p "../$BUILD_DIR"
    cp "$APK_PATH" "../$BUILD_DIR/fidan-$BUILD_TYPE.apk"
    
    cd ..
    print_success "APK built: $BUILD_DIR/fidan-$BUILD_TYPE.apk"
}

# Install to device
install_android() {
    print_step "Installing to device..."
    
    APK_FILE="$BUILD_DIR/fidan-$BUILD_TYPE.apk"
    
    if [ ! -f "$APK_FILE" ]; then
        print_error "APK not found: $APK_FILE"
        exit 1
    fi
    
    adb install -r "$APK_FILE"
    print_success "App installed successfully"
    
    # Try to launch the app
    print_step "Launching app..."
    adb shell am start -n com.erdalgunes.fidan.debug/com.erdalgunes.fidan.MainActivity || true
}

# Main execution
main() {
    echo "📱 Fidan Android Deployment"
    echo "=========================="
    echo
    
    check_prerequisites
    build_android
    install_android
    
    echo
    print_success "Deployment complete!"
}

main