#!/bin/bash
# Check for problematic untracked files that might cause build issues

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() { echo -e "${BLUE}ℹ️  $1${NC}"; }
log_success() { echo -e "${GREEN}✅ $1${NC}"; }
log_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }
log_error() { echo -e "${RED}❌ $1${NC}"; }

echo "🔍 Checking for problematic untracked files..."
echo

# Get untracked files
UNTRACKED_FILES=$(git ls-files --others --exclude-standard 2>/dev/null || true)

if [ -z "$UNTRACKED_FILES" ]; then
    log_success "No untracked files found"
    exit 0
fi

echo "📁 Untracked files found:"
echo "$UNTRACKED_FILES"
echo

# Categorize files
CRITICAL_FILES=""
BUILD_ARTIFACTS=""
CONFIG_FILES=""
TEMP_FILES=""
UNKNOWN_FILES=""

while IFS= read -r file; do
    case "$file" in
        # Critical application files
        *.xml|*.json|*.kt|*.java|*.js|*.ts|*.py|*.go|*.rs)
            if [[ "$file" == *"src/main"* ]] || [[ "$file" == *"src/test"* ]]; then
                CRITICAL_FILES="$CRITICAL_FILES\n  $file"
            fi
            ;;
        # Build artifacts
        *.apk|*.prg|*.iq|*.der|*.pem|*/build/*|*/gen/*|*/bin/*)
            BUILD_ARTIFACTS="$BUILD_ARTIFACTS\n  $file"
            ;;
        # Config files
        *.properties|*.gradle|*.yml|*.yaml)
            CONFIG_FILES="$CONFIG_FILES\n  $file"
            ;;
        # Temp/log files
        *.log|*.tmp|*_report_*.json|temp_*|*orchestrator.py)
            TEMP_FILES="$TEMP_FILES\n  $file"
            ;;
        *)
            UNKNOWN_FILES="$UNKNOWN_FILES\n  $file"
            ;;
    esac
done <<< "$UNTRACKED_FILES"

# Report findings
EXIT_CODE=0

if [ -n "$CRITICAL_FILES" ]; then
    log_error "Critical application files (should be tracked):"
    echo -e "$CRITICAL_FILES"
    echo "  👉 Run: git add <file> to track these files"
    EXIT_CODE=1
    echo
fi

if [ -n "$BUILD_ARTIFACTS" ]; then
    log_warning "Build artifacts (should be in .gitignore):"
    echo -e "$BUILD_ARTIFACTS"
    echo "  👉 These files should be added to .gitignore"
    echo
fi

if [ -n "$CONFIG_FILES" ]; then
    log_warning "Configuration files (review needed):"
    echo -e "$CONFIG_FILES" 
    echo "  👉 Determine if these should be tracked or ignored"
    echo
fi

if [ -n "$TEMP_FILES" ]; then
    log_info "Temporary files (safe to delete):"
    echo -e "$TEMP_FILES"
    echo "  👉 These can be safely deleted"
    echo
fi

if [ -n "$UNKNOWN_FILES" ]; then
    log_warning "Unknown files (manual review needed):"
    echo -e "$UNKNOWN_FILES"
    echo "  👉 Review these files manually"
    echo
fi

# Provide recommendations
echo "🔧 Recommendations:"
echo

if [ "$EXIT_CODE" -eq 1 ]; then
    echo "1. 🚨 Track critical files immediately:"
    echo "   git add <critical-files>"
    echo "   git commit -m 'Add missing application files'"
    echo
fi

if [ -n "$BUILD_ARTIFACTS" ] || [ -n "$TEMP_FILES" ]; then
    echo "2. 🧹 Clean up temporary/build files:"
    echo "   rm -rf <temp-files>"
    echo "   # Update .gitignore to prevent future issues"
    echo
fi

echo "3. ✅ Set up pre-commit hook:"
echo "   # Add this script to .git/hooks/pre-commit"
echo "   # Or use tools like husky/pre-commit"
echo

echo "4. 📋 Regular maintenance:"
echo "   # Run this script periodically: ./scripts/check-untracked.sh"
echo "   # Keep .gitignore up to date"

exit $EXIT_CODE