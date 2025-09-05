#!/bin/bash
# Multi-Agent Issue Resolver Runner
# Usage: ./run_resolver.sh <issue_number>

set -e

ISSUE_NUMBER=$1
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MAIN_DIR="$(dirname "$SCRIPT_DIR")"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}🤖 Multi-Agent Issue Resolver${NC}"
echo -e "${GREEN}================================${NC}"

# Validate input
if [ -z "$ISSUE_NUMBER" ]; then
    echo -e "${RED}Error: Please provide an issue number${NC}"
    echo "Usage: $0 <issue_number>"
    exit 1
fi

# Check prerequisites
echo -e "${YELLOW}Checking prerequisites...${NC}"

# Check if we're in a git repository
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    echo -e "${RED}Error: Not in a git repository${NC}"
    exit 1
fi

# Check if gh CLI is installed
if ! command -v gh &> /dev/null; then
    echo -e "${RED}Error: GitHub CLI (gh) is not installed${NC}"
    echo "Install from: https://cli.github.com/"
    exit 1
fi

# Check if Python 3 is installed
if ! command -v python3 &> /dev/null; then
    echo -e "${RED}Error: Python 3 is not installed${NC}"
    exit 1
fi

# Check if GPT-5 and Tavily are available
if ! command -v gpt5 &> /dev/null; then
    echo -e "${YELLOW}Warning: gpt5 command not found${NC}"
    echo "The automation may not work properly without it"
fi

if ! command -v tavily &> /dev/null; then
    echo -e "${YELLOW}Warning: tavily command not found${NC}"
    echo "The automation may not work properly without it"
fi

# Ensure we're on main branch and up to date
echo -e "${YELLOW}Ensuring main branch is up to date...${NC}"
git checkout main
git pull origin main

# Check if issue exists and is open
echo -e "${YELLOW}Checking issue #$ISSUE_NUMBER...${NC}"
ISSUE_STATE=$(gh issue view $ISSUE_NUMBER --json state -q .state)

if [ "$ISSUE_STATE" != "OPEN" ]; then
    echo -e "${RED}Error: Issue #$ISSUE_NUMBER is not open (state: $ISSUE_STATE)${NC}"
    exit 1
fi

# Display issue details
echo -e "${GREEN}Issue Details:${NC}"
gh issue view $ISSUE_NUMBER

# Confirm before proceeding
echo -e "\n${YELLOW}This will automatically:${NC}"
echo "  1. Analyze the issue and create an implementation plan"
echo "  2. Research best practices using Tavily"
echo "  3. Implement the solution following SOLID/DRY/KISS/YAGNI"
echo "  4. Run 3 rounds of review (quality, testing, principles)"
echo "  5. Create a PR and auto-merge if all reviews pass"
echo ""
read -p "Proceed with automated resolution? (y/n) " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Aborted by user${NC}"
    exit 0
fi

# Create a backup branch just in case
BACKUP_BRANCH="backup-$(date +%Y%m%d-%H%M%S)"
git branch $BACKUP_BRANCH
echo -e "${GREEN}Created backup branch: $BACKUP_BRANCH${NC}"

# Run the multi-agent resolver
echo -e "\n${GREEN}Starting Multi-Agent Resolution...${NC}"
echo "========================================"

cd "$SCRIPT_DIR"

# Set up Python environment if needed
if [ ! -d "venv" ]; then
    echo -e "${YELLOW}Creating Python virtual environment...${NC}"
    python3 -m venv venv
    source venv/bin/activate
    pip install pyyaml aiofiles
else
    source venv/bin/activate
fi

# Run the orchestrator
python3 issue_resolver.py $ISSUE_NUMBER

EXIT_CODE=$?

# Deactivate virtual environment
deactivate

# Check result
if [ $EXIT_CODE -eq 0 ]; then
    echo -e "\n${GREEN}✅ Issue #$ISSUE_NUMBER resolved successfully!${NC}"
    echo -e "${GREEN}Check the PR for the automated changes.${NC}"
    
    # Show the created PR
    echo -e "\n${GREEN}Created PR:${NC}"
    gh pr list --author "@me" --limit 1
else
    echo -e "\n${RED}❌ Failed to resolve issue #$ISSUE_NUMBER${NC}"
    echo -e "${YELLOW}The branch with attempted changes has been preserved.${NC}"
    echo -e "${YELLOW}You can review the changes and complete manually.${NC}"
    
    # Show current branch
    CURRENT_BRANCH=$(git branch --show-current)
    echo -e "\n${YELLOW}Current branch: $CURRENT_BRANCH${NC}"
    echo -e "${YELLOW}To return to main: git checkout main${NC}"
fi

echo -e "\n${GREEN}Backup branch available at: $BACKUP_BRANCH${NC}"
echo -e "${YELLOW}To restore: git checkout $BACKUP_BRANCH${NC}"

exit $EXIT_CODE