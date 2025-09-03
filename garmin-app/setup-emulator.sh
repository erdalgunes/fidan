#!/bin/bash

# Garmin Connect IQ Emulator Setup Script
set -e

echo "🚀 Setting up Garmin Connect IQ development environment..."

# Check if SDK is already installed
if [ -d "$HOME/connectiq" ] && [ -f "$HOME/connectiq/bin/monkeyc" ]; then
    echo "✅ Connect IQ SDK already installed"
    export PATH="$HOME/connectiq/bin:$PATH"
else
    echo "📥 Connect IQ SDK not found. Please download it manually:"
    echo "   1. Visit: https://developer.garmin.com/connect-iq/sdk/"
    echo "   2. Download Connect IQ SDK for macOS"
    echo "   3. Extract to ~/connectiq/"
    echo "   4. Run this script again"
    exit 1
fi

# Create monkey.jungle if it doesn't exist
if [ ! -f "monkey.jungle" ]; then
    echo "📝 Creating monkey.jungle build configuration..."
    cat > monkey.jungle << 'EOF'
project.manifest = manifest.xml

# Base configuration
base.sourcePath = source
base.resourcePath = resources
base.excludeAnnotations = test;debug

# Device configurations for popular models
fenix7.sourcePath = source
fenix7.resourcePath = resources

fr965.sourcePath = source
fr965.resourcePath = resources

venu3.sourcePath = source
venu3.resourcePath = resources

vivoactive5.sourcePath = source
vivoactive5.resourcePath = resources
EOF
    echo "✅ Created monkey.jungle"
fi

# Create developer key if it doesn't exist
if [ ! -f "$HOME/connectiq/developer_key.pem" ]; then
    echo "🔑 Creating developer key..."
    if command -v openssl >/dev/null 2>&1; then
        openssl genrsa -out "$HOME/connectiq/developer_key.pem" 2048
        echo "✅ Developer key created"
    else
        echo "⚠️ OpenSSL not found. You'll need to create a developer key manually"
    fi
fi

echo "🎉 Setup complete! You can now:"
echo "   • Build: monkeyc -d fenix7 -f monkey.jungle -o fidan-fenix7.prg -y ~/connectiq/developer_key.pem"
echo "   • Run simulator: connectiq"
echo "   • Load the .prg file in the simulator"