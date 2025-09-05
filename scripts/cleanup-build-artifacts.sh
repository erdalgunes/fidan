#!/bin/bash

# Build Artifact Cleanup Script for Git Worktrees
# Addresses Issue #49: DevOps: Build artifact cleanup needed across worktrees
#
# This script safely removes build artifacts across all git worktrees:
# - build/ directories in Android projects
# - .gradle cache directories 
# - Gradle wrapper cache
# - APK/AAB outputs
# - Compiled classes and intermediate files
#
# Safety features:
# - Checks for uncommitted changes before cleaning
# - Logs all actions with timestamps
# - Reports disk space reclaimed
# - Dry-run mode for testing
#
# Usage:
#   ./cleanup-build-artifacts.sh [--dry-run] [--force] [--verbose]

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_FILE="$SCRIPT_DIR/cleanup.log"
DRY_RUN=false
FORCE=false
VERBOSE=false
TOTAL_SPACE_SAVED=0

# Color codes for output
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m' # No Color

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --force)
            FORCE=true
            shift
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        -h|--help)
            echo "Usage: $0 [--dry-run] [--force] [--verbose]"
            echo "  --dry-run  Show what would be cleaned without actually cleaning"
            echo "  --force    Skip safety checks and clean anyway"
            echo "  --verbose  Show detailed output"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Logging function
log() {
    local level=$1
    shift
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "[$timestamp] $level: $*" | tee -a "$LOG_FILE"
}

# Print function with colors
print_status() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Get directory size in bytes
get_dir_size() {
    local dir=$1
    if [[ -d "$dir" ]]; then
        du -sb "$dir" 2>/dev/null | cut -f1 || echo "0"
    else
        echo "0"
    fi
}

# Format bytes to human readable
format_bytes() {
    local bytes=$1
    if [[ $bytes -eq 0 ]]; then
        echo "0B"
    elif [[ $bytes -lt 1024 ]]; then
        echo "${bytes}B"
    elif [[ $bytes -lt 1048576 ]]; then
        echo "$(( bytes / 1024 ))KB"
    elif [[ $bytes -lt 1073741824 ]]; then
        echo "$(( bytes / 1048576 ))MB"
    else
        echo "$(( bytes / 1073741824 ))GB"
    fi
}

# Check if worktree has uncommitted changes
has_uncommitted_changes() {
    local worktree_path=$1
    cd "$worktree_path"
    
    # Check for staged changes
    if ! git diff --cached --quiet 2>/dev/null; then
        return 0
    fi
    
    # Check for unstaged changes
    if ! git diff --quiet 2>/dev/null; then
        return 0
    fi
    
    # Check for untracked files (excluding build artifacts)
    local untracked=$(git ls-files --others --exclude-standard 2>/dev/null | grep -v -E '(build/|\.gradle|\.apk$|\.aab$)' || true)
    if [[ -n "$untracked" ]]; then
        return 0
    fi
    
    return 1
}

