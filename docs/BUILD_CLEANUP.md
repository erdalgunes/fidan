# Build Artifact Cleanup Documentation

This document describes the build artifact cleanup system implemented to address **Issue #49: DevOps: Build artifact cleanup needed across worktrees**.

## Problem Statement

Multiple worktrees contain build artifacts that can cause conflicts and consume unnecessary disk space:

- Found build artifacts in `main/issue-39-garmin-sync/android-app/app/build/`
- Found build artifacts in `issue-16/android-app/app/build/`
- Multiple `.gradle` cache directories across worktrees
- Increased disk usage and potential conflicts between different branch builds
- Slower checkout/sync operations and inconsistent build states

## Solution Overview

The cleanup system consists of three main components:

1. **Manual Cleanup Script** (`scripts/cleanup-build-artifacts.sh`)
2. **Automated GitHub Actions Workflow** (`.github/workflows/build-artifact-cleanup.yml`)
3. **Developer Documentation** (this file)

## Manual Cleanup Script

### Usage

```bash
# Basic cleanup
./scripts/cleanup-build-artifacts.sh

# Dry run to see what would be cleaned
./scripts/cleanup-build-artifacts.sh --dry-run

# Force cleanup even with uncommitted changes
./scripts/cleanup-build-artifacts.sh --force

# Verbose output
./scripts/cleanup-build-artifacts.sh --verbose

# Combined options
./scripts/cleanup-build-artifacts.sh --dry-run --verbose
```

### What Gets Cleaned

The script removes the following artifacts across all git worktrees:

#### Android Build Artifacts
- `build/` directories (main build outputs)
- `app/build/` directories (app-specific builds)
- `**/build/` directories (nested build directories)
- `*.apk` files (Android packages)
- `*.aab` files (Android App Bundles)
- `.cxx/` directories (C++ build artifacts)

#### Gradle Cache Files
- `.gradle/` directories (local Gradle caches)
- `**/.gradle/` directories (nested Gradle caches)
- Global Gradle cache (`~/.gradle/caches/`) when > 1GB
- `local.properties` files (can be regenerated)

### Safety Features

#### Uncommitted Changes Check
- Scans for staged changes (`git diff --cached`)
- Scans for unstaged changes (`git diff`)
- Scans for untracked files (excluding build artifacts)
- Skips worktree if changes detected (unless `--force` used)

#### Logging and Reporting
- Comprehensive logging to `scripts/cleanup.log`
- Timestamped entries for all actions
- Size calculation before/after cleanup
- Color-coded terminal output
- Human-readable size formatting

#### Error Handling
- Graceful handling of missing directories
- Trap for cleanup on script failure
- Detailed error messages and suggestions

## Automated Cleanup

### GitHub Actions Workflow

The workflow runs automatically:
- **Weekly**: Every Sunday at 2 AM UTC
- **Manual**: Can be triggered via GitHub Actions UI

#### Workflow Features
- **Dry-run mode**: Test cleanup without making changes
- **Force mode**: Override safety checks
- **Log artifacts**: Uploads cleanup logs for review
- **Status reporting**: GitHub Actions annotations with results
- **Auto-commit**: Commits log files to track cleanup history

#### Manual Trigger Options
1. Navigate to Actions tab in GitHub repository
2. Select "Build Artifact Cleanup" workflow
3. Click "Run workflow"
4. Choose options:
   - **Dry run**: Check what would be cleaned
   - **Force**: Skip uncommitted change checks

## Integration with Development Workflow

### Pre-commit Hook (Optional)

Add to `.git/hooks/pre-commit`:

```bash
#!/bin/bash
# Optional: Clean build artifacts before committing
./scripts/cleanup-build-artifacts.sh --force > /dev/null 2>&1 || true
```

### IDE Integration

#### VS Code
Add to `.vscode/tasks.json`:

```json
{
    "version": "2.0.0",
    "tasks": [
        {
            "label": "Clean Build Artifacts",
            "type": "shell",
            "command": "./scripts/cleanup-build-artifacts.sh",
            "args": ["--verbose"],
            "group": "build",
            "presentation": {
                "echo": true,
                "reveal": "always",
                "focus": false,
                "panel": "shared"
            }
        }
    ]
}
```

#### Android Studio
1. Go to **File > Settings > Tools > External Tools**
2. Add new tool:
   - **Name**: Clean Build Artifacts
   - **Program**: `bash`
   - **Arguments**: `scripts/cleanup-build-artifacts.sh --verbose`
   - **Working Directory**: `$ProjectFileDir$`

## Performance Impact

