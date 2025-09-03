#!/bin/bash

# Garmin Connect IQ App Preflight Check Script
# Validates the Garmin app structure and catches common CI/CD errors

set +e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
APP_DIR="${SCRIPT_DIR}"
ERRORS=0
WARNINGS=0

# Colors for output
RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

echo "🚀 Running Garmin App Preflight Checks..."
echo "==========================================="

# Function to log errors
log_error() {
    echo -e "${RED}❌ ERROR: $1${NC}"
    ((ERRORS++))
}

# Function to log warnings
log_warning() {
    echo -e "${YELLOW}⚠️  WARNING: $1${NC}"
    ((WARNINGS++))
}

# Function to log success
log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

# 1. Check required directories
echo -e "\n📁 Checking directory structure..."
REQUIRED_DIRS=("source" "resources" "resources/strings" "resources/drawables" "resources/settings")
for dir in "${REQUIRED_DIRS[@]}"; do
    if [ -d "$APP_DIR/$dir" ]; then
        log_success "Directory $dir exists"
    else
        log_error "Missing required directory: $dir"
    fi
done

# 2. Check required files
echo -e "\n📄 Checking required files..."
REQUIRED_FILES=(
    "manifest.xml"
    "resources/strings/strings.xml"
    "resources/drawables/drawables.xml"
)
for file in "${REQUIRED_FILES[@]}"; do
    if [ -f "$APP_DIR/$file" ]; then
        log_success "File $file exists"
    else
        log_error "Missing required file: $file"
    fi
done

# 3. Validate manifest.xml
echo -e "\n🔍 Validating manifest.xml..."
if [ -f "$APP_DIR/manifest.xml" ]; then
    # Check for required elements
    if grep -q 'iq:application' "$APP_DIR/manifest.xml"; then
        log_success "Application element found"
    else
        log_error "Missing iq:application element in manifest.xml"
    fi
    
    if grep -q 'entry=' "$APP_DIR/manifest.xml"; then
        ENTRY_CLASS=$(grep -o 'entry="[^"]*"' "$APP_DIR/manifest.xml" | sed 's/entry="\(.*\)"/\1/')
        log_success "Entry class defined: $ENTRY_CLASS"
        
        # Check if entry class file exists
        if [ -f "$APP_DIR/source/${ENTRY_CLASS}.mc" ]; then
            log_success "Entry class file exists: source/${ENTRY_CLASS}.mc"
        else
            log_error "Entry class file not found: source/${ENTRY_CLASS}.mc"
        fi
    else
        log_error "No entry point defined in manifest.xml"
    fi
    
    if grep -q 'iq:product' "$APP_DIR/manifest.xml"; then
        PRODUCT_COUNT=$(grep -c 'iq:product' "$APP_DIR/manifest.xml")
        log_success "Found $PRODUCT_COUNT device targets"
    else
        log_error "No device targets defined in manifest.xml"
    fi
    
    # Check API version
    if grep -q 'minApiLevel=' "$APP_DIR/manifest.xml"; then
        API_LEVEL=$(grep -o 'minApiLevel="[^"]*"' "$APP_DIR/manifest.xml" | sed 's/minApiLevel="\(.*\)"/\1/')
        log_success "Min API level: $API_LEVEL"
    else
        log_warning "No minApiLevel specified in manifest.xml"
    fi
fi

# 4. Check Monkey C files syntax
echo -e "\n🐒 Checking Monkey C files..."
MC_FILES=$(find "$APP_DIR/source" -name "*.mc" 2>/dev/null)
if [ -z "$MC_FILES" ]; then
    log_error "No Monkey C source files found"
else
    FILE_COUNT=$(echo "$MC_FILES" | wc -l)
    log_success "Found $FILE_COUNT Monkey C files"
    
    # Basic syntax checks
    for file in $MC_FILES; do
        filename=$(basename "$file")
        
        # Check for class definition matching filename
        classname="${filename%.mc}"
        if grep -q "class $classname" "$file"; then
            log_success "$filename: Class definition found"
        else
            log_warning "$filename: No matching class definition found"
        fi
        
        # Check for common syntax errors
        if grep -q 'using Toybox' "$file"; then
            :  # Good - has imports
        else
            log_warning "$filename: No Toybox imports found"
        fi
        
        # Check for unbalanced braces
        open_braces=$(grep -o '{' "$file" | wc -l)
        close_braces=$(grep -o '}' "$file" | wc -l)
        if [ "$open_braces" -ne "$close_braces" ]; then
            log_error "$filename: Unbalanced braces (open: $open_braces, close: $close_braces)"
        fi
        
        # Check for unbalanced parentheses
        open_parens=$(grep -o '(' "$file" | wc -l)
        close_parens=$(grep -o ')' "$file" | wc -l)
        if [ "$open_parens" -ne "$close_parens" ]; then
            log_error "$filename: Unbalanced parentheses (open: $open_parens, close: $close_parens)"
        fi
    done
