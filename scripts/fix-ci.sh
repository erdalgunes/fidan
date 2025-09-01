#!/bin/bash

# Quick CI Fix Script - Resolves common CI failures

set -e

YELLOW='\033[1;33m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}Attempting to fix common CI issues...${NC}"

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "${SCRIPT_DIR}/.." && pwd )"
ANDROID_DIR="${PROJECT_ROOT}/android-app"

# 1. Fix gradle wrapper permissions
echo "Fixing gradle wrapper permissions..."
chmod +x "${ANDROID_DIR}/gradlew"

# 2. Clean build cache
echo "Cleaning build cache..."
cd "${ANDROID_DIR}"
./gradlew clean --no-daemon

# 3. Refresh dependencies
echo "Refreshing dependencies..."
./gradlew --refresh-dependencies --no-daemon

# 4. Fix any formatting issues
if ./gradlew tasks --all | grep -q "ktlintFormat"; then
    echo "Fixing code formatting..."
    ./gradlew ktlintFormat --no-daemon
fi

# 5. Rebuild
echo "Rebuilding project..."
./gradlew build --no-daemon

echo -e "${GREEN}✓ CI fixes applied. Try running preflight check now.${NC}"