# Clean a single worktree
clean_worktree() {
    local worktree_path=$1
    local worktree_name=$(basename "$worktree_path")
    local space_saved=0
    
    print_status "$BLUE" "🔍 Analyzing worktree: $worktree_name"
    
    # Safety check for uncommitted changes
    if [[ "$FORCE" != true ]] && has_uncommitted_changes "$worktree_path"; then
        print_status "$YELLOW" "⚠️  Skipping $worktree_name: has uncommitted changes"
        log "WARN" "Skipped $worktree_path due to uncommitted changes"
        return 0
    fi
    
    cd "$worktree_path"
    
    # Define cleanup targets with patterns
    local cleanup_targets=(
        "build"                           # Android build directories
        "app/build"                       # App-specific build directory
        "**/build"                        # Nested build directories
        ".gradle"                         # Gradle cache
        "**/.gradle"                      # Nested gradle caches
        "*.apk"                          # APK files
        "*.aab"                          # Android App Bundle files
        "**/*.apk"                       # Nested APK files
        "**/*.aab"                       # Nested AAB files
        ".cxx"                           # C++ build artifacts
        "**/.cxx"                        # Nested C++ artifacts
        "local.properties"               # Local SDK configuration (can be regenerated)
    )
    
    print_status "$GREEN" "🧹 Cleaning build artifacts in $worktree_name..."
    
    for pattern in "${cleanup_targets[@]}"; do
        # Use find for more precise control
        local files_to_clean=()
        
        case "$pattern" in
            "build"|"app/build"|".gradle"|".cxx"|"local.properties")
                # Direct path matches
                if [[ -e "$pattern" ]]; then
                    files_to_clean+=("$pattern")
                fi
                ;;
            "**/build"|"**/.gradle"|"**/.cxx")
                # Recursive directory patterns
                local base_pattern=${pattern#**/}
                while IFS= read -r -d '' file; do
                    files_to_clean+=("$file")
                done < <(find . -name "$base_pattern" -type d -print0 2>/dev/null || true)
                ;;
            "*.apk"|"*.aab"|"**/*.apk"|"**/*.aab")
                # File extension patterns
                local ext_pattern=${pattern##*.}
                while IFS= read -r -d '' file; do
                    files_to_clean+=("$file")
                done < <(find . -name "*.$ext_pattern" -type f -print0 2>/dev/null || true)
                ;;
        esac
        
        # Clean found files/directories
        if [[ ${#files_to_clean[@]} -gt 0 ]]; then
            for target in "${files_to_clean[@]}"; do
            if [[ -e "$target" ]]; then
                local size_before=$(get_dir_size "$target")
                
                if [[ "$VERBOSE" == true ]]; then
                    print_status "$BLUE" "  📁 $target ($(format_bytes $size_before))"
                fi
                
                if [[ "$DRY_RUN" == true ]]; then
                    print_status "$YELLOW" "    [DRY RUN] Would remove: $target"
                else
                    if rm -rf "$target" 2>/dev/null; then
                        space_saved=$((space_saved + size_before))
                        log "INFO" "Cleaned $worktree_path/$target ($(format_bytes $size_before))"
                    else
                        log "ERROR" "Failed to clean $worktree_path/$target"
                    fi
                fi
            fi
            done
        fi
    done
    
    # Additional cleanup for Gradle wrapper cache (global)
    local gradle_home="${HOME}/.gradle"
    if [[ -d "$gradle_home/caches" ]]; then
        local gradle_cache_size=$(get_dir_size "$gradle_home/caches")
        if [[ "$DRY_RUN" == true ]]; then
            print_status "$YELLOW" "  [DRY RUN] Would clean global Gradle cache: $(format_bytes $gradle_cache_size)"
        else
            if [[ "$FORCE" == true ]] || [[ $gradle_cache_size -gt 1073741824 ]]; then # > 1GB
                print_status "$BLUE" "  🧹 Cleaning global Gradle cache..."
                if rm -rf "$gradle_home/caches"/* 2>/dev/null; then
                    space_saved=$((space_saved + gradle_cache_size))
                    log "INFO" "Cleaned global Gradle cache ($(format_bytes $gradle_cache_size))"
                fi
            fi
        fi
    fi
    
    if [[ $space_saved -gt 0 ]] || [[ "$DRY_RUN" == true ]]; then
        local formatted_space=$(format_bytes $space_saved)
        if [[ "$DRY_RUN" == true ]]; then
            print_status "$GREEN" "✅ $worktree_name: Would reclaim ~$formatted_space"
        else
            print_status "$GREEN" "✅ $worktree_name: Reclaimed $formatted_space"
        fi
        TOTAL_SPACE_SAVED=$((TOTAL_SPACE_SAVED + space_saved))
    else
        print_status "$GREEN" "✅ $worktree_name: Already clean"
    fi
    
    echo ""
}

# Main function
main() {
    print_status "$BLUE" "🚀 Starting Build Artifact Cleanup"
    if [[ "$DRY_RUN" == true ]]; then
        print_status "$YELLOW" "🔍 DRY RUN MODE - No files will be deleted"
    fi
    echo ""
    
    # Initialize log
    log "INFO" "Starting build artifact cleanup (dry-run: $DRY_RUN, force: $FORCE)"
    
    # Get repository root
    local repo_root
    if ! repo_root=$(git rev-parse --show-toplevel 2>/dev/null); then
        print_status "$RED" "❌ Error: Not in a git repository"
        exit 1
    fi
    
    # Get list of worktrees
    local worktrees=()
    while IFS= read -r line; do
        if [[ $line == worktree* ]]; then
            local worktree_path=${line#worktree }
            worktrees+=("$worktree_path")
        fi
    done < <(git worktree list --porcelain)
    
    if [[ ${#worktrees[@]} -eq 0 ]]; then
        print_status "$YELLOW" "⚠️  No worktrees found"
        exit 0
    fi
    
    print_status "$BLUE" "📋 Found ${#worktrees[@]} worktree(s) to clean"
    echo ""
    
    # Clean each worktree
    for worktree in "${worktrees[@]}"; do
        if [[ -d "$worktree" ]]; then
            clean_worktree "$worktree"
        else
            print_status "$YELLOW" "⚠️  Worktree path does not exist: $worktree"
            log "WARN" "Worktree path does not exist: $worktree"
        fi
    done
    
    # Final summary
    print_status "$GREEN" "🎉 Cleanup Complete!"
    local formatted_total=$(format_bytes $TOTAL_SPACE_SAVED)
    if [[ "$DRY_RUN" == true ]]; then
        print_status "$BLUE" "📊 Total space that would be reclaimed: $formatted_total"
    else
        print_status "$BLUE" "📊 Total space reclaimed: $formatted_total"
    fi
    
    log "INFO" "Cleanup completed. Total space saved: $formatted_total"
    
    if [[ "$VERBOSE" == true ]]; then
        echo ""
        print_status "$BLUE" "📄 Log file: $LOG_FILE"
    fi
}

# Trap for cleanup on exit
cleanup_on_exit() {
    if [[ $? -ne 0 ]]; then
        print_status "$RED" "❌ Script failed. Check log file: $LOG_FILE"
        log "ERROR" "Script execution failed"
    fi
}
trap cleanup_on_exit EXIT

# Ensure log directory exists
mkdir -p "$(dirname "$LOG_FILE")"

# Run main function
main "$@"