# Fidan Project Makefile
# Provides convenient commands for development

.PHONY: help preflight check build test lint clean setup-hooks quick-check

# Default target
help:
	@echo "Fidan Development Commands:"
	@echo ""
	@echo "  make preflight    - Run full CI validation locally"
	@echo "  make check        - Same as preflight (alias)"
	@echo "  make build        - Build Android app"
	@echo "  make test         - Run tests"
	@echo "  make lint         - Run lint checks"
	@echo "  make clean        - Clean build artifacts"
	@echo "  make setup-hooks  - Configure git hooks"
	@echo "  make quick-check  - Fast compilation check"
	@echo ""
	@echo "Android-specific:"
	@echo "  make android-build   - Build Android debug APK"
	@echo "  make android-release - Build Android release APK" 
	@echo "  make android-install - Install debug APK on device"
	@echo ""
	@echo "Deployment:"
	@echo "  make deploy         - Build & deploy both apps (debug + fr965)"
	@echo "  make deploy-debug   - Build & deploy debug versions"
	@echo "  make deploy-release - Build & deploy release versions"
	@echo "  make deploy-android - Deploy Android app only"
	@echo "  make deploy-garmin  - Deploy Garmin app only"
	@echo ""

# Main preflight check
preflight:
	@bash scripts/preflight.sh

# Alias for preflight
check: preflight

# Android build
build:
	@echo "Building Android app..."
	@cd android-app && ./gradlew build

# Run tests
test:
	@echo "Running tests..."
	@cd android-app && ./gradlew test

# Run lint
lint:
	@echo "Running lint checks..."
	@cd android-app && ./gradlew lint
	@if cd android-app && ./gradlew tasks --all | grep -q "ktlintCheck"; then \
		echo "Running KtLint..."; \
		./gradlew ktlintCheck; \
	fi

# Clean build artifacts
clean:
	@echo "Cleaning build artifacts..."
	@cd android-app && ./gradlew clean

# Setup git hooks
setup-hooks:
	@echo "Setting up git hooks..."
	@git config core.hooksPath .githooks
	@echo "Git hooks configured! Pre-commit and pre-push checks enabled."

# Quick compilation check
quick-check:
	@echo "Running quick compilation check..."
	@cd android-app && ./gradlew compileDebugKotlin --quiet

# Android specific targets
android-build:
	@echo "Building Android debug APK..."
	@cd android-app && ./gradlew assembleDebug

android-release:
	@echo "Building Android release APK..."
	@cd android-app && ./gradlew assembleRelease

android-install:
	@echo "Installing debug APK on device..."
	@cd android-app && ./gradlew installDebug

# Deployment targets
deploy:
	@echo "Building and deploying apps to both Android and Garmin devices..."
	@bash scripts/deploy.sh

deploy-debug:
	@echo "Building and deploying debug versions..."
	@bash scripts/deploy.sh debug fr965

deploy-release:
	@echo "Building and deploying release versions..."
	@bash scripts/deploy.sh release fr965

deploy-garmin:
	@echo "Building and deploying Garmin app only..."
	@bash scripts/deploy.sh debug fr965

deploy-android:
	@echo "Building and deploying Android app only..."
	@cd android-app && ./gradlew assembleDebug installDebug

# Format code
format:
	@echo "Formatting code..."
	@if cd android-app && ./gradlew tasks --all | grep -q "ktlintFormat"; then \
		./gradlew ktlintFormat; \
	else \
		echo "KtLint not configured"; \
	fi