#!/bin/bash
# Install Fidan app to Garmin 965 via MTP
# Usage: ./scripts/install-to-garmin.sh [prg_file]

set -e

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

# Default PRG file location
PRG_FILE=${1:-"build-artifacts/fidan-965-simple.prg"}
APP_NAME="fidan-965-simple.prg"

# Make path absolute if relative
if [[ "$PRG_FILE" != /* ]]; then
    PRG_FILE="$(pwd)/$PRG_FILE"
fi

echo "🏃‍♂️ Garmin 965 App Installer"
echo "=========================="
echo

# Check if file exists
if [[ ! -f "$PRG_FILE" ]]; then
    print_error "PRG file not found: $PRG_FILE"
    echo "Available files:"
    ls -la build-artifacts/*.prg 2>/dev/null || echo "No PRG files found in build-artifacts/"
    exit 1
fi

print_step "Checking for sudo access..."
# Test sudo access early
if ! sudo -n true 2>/dev/null; then
    print_warning "This script requires sudo access for MTP operations"
    print_warning "Please enter your password when prompted:"
    sudo -v || { print_error "Failed to get sudo access"; exit 1; }
fi

print_step "Checking Garmin device connection..."

# Check if device is connected (need sudo for MTP access on macOS)
if ! sudo mtp-detect 2>/dev/null | grep -q "Garmin Forerunner 965"; then
    print_error "Garmin Forerunner 965 not detected via MTP"
    print_warning "Make sure your watch is:"
    print_warning "1. Connected via USB cable"
    print_warning "2. In MTP mode (not Garmin mode)"
    print_warning "3. Screen is unlocked"
    echo
    print_warning "Troubleshooting tips:"
    echo "- Disconnect and reconnect the USB cable"
    echo "- On the watch: Settings > System > USB Mode > MTP"
    echo "- Try running: sudo mtp-detect"
    exit 1
fi

print_success "Garmin 965 detected via MTP"

print_step "Installing app to Garmin 965..."

# Copy file to device (Note: Apps directory is case-sensitive)
if sudo mtp-sendfile "$PRG_FILE" "/GARMIN/Apps/$APP_NAME"; then
    print_success "App installed successfully!"
    echo
    print_step "Next steps:"
    echo "1. Safely eject your Garmin 965 from computer"
    echo "2. Restart your watch (hold power button)"
    echo "3. Navigate to Apps menu on your watch"
    echo "4. Look for 'Fidan' in your apps list"
    echo
    print_success "Installation complete! 🎉"
else
    print_error "Failed to copy app to device"
    print_warning "Try these troubleshooting steps:"
    echo "1. Disconnect and reconnect USB cable"
    echo "2. Make sure watch screen is unlocked"
    echo "3. Check that watch is in MTP mode (not Garmin mode)"
    echo "4. Try running: sudo mtp-folders (to test connection)"
    exit 1
fi