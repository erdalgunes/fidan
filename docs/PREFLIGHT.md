# Preflight Check System

## Overview

The preflight system helps catch CI failures locally before pushing code, saving time and preventing broken builds. It runs the same checks as our GitHub Actions CI pipeline.

## Quick Start

```bash
# Run full preflight check
make preflight

# Or directly
./scripts/preflight.sh
```

## Available Commands

### Make Commands

```bash
make preflight    # Run full CI validation
make check        # Alias for preflight
make quick-check  # Fast compilation check only
make build        # Build the project
make test         # Run tests
make lint         # Run lint checks
make format       # Auto-format code
make clean        # Clean build artifacts
```

### Setup Git Hooks

Enable automatic checks on commit and push:

```bash
make setup-hooks
```

This configures:
- **pre-commit**: Quick compilation and formatting checks
- **pre-push**: Full preflight validation

### Bypass Hooks (Emergency Only)

```bash
git commit --no-verify  # Skip pre-commit
git push --no-verify    # Skip pre-push
```

## What Gets Checked

### 1. Pre-commit (Fast)
- Kotlin compilation
- KtLint formatting (if configured)

### 2. Pre-push / Preflight (Complete)
- Java version (17 or 21 required)
- Gradle wrapper permissions
- Full build
- All tests
- Android lint
- Detekt static analysis (if configured)
- KtLint formatting (if configured)
- Git status

## Troubleshooting

### Build Fails Locally But Not in CI

1. Check Java version: `java -version` (need 17 or 21)
2. Clean build: `make clean && make build`
3. Update dependencies: `cd android-app && ./gradlew --refresh-dependencies`

### Permission Denied on gradlew

```bash
chmod +x android-app/gradlew
```

### Hooks Not Running

```bash
# Verify hooks are configured
git config core.hooksPath

# Should output: .githooks
# If not, run:
make setup-hooks
```

### KtLint Formatting Issues

```bash
# Auto-fix formatting
make format

# Or directly
cd android-app && ./gradlew ktlintFormat
```

## CI/CD Pipeline

Our GitHub Actions workflow (`/.github/workflows/android.yml`) runs:

1. Setup JDK 17
2. Build with Gradle
3. Run tests
4. Upload artifacts

The preflight system mirrors these steps locally.

## Best Practices

1. **Always run preflight before pushing** to feature branches
2. **Use quick-check during development** for fast feedback
3. **Configure git hooks** for automatic validation
4. **Fix issues immediately** - don't accumulate technical debt
5. **Keep preflight script updated** when CI changes

## Adding New Checks

To add a new check to preflight:

1. Edit `scripts/preflight.sh`
2. Add your check in a new section
3. Update the `FAILED_CHECKS` array on failure
4. Test locally
5. Update CI workflow if needed

Example:
```bash
# New Security Check
print_header "X. SECURITY CHECK"
if ./gradlew dependencyCheckAnalyze; then
    print_success "No security vulnerabilities found"
else
    print_error "Security issues detected"
    FAILED_CHECKS+=("Security")
fi
```

## Contributing

When modifying the preflight system:

1. Test changes locally first
2. Ensure parity with CI pipeline
3. Update this documentation
4. Consider performance impact

## Questions?

- Check build logs: `android-app/build/reports/`
- Run with verbose: `./gradlew build --info`
- Check CI logs on GitHub Actions