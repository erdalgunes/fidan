#!/bin/bash

# Preflight Check Script - Catches CI issues locally before pushing
# Runs all the same checks as GitHub Actions CI

set -e  # Exit on error

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Icons
CHECK="✓"
CROSS="✗"
ARROW="→"

# Script variables
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "${SCRIPT_DIR}/.." && pwd )"
ANDROID_DIR="${PROJECT_ROOT}/android-app"

# Functions
print_header() {
    echo -e "\n${BLUE}=====================================
$1
=====================================${NC}\n"
}

print_success() {
    echo -e "${GREEN}${CHECK} $1${NC}"
}

print_error() {
    echo -e "${RED}${CROSS} $1${NC}"
}

print_info() {
    echo -e "${YELLOW}${ARROW} $1${NC}"
}

# Check if running from correct location
if [ ! -d "${ANDROID_DIR}" ]; then
    print_error "Error: android-app directory not found"
    print_info "Please run this script from the project root"
    exit 1
fi

print_header "🚀 PREFLIGHT CHECKS"
echo "Running local CI validation to catch issues before push..."

# Track failures
FAILED_CHECKS=()

# 1. Java Version Check
print_header "1. JAVA VERSION CHECK"
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge "17" ]; then
        print_success "Java $JAVA_VERSION detected (17+ required)"
    else
        print_error "Java 17+ required (found: $JAVA_VERSION)"
        FAILED_CHECKS+=("Java version")
    fi
else
    print_error "Java not found. Please install JDK 17+"
    FAILED_CHECKS+=("Java installation")
fi

# 2. Gradle Wrapper Check
print_header "2. GRADLE WRAPPER CHECK"
if [ -f "${ANDROID_DIR}/gradlew" ]; then
    if [ -x "${ANDROID_DIR}/gradlew" ]; then
        print_success "Gradle wrapper is executable"
    else
        print_info "Making gradlew executable..."
        chmod +x "${ANDROID_DIR}/gradlew"
        print_success "Fixed gradle wrapper permissions"
    fi
else
    print_error "Gradle wrapper not found"
    FAILED_CHECKS+=("Gradle wrapper")
fi

# 3. Build Check
print_header "3. BUILD CHECK"
print_info "Running gradle build..."
cd "${ANDROID_DIR}"
if ./gradlew build --no-daemon; then
    print_success "Build completed successfully"
else
    print_error "Build failed"
    FAILED_CHECKS+=("Build")
fi

# 4. Test Check
print_header "4. TEST CHECK"
print_info "Running tests..."
if ./gradlew test --no-daemon; then
    print_success "All tests passed"
else
    print_error "Tests failed"
    FAILED_CHECKS+=("Tests")
fi

# 5. Lint Check
print_header "5. LINT CHECK"
print_info "Running Android lint..."
if ./gradlew lint --no-daemon; then
    print_success "Lint checks passed"
else
    print_error "Lint issues found"
    FAILED_CHECKS+=("Lint")
fi

# 6. Detekt Check (if configured)
print_header "6. STATIC ANALYSIS CHECK"
if ./gradlew tasks --all | grep -q "detekt"; then
    print_info "Running Detekt static analysis..."
    if ./gradlew detekt --no-daemon; then
        print_success "Detekt analysis passed"
    else
        print_error "Detekt found issues"
        FAILED_CHECKS+=("Detekt")
    fi
else
    print_info "Detekt not configured, skipping..."
fi

# 7. KtLint Check (if configured)
if ./gradlew tasks --all | grep -q "ktlint"; then
    print_info "Running KtLint formatting check..."
    if ./gradlew ktlintCheck --no-daemon; then
        print_success "KtLint formatting check passed"
    else
        print_error "KtLint formatting issues found"
        print_info "Run './gradlew ktlintFormat' to fix automatically"
        FAILED_CHECKS+=("KtLint")
    fi
else
    print_info "KtLint not configured, skipping..."
fi

# 8. Git Status Check
print_header "7. GIT STATUS CHECK"
cd "${PROJECT_ROOT}"
if [ -n "$(git status --porcelain)" ]; then
    print_info "Uncommitted changes detected:"
    git status --short
else
    print_success "Working directory clean"
fi

# Summary
print_header "📊 PREFLIGHT SUMMARY"
if [ ${#FAILED_CHECKS[@]} -eq 0 ]; then
    echo -e "${GREEN}${CHECK} ALL CHECKS PASSED!${NC}"
    echo -e "${GREEN}You're ready to push your changes.${NC}"
    exit 0
else
    echo -e "${RED}${CROSS} PREFLIGHT FAILED${NC}"
    echo -e "${RED}Failed checks:${NC}"
    for check in "${FAILED_CHECKS[@]}"; do
        echo -e "  ${RED}- $check${NC}"
    done
    echo -e "\n${YELLOW}Fix the issues above before pushing to avoid CI failures.${NC}"
    exit 1
fi