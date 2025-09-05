#!/bin/bash

# AI-Powered PR Review Bot
# Uses Tavily for research and GPT-5 for code analysis
# Follows DRY, KISS, YAGNI, SOLID principles

set -euo pipefail

# Configuration
MAX_DIFF_SIZE=50000
MAX_TOTAL_CHANGES=2000
COST_CONTROL_ENABLED=true

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Utility functions
log_info() { echo -e "${BLUE}ℹ️  $1${NC}"; }
log_success() { echo -e "${GREEN}✅ $1${NC}"; }
log_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }
log_error() { echo -e "${RED}❌ $1${NC}"; }

# Check dependencies
check_dependencies() {
    local missing_deps=()
    
    command -v git >/dev/null || missing_deps+=("git")
    command -v gh >/dev/null || missing_deps+=("gh")
    
    if [ ${#missing_deps[@]} -gt 0 ]; then
        log_error "Missing dependencies: ${missing_deps[*]}"
        exit 1
    fi
    
    # Optional dependencies
    TAVILY_AVAILABLE=false
    GPT5_AVAILABLE=false
    
    if command -v tavily >/dev/null; then
        TAVILY_AVAILABLE=true
        log_success "Tavily available for research"
    else
        log_warning "Tavily not available - skipping security research"
    fi
    
    if command -v gpt5 >/dev/null; then
        GPT5_AVAILABLE=true  
        log_success "GPT-5 available for analysis"
    else
        log_warning "GPT-5 not available - skipping code analysis"
    fi
}

# Get PR information
get_pr_info() {
    if [ $# -eq 0 ]; then
        log_error "Usage: $0 <PR_NUMBER> [BASE_BRANCH]"
        log_info "Example: $0 123 main"
        exit 1
    fi
    
    PR_NUMBER=$1
    BASE_BRANCH=${2:-main}
    
    log_info "Analyzing PR #$PR_NUMBER against $BASE_BRANCH"
    
    # Get PR details
    if ! PR_INFO=$(gh pr view "$PR_NUMBER" --json title,body,headRefName,draft 2>/dev/null); then
        log_error "Failed to get PR information. Make sure you're in a git repo with gh auth."
        exit 1
    fi
    
    PR_TITLE=$(echo "$PR_INFO" | jq -r '.title')
    PR_BRANCH=$(echo "$PR_INFO" | jq -r '.headRefName')
    IS_DRAFT=$(echo "$PR_INFO" | jq -r '.draft')
    
    if [ "$IS_DRAFT" = "true" ]; then
        log_warning "Skipping draft PR to save costs"
        exit 0
    fi
    
    log_info "PR: $PR_TITLE"
    log_info "Branch: $PR_BRANCH"
}

# Analyze changes
analyze_changes() {
    log_info "Analyzing code changes..."
    
    # Get changed files
    if ! git diff --name-status "origin/$BASE_BRANCH...origin/$PR_BRANCH" > changed_files.txt 2>/dev/null; then
        log_error "Failed to get diff. Make sure branches are up to date."
        exit 1
    fi
    
    CHANGED_FILES_COUNT=$(wc -l < changed_files.txt)
    
    # Calculate total changes
    TOTAL_CHANGES=0
    if DIFF_STATS=$(git diff --numstat "origin/$BASE_BRANCH...origin/$PR_BRANCH" 2>/dev/null); then
        TOTAL_CHANGES=$(echo "$DIFF_STATS" | awk '{ lines += $1 + $2 } END { print lines+0 }')
    fi
    
    log_info "Files changed: $CHANGED_FILES_COUNT"
    log_info "Total changes: $TOTAL_CHANGES lines"
    
    # Cost control check
    if [ "$COST_CONTROL_ENABLED" = "true" ] && [ "$TOTAL_CHANGES" -gt $MAX_TOTAL_CHANGES ]; then
        log_warning "PR too large ($TOTAL_CHANGES lines) - skipping detailed analysis"
        echo "## 🤖 AI Review - PR Too Large

This PR has $TOTAL_CHANGES lines of changes, exceeding the analysis limit.

**Recommendations:**
- Split into smaller PRs (< 500 lines each)
- Focus on single responsibilities per PR
- Use our PR size guidelines

*Detailed AI review available for smaller PRs*"
        return 0
    fi
    
    # Extract file types
    FILE_TYPES=$(git diff --name-only "origin/$BASE_BRANCH...origin/$PR_BRANCH" | sed 's/.*\.//' | sort -u | tr '\n' ' ' | xargs)
    
    # Check for dependency changes
    DEPS_CHANGED=""
    if grep -qE "(build\.gradle|package\.json|requirements\.txt|Cargo\.toml|go\.mod|pom\.xml)" changed_files.txt 2>/dev/null; then
        DEPS_CHANGED="true"
        log_info "Dependencies modified detected"
    fi
}

# Research using Tavily
research_security() {
    if [ "$TAVILY_AVAILABLE" != "true" ]; then
        return 0
    fi
    
    echo "## 🔐 Security & Best Practices Research"
    echo ""
    
    if [ -n "$DEPS_CHANGED" ]; then
        log_info "Researching dependency security..."
        if ! tavily "security vulnerabilities $FILE_TYPES dependencies 2024" --max-results 2 2>/dev/null; then
            echo "⚠️ Security research failed"
        fi
        echo ""
    fi
    
    # Research best practices for file types
    if [ -n "$FILE_TYPES" ]; then
        log_info "Researching best practices for: $FILE_TYPES"
        if ! tavily "$FILE_TYPES code best practices security 2024" --max-results 2 2>/dev/null; then
            echo "⚠️ Best practices research failed"
        fi
        echo ""
    fi
}

# Code analysis using GPT-5
analyze_code() {
    if [ "$GPT5_AVAILABLE" != "true" ]; then
        echo "## 📝 Code Analysis"
        echo "⚠️ GPT-5 not available for detailed code analysis"
        echo ""
        return 0
    fi
    
    log_info "Performing AI code analysis..."
    
    # Generate code diff
    git diff "origin/$BASE_BRANCH...origin/$PR_BRANCH" -- '*.kt' '*.java' '*.js' '*.ts' '*.py' '*.go' '*.rs' '*.swift' '*.cpp' '*.c' '*.cs' > code_diff.txt 2>/dev/null || true
    
    if [ ! -s code_diff.txt ]; then
        echo "## 📝 Code Analysis"
        echo "ℹ️ No code files changed or diff unavailable"
        echo ""
        return 0
    fi
    
    # Truncate if too large
    DIFF_SIZE=$(wc -c < code_diff.txt)
    if [ "$DIFF_SIZE" -gt $MAX_DIFF_SIZE ]; then
        head -c $MAX_DIFF_SIZE code_diff.txt > code_diff_truncated.txt
        mv code_diff_truncated.txt code_diff.txt
        log_warning "Code diff truncated to ${MAX_DIFF_SIZE} characters"
    fi
    
    echo "## 📝 Code Analysis"
    echo ""
    
    # Create analysis prompt
    cat > analysis_prompt.txt << EOF
Analyze this code diff for potential issues:

$(cat code_diff.txt)

Focus on:
1. 🐛 Potential bugs or logic errors
2. 🔒 Security vulnerabilities 
3. ⚡ Performance concerns
4. 🧹 Code quality improvements
5. 🏗️  Architecture feedback

Provide specific, actionable feedback with line references where possible.
If no significant issues, provide brief positive feedback.
Use emojis and clear formatting.
EOF
    
    # Run GPT-5 analysis
    if ! gpt5 "$(cat analysis_prompt.txt)" --effort medium --max-tokens 1000 2>/dev/null; then
        echo "⚠️ AI code analysis failed"
    fi
    echo ""
}

# Generate summary
generate_summary() {
    echo "## 📋 Review Summary"
    echo ""
    echo "| Metric | Value |"
    echo "|--------|-------|"
    echo "| **Files Changed** | $CHANGED_FILES_COUNT |"
    echo "| **Total Changes** | $TOTAL_CHANGES lines |"
    echo "| **File Types** | $FILE_TYPES |"
    echo "| **Dependencies** | ${DEPS_CHANGED:+✅ Modified} ${DEPS_CHANGED:-❌ No changes} |"
    echo "| **Research** | ${TAVILY_AVAILABLE:+✅ Available} ${TAVILY_AVAILABLE:-❌ Unavailable} |"
    echo "| **AI Analysis** | ${GPT5_AVAILABLE:+✅ Available} ${GPT5_AVAILABLE:-❌ Unavailable} |"
    echo ""
    
    # Recommendations
    echo "### 💡 General Recommendations"
    echo ""
    
    if [ "$TOTAL_CHANGES" -gt 500 ]; then
        echo "- 📏 Consider splitting large PRs (current: $TOTAL_CHANGES lines)"
    fi
    
    if [ -n "$DEPS_CHANGED" ]; then
        echo "- 🔒 Review dependency changes for security implications"
        echo "- 📊 Run security scans on new dependencies"
    fi
    
    echo "- ✅ Ensure all tests pass before merging"
    echo "- 👀 Request manual review for critical changes"
    echo ""
}

# Post review comment
post_review() {
    if [ $# -eq 0 ]; then
        log_warning "No content to post"
        return 1
    fi
    
    local review_file="$1"
    
    log_info "Posting AI review to PR #$PR_NUMBER"
    
    # Add header and footer
    {
        echo "# 🤖 AI-Powered Code Review"
        echo ""
        echo "*Automated review for PR #$PR_NUMBER*"
        echo ""
        cat "$review_file"
        echo ""
        echo "---"
        echo "*🤖 Generated by AI PR Review Bot | $(date '+%Y-%m-%d %H:%M UTC')*"
    } > final_review.md
    
    # Post comment using gh CLI
    if gh pr comment "$PR_NUMBER" --body-file final_review.md >/dev/null 2>&1; then
        log_success "Review posted successfully!"
    else
        log_error "Failed to post review. Using fallback output:"
        echo ""
        cat final_review.md
    fi
}

# Main execution
main() {
    log_info "🤖 Starting AI-Powered PR Review"
    echo ""
    
    check_dependencies
    get_pr_info "$@"
    analyze_changes
    
    # Generate review in temp file
    REVIEW_FILE=$(mktemp)
    {
        research_security
        analyze_code  
        generate_summary
    } > "$REVIEW_FILE"
    
    # Post review
    post_review "$REVIEW_FILE"
    
    # Cleanup
    rm -f changed_files.txt code_diff.txt analysis_prompt.txt final_review.md "$REVIEW_FILE"
    
    log_success "AI review completed!"
}

# Run if called directly
if [ "${BASH_SOURCE[0]}" = "${0}" ]; then
    main "$@"
fi