fi

# 5. Validate XML resources
echo -e "\n📋 Validating XML resources..."
XML_FILES=$(find "$APP_DIR/resources" -name "*.xml" 2>/dev/null)
for file in $XML_FILES; do
    filename=$(basename "$file")
    if xmllint --noout "$file" 2>/dev/null; then
        log_success "$filename: Valid XML"
    else
        # Try basic XML validation if xmllint not available
        if head -1 "$file" | grep -q '<?xml'; then
            log_success "$filename: Has XML declaration"
        else
            log_warning "$filename: Missing XML declaration"
        fi
    fi
done

# 6. Check for required string resources
echo -e "\n📝 Checking string resources..."
if [ -f "$APP_DIR/resources/strings/strings.xml" ]; then
    if grep -q 'AppName' "$APP_DIR/resources/strings/strings.xml"; then
        log_success "AppName string defined"
    else
        log_error "AppName string not defined in strings.xml"
    fi
fi

# 7. Check for icon resources
echo -e "\n🎨 Checking icon resources..."
if [ -f "$APP_DIR/resources/drawables/drawables.xml" ]; then
    if grep -q 'LauncherIcon' "$APP_DIR/resources/drawables/drawables.xml"; then
        log_success "LauncherIcon defined in drawables.xml"
        
        # Check if actual icon file exists
        ICON_FILE=$(grep -o 'filename="[^"]*"' "$APP_DIR/resources/drawables/drawables.xml" | sed 's/filename="\(.*\)"/\1/' | head -1)
        if [ -n "$ICON_FILE" ]; then
            if [ -f "$APP_DIR/resources/drawables/$ICON_FILE" ]; then
                log_success "Icon file exists: $ICON_FILE"
            else
                log_warning "Icon file not found: $ICON_FILE (will need to be created)"
            fi
        fi
    else
        log_error "LauncherIcon not defined in drawables.xml"
    fi
fi

# 8. Check for common issues
echo -e "\n🔎 Checking for common issues..."

# Check for TODO comments
TODO_COUNT=$(grep -r "TODO" "$APP_DIR/source" 2>/dev/null | wc -l || echo 0)
if [ "$TODO_COUNT" -gt 0 ]; then
    log_warning "Found $TODO_COUNT TODO comments in source files"
fi

# Check for debug prints
DEBUG_COUNT=$(grep -r "System.println" "$APP_DIR/source" 2>/dev/null | wc -l || echo 0)
if [ "$DEBUG_COUNT" -gt 0 ]; then
    log_warning "Found $DEBUG_COUNT debug print statements"
fi

# Check for hardcoded values that should be configurable
HARDCODED_COUNT=$(grep -r "1500\|25" "$APP_DIR/source" 2>/dev/null | grep -v "const" | wc -l || echo 0)
if [ "$HARDCODED_COUNT" -gt 0 ]; then
    log_warning "Found potential hardcoded values that could be constants"
fi

# 9. Memory usage estimation
echo -e "\n💾 Estimating memory usage..."
TOTAL_LINES=$(find "$APP_DIR/source" -name "*.mc" -exec wc -l {} + 2>/dev/null | tail -1 | awk '{print $1}')
if [ -n "$TOTAL_LINES" ]; then
    log_success "Total source lines: $TOTAL_LINES"
    if [ "$TOTAL_LINES" -gt 5000 ]; then
        log_warning "Large codebase - consider memory optimization for older devices"
    fi
fi

# 10. Check permissions in manifest
echo -e "\n🔐 Checking permissions..."
if [ -f "$APP_DIR/manifest.xml" ]; then
    if grep -q 'Communications' "$APP_DIR/manifest.xml"; then
        log_success "Communications permission declared"
    else
        log_warning "No Communications permission - sync features may not work"
    fi
    
    if grep -q 'Background' "$APP_DIR/manifest.xml"; then
        log_success "Background permission declared"
    else
        log_warning "No Background permission - background sync disabled"
    fi
fi

# Summary
echo -e "\n==========================================="
echo "📊 Preflight Check Summary"
echo "==========================================="

if [ $ERRORS -eq 0 ]; then
    if [ $WARNINGS -eq 0 ]; then
        log_success "All checks passed! Ready for CI/CD 🎉"
        exit 0
    else
        echo -e "${YELLOW}Checks completed with $WARNINGS warnings${NC}"
        echo "The app should build but review warnings above"
        exit 0
    fi
else
    echo -e "${RED}Found $ERRORS errors and $WARNINGS warnings${NC}"
    echo "Please fix errors before proceeding with CI/CD"
    exit 1
fi