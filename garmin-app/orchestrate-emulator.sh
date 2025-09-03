#!/bin/bash

# Garmin Connect IQ Emulator Orchestration Script
# This script automates the entire setup and launch process

set -e

echo "🎮 Orchestrating Garmin Connect IQ Emulator Setup..."
echo "=================================================="

# Color codes for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Determine SDK path
SDK_PATH="$HOME/Library/Application Support/Garmin/ConnectIQ"
SDK_BIN="$SDK_PATH/Sdks/connectiq-sdk-mac-4.2.4/bin"
SIMULATOR_PATH="/Applications/Garmin Express.app"

# Alternative SDK path (user home)
ALT_SDK_PATH="$HOME/connectiq"
ALT_SDK_BIN="$ALT_SDK_PATH/bin"

echo -e "${YELLOW}Step 1: Checking for Connect IQ SDK...${NC}"

# Check for SDK in standard locations
if [ -d "$SDK_BIN" ]; then
    echo -e "${GREEN}✅ Found SDK at: $SDK_BIN${NC}"
    export PATH="$SDK_BIN:$PATH"
    SDK_FOUND=true
elif [ -d "$ALT_SDK_BIN" ]; then
    echo -e "${GREEN}✅ Found SDK at: $ALT_SDK_BIN${NC}"
    export PATH="$ALT_SDK_BIN:$PATH"
    SDK_FOUND=true
else
    SDK_FOUND=false
fi

if [ "$SDK_FOUND" = false ]; then
    echo -e "${YELLOW}📥 SDK not found. Setting up minimal build environment...${NC}"
    
    # Create SDK directory structure
    mkdir -p "$ALT_SDK_PATH/bin"
    
    # Download SDK Manager CLI as alternative
    echo "Downloading Connect IQ SDK Manager CLI..."
    if command -v brew >/dev/null 2>&1; then
        brew tap lindell/connect-iq
        brew install connect-iq-sdk-manager-cli
        SDK_FOUND=true
    else
        echo -e "${RED}❌ Homebrew not found. Installing SDK manually...${NC}"
        
        # Create mock SDK for demonstration
        cat > "$ALT_SDK_PATH/bin/monkeyc" << 'EOF'
#!/bin/bash
echo "Mock Connect IQ compiler - for demonstration only"
echo "Building: $@"
echo "To use real compiler, download SDK from:"
echo "https://developer.garmin.com/connect-iq/sdk/"
exit 0
EOF
        chmod +x "$ALT_SDK_PATH/bin/monkeyc"
        
        cat > "$ALT_SDK_PATH/bin/connectiq" << 'EOF'
#!/bin/bash
echo "Mock Connect IQ simulator - for demonstration only"
echo "Download real SDK from: https://developer.garmin.com/connect-iq/sdk/"
open "https://developer.garmin.com/connect-iq/sdk/"
exit 0
EOF
        chmod +x "$ALT_SDK_PATH/bin/connectiq"
        
        export PATH="$ALT_SDK_PATH/bin:$PATH"
        SDK_FOUND=partial
    fi
fi

# Step 2: Create developer key
echo -e "${YELLOW}Step 2: Setting up developer key...${NC}"
DEV_KEY="$HOME/connectiq_developer_key.pem"

if [ ! -f "$DEV_KEY" ]; then
    echo "Creating developer key..."
    openssl genrsa -out "$DEV_KEY" 2048 2>/dev/null
    openssl pkcs8 -topk8 -inform PEM -outform DER -in "$DEV_KEY" -out "${DEV_KEY%.pem}.der" -nocrypt 2>/dev/null
    echo -e "${GREEN}✅ Developer key created${NC}"
else
    echo -e "${GREEN}✅ Developer key already exists${NC}"
fi

# Step 3: Build the app
echo -e "${YELLOW}Step 3: Building the Garmin app...${NC}"

# Supported devices for testing
DEVICES=("fenix7" "fr965" "venu3")
BUILD_SUCCESS=false

for device in "${DEVICES[@]}"; do
    echo "Building for $device..."
    
    if [ "$SDK_FOUND" = true ]; then
        if monkeyc -d "$device" -f monkey.jungle -o "fidan-$device.prg" -y "$DEV_KEY" 2>/dev/null; then
            echo -e "${GREEN}✅ Successfully built for $device${NC}"
            BUILD_SUCCESS=true
            BUILT_FILE="fidan-$device.prg"
            break
        else
            echo -e "${YELLOW}⚠️  Build failed for $device, trying next...${NC}"
        fi
    else
        # Create mock PRG file for demonstration
        echo "Mock PRG file for $device" > "fidan-$device.prg"
        echo -e "${YELLOW}⚠️  Created mock build file (SDK required for real build)${NC}"
        BUILT_FILE="fidan-$device.prg"
        break
    fi
done

# Step 4: Launch simulator
echo -e "${YELLOW}Step 4: Launching Connect IQ Simulator...${NC}"

if [ "$SDK_FOUND" = true ]; then
    # Try to launch the simulator
    if command -v connectiq >/dev/null 2>&1; then
        echo -e "${GREEN}✅ Launching simulator...${NC}"
        echo ""
        echo "📱 Simulator Instructions:"
        echo "1. Select device: Fenix 7 or FR965"
        echo "2. Go to Settings > Apps > Run"
        echo "3. Load file: $BUILT_FILE"
        echo "4. Press START to begin focus session"
        echo ""
        
        # Launch simulator in background
        connectiq &
        SIMULATOR_PID=$!
        
        echo -e "${GREEN}✅ Simulator launched (PID: $SIMULATOR_PID)${NC}"
        
        # Wait a moment for simulator to start
        sleep 3
        
        # Auto-load the app if possible
        if [ -f "$BUILT_FILE" ]; then
            echo "Attempting to auto-load app..."
            # Note: Auto-loading varies by SDK version
        fi
        
    else
        echo -e "${YELLOW}⚠️  Simulator not found. Opening SDK download page...${NC}"
        open "https://developer.garmin.com/connect-iq/sdk/"
    fi
elif [ "$SDK_FOUND" = partial ]; then
    echo -e "${YELLOW}📥 Opening Connect IQ SDK download page...${NC}"
    echo "Please download and install the SDK, then run this script again."
    open "https://developer.garmin.com/connect-iq/sdk/"
fi

# Step 5: Summary
echo ""
echo "=================================================="
echo -e "${GREEN}🎉 Orchestration Complete!${NC}"
echo "=================================================="

if [ "$SDK_FOUND" = true ] && [ "$BUILD_SUCCESS" = true ]; then
    echo -e "${GREEN}✅ SDK installed${NC}"
    echo -e "${GREEN}✅ Developer key created${NC}"
    echo -e "${GREEN}✅ App built successfully${NC}"
    echo -e "${GREEN}✅ Simulator launched${NC}"
    echo ""
    echo "Next steps in the simulator:"
    echo "1. Select a device (Fenix 7 recommended)"
    echo "2. Load the app file: $BUILT_FILE"
    echo "3. Press START button to begin focus session"
    echo "4. Watch the tree grow as the timer counts down!"
else
    echo -e "${YELLOW}⚠️  Partial setup complete${NC}"
    echo ""
    echo "To complete setup:"
    echo "1. Download Connect IQ SDK from the opened webpage"
    echo "2. Install to ~/connectiq/"
    echo "3. Run this script again: ./orchestrate-emulator.sh"
fi

echo ""
echo "📚 Documentation: https://developer.garmin.com/connect-iq/"
echo "🐛 Debugging: Use System.println() in code and check simulator console"