### Typical Space Savings
- **Small projects**: 100-500 MB per worktree
- **Medium projects**: 500 MB - 2 GB per worktree  
- **Large projects**: 2-10 GB per worktree
- **Global Gradle cache**: 1-5 GB

### Execution Time
- **Single worktree**: 5-30 seconds
- **Multiple worktrees**: 1-5 minutes
- **Network impact**: None (local cleanup only)

## Troubleshooting

### Common Issues

#### Permission Denied
```bash
# Fix: Make script executable
chmod +x scripts/cleanup-build-artifacts.sh
```

#### Script Not Found
```bash
# Fix: Run from repository root
cd /path/to/repository
./scripts/cleanup-build-artifacts.sh
```

#### Worktree Has Uncommitted Changes
```bash
# Option 1: Use force flag
./scripts/cleanup-build-artifacts.sh --force

# Option 2: Commit or stash changes first
git add . && git commit -m "WIP: save before cleanup"
./scripts/cleanup-build-artifacts.sh
```

#### Global Gradle Cache Not Cleaned
The script only cleans global Gradle cache if:
- Size > 1 GB, OR
- `--force` flag is used

### Log Analysis

Check `scripts/cleanup.log` for detailed information:

```bash
# View recent cleanup activity
tail -20 scripts/cleanup.log

# Search for errors
grep "ERROR" scripts/cleanup.log

# View space savings
grep "Total space" scripts/cleanup.log
```

## Best Practices

### For Developers
1. **Run before major merges**: Clean artifacts to avoid conflicts
2. **Use dry-run first**: Always test with `--dry-run` on new worktrees
3. **Check logs**: Review cleanup logs periodically
4. **Commit first**: Save work before running cleanup

### For DevOps
1. **Monitor automation**: Check GitHub Actions cleanup results weekly
2. **Adjust schedule**: Modify cron schedule based on team activity
3. **Log retention**: Keep cleanup logs for trend analysis
4. **Disk monitoring**: Set up alerts for low disk space

### For CI/CD
1. **Clean before builds**: Run cleanup in CI pipeline start
2. **Cache management**: Consider cleanup in cache warming steps
3. **Artifact retention**: Set appropriate retention policies
4. **Build optimization**: Clean builds may improve performance

## Metrics and Monitoring

### Key Metrics
- **Space reclaimed per cleanup**: Track storage savings
- **Cleanup frequency**: Monitor automated vs manual runs
- **Failure rate**: Track worktrees skipped due to changes
- **Execution time**: Monitor performance across repository growth

### Monitoring Setup
- GitHub Actions provide built-in run history
- Log files contain timestamped entries for analysis
- Repository size can be tracked via git commands

## Security Considerations

### What's Safe to Delete
- Build outputs (can be regenerated)
- Gradle caches (will be re-downloaded)
- Compiled binaries and intermediate files
- Local configuration files (like `local.properties`)

### What's Never Deleted
- Source code files
- Committed files
- Configuration templates
- Documentation
- Test files
- Resource files

### Data Safety
- Multiple confirmation checks before deletion
- Dry-run mode for testing
- Comprehensive logging of all deletions
- Git status verification before proceeding

## Future Enhancements

### Planned Features
1. **Size quotas**: Configurable maximum sizes per worktree
2. **Retention policies**: Keep artifacts newer than X days
3. **Selective cleaning**: Clean specific artifact types only
4. **Integration hooks**: Pre/post cleanup hooks for custom logic
5. **Metrics dashboard**: Visual reporting of cleanup trends

### Configuration Options
Future versions may include `cleanup-config.yml`:

```yaml
# Example future configuration
cleanup:
  retention_days: 7
  max_worktree_size: "2GB"
  excluded_patterns:
    - "*.keystore"
    - "release-keys/"
  gradle_cache_threshold: "1GB"
  notification:
    slack_webhook: "https://..."
```

## Contributing

### Reporting Issues
1. Include full error output
2. Specify worktree layout and sizes
3. Include relevant log excerpts
4. Describe expected vs actual behavior

### Improving the Script
1. Test changes with `--dry-run` extensively
2. Update documentation for new features
3. Add appropriate error handling
4. Include logging for new actions

---

## Quick Reference

```bash
# Most common commands
./scripts/cleanup-build-artifacts.sh --dry-run    # See what would be cleaned
./scripts/cleanup-build-artifacts.sh --verbose   # Clean with detailed output
./scripts/cleanup-build-artifacts.sh --force     # Clean despite uncommitted changes

# Check results
tail scripts/cleanup.log                          # View recent log entries
du -sh */build 2>/dev/null || echo "No builds"  # Check remaining build dirs
```

**This cleanup system addresses the storage and conflict issues identified in Issue #49 while maintaining safety and providing comprehensive DevOps automation.**