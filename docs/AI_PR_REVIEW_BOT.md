# 🤖 AI-Powered PR Review Bot

An intelligent code review bot that combines Tavily web research with GPT-5 analysis to provide comprehensive PR feedback.

## Features

### 🔍 **Research-Driven Reviews**
- **Security Analysis**: Uses Tavily to research known vulnerabilities in dependencies
- **Best Practices**: Searches for current industry standards and patterns
- **Real-time Updates**: Always uses the latest security and best practice information

### 🧠 **AI Code Analysis**
- **Bug Detection**: GPT-5 analyzes code for potential issues and logic errors
- **Performance Review**: Identifies performance bottlenecks and optimization opportunities  
- **Architecture Feedback**: Provides high-level design and structure recommendations
- **Code Quality**: Suggests improvements for readability and maintainability

### 💰 **Cost-Effective Design**
- **Smart Limits**: Skips review for PRs > 2000 lines to control costs
- **Efficient Processing**: Truncates large diffs while preserving key information
- **Batch Operations**: Analyzes multiple aspects in single API calls

### 🛡️ **Error Handling & Reliability**
- **Graceful Degradation**: Works even when some tools are unavailable
- **Fallback Modes**: Provides partial reviews if full analysis fails
- **Cost Controls**: Built-in safeguards prevent runaway API usage

## Engineering Principles

### DRY (Don't Repeat Yourself)
- Reuses existing CLI tools (`tavily`, `gpt5`) instead of custom implementations
- Shared logic between GitHub Action and standalone script
- Template-based review generation

### KISS (Keep It Simple, Stupid)
- Single workflow file for GitHub integration
- Bash script for local usage - no complex frameworks
- Clear, linear execution flow

### YAGNI (You Aren't Gonna Need It)
- Starts with essential features only
- Modular design allows adding capabilities later
- No over-engineering or premature optimization

### SOLID Principles
- **Single Responsibility**: Each function has one clear purpose
- **Open/Closed**: Easy to extend with new analysis types
- **Dependency Inversion**: Abstracts away specific AI/research tools

## Setup & Usage

### GitHub Action (Notification-Friendly)

The bot is designed to minimize email notifications:

1. **Triggers only on:** PR opened/reopened (not every push)
2. **Manual triggering:** Comment `/ai-review` on any PR to run analysis
3. **Smart updates:** Only updates comments when content meaningfully changes
4. **Easy disable:** Add `skip-ai-review` label to any PR to disable bot

**Trigger Options:**
- Automatic: New PRs only (reduces notification spam)
- Manual: Comment `/ai-review` on any PR
- Label control: Add `skip-ai-review` label to disable

#### Configuration Options:

```yaml
# Customize in the workflow file
MAX_CHANGES: 2000        # Skip analysis for larger PRs  
MAX_DIFF_SIZE: 50000     # Truncate large code diffs
SKIP_DRAFTS: true        # Don't analyze draft PRs
```

### Local Usage

```bash
# Review a specific PR
./scripts/ai-pr-reviewer.sh 123

# Review against different base branch
./scripts/ai-pr-reviewer.sh 123 develop

# Preview without posting (stdout output)
./scripts/ai-pr-reviewer.sh 123 2>/dev/null
```

### Prerequisites

**Required:**
- `git` - For analyzing code changes
- `gh` - For GitHub API interactions

**Optional (but recommended):**
- `tavily` - For security and best practices research
- `gpt5` - For AI-powered code analysis

The bot gracefully handles missing optional tools.

### 🔐 Private Repository Access Setup

**Current Status:** The bot can see the repositories exist:
- https://github.com/erdalgunes/tavily-cli ✅ 
- https://github.com/erdalgunes/gpt5-cli ✅

**Issue:** GitHub Actions token cannot access private repositories outside the current repository context.

**Solution Options:**

**Option 1: Personal Access Token (Recommended)**
1. Create PAT: GitHub Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Generate token with `repo` scope for private repository access
3. Add repository secret: `PERSONAL_GITHUB_TOKEN` = your token
4. Bot will automatically detect and use the PAT

**Option 2: Make Repositories Public** 
- Change repository visibility to public in repository settings
- Bot will work immediately with existing GitHub Actions token

**Option 3: Alternative Implementation**
- Use public AI APIs instead of private CLI tools
- Modify bot to use OpenAI API directly

**Verification:**
- Successful: `✅ Can access erdalgunes/tavily-cli`
- Failed: `❌ Tavily repo not accessible` with clear instructions

## 📧 Notification Management

**Reduced Email Notifications:**
- Bot only runs on PR creation (not every push)
- Comment updates are skipped when no meaningful changes
- Use `/ai-review` comment for manual analysis only when needed

**Complete Disable Options:**
1. **Per PR:** Add `skip-ai-review` label to specific PRs
2. **Temporarily:** Disable the workflow file in Actions tab
3. **Permanently:** Delete `.github/workflows/ai-pr-review.yml`

**GitHub Notification Settings:**
- Go to GitHub Settings → Notifications
- Customize "Pull requests" and "Issues" notification preferences
- Consider "Web and mobile" only for bot comments

## Review Format

### Security & Research Section
```markdown
## 🔐 Security & Best Practices Research

- Known vulnerabilities in dependencies
- Industry best practices for file types
- Security recommendations
```

### Code Analysis Section  
```markdown
## 📝 Code Analysis

🐛 **Potential Issues:**
- Logic errors or edge cases
- Null pointer risks

🔒 **Security Concerns:**  
- Input validation gaps
- Authentication bypasses

⚡ **Performance:**
- Inefficient algorithms
- Resource leaks

🧹 **Code Quality:**
- Readability improvements
- Refactoring suggestions

🏗️ **Architecture:**
- Design pattern recommendations
- Structural improvements
```

### Summary Section
```markdown
## 📋 Review Summary

| Metric | Value |
|--------|--------|
| Files Changed | 12 |
| Total Changes | 234 lines |
| File Types | kt, xml, yml |
| Dependencies | ✅ Modified |

💡 **Recommendations:**
- Ensure tests pass
- Review dependency security
```

## Cost Management

### Automatic Controls
- **PR Size Limits**: Skips analysis for PRs > 2000 lines
- **Diff Truncation**: Limits code analysis to 50KB of changes
- **Draft Skipping**: Ignores draft PRs to save costs
- **Smart Batching**: Combines multiple queries efficiently

### Manual Controls
```bash
# Disable cost controls (use carefully!)
COST_CONTROL_ENABLED=false ./scripts/ai-pr-reviewer.sh 123

# Custom limits
MAX_TOTAL_CHANGES=1000 ./scripts/ai-pr-reviewer.sh 123
```

## Extending the Bot

### Adding New Research Topics

```bash
# In research_security() function
tavily "new research topic $FILE_TYPES 2024" --max-results 2
```

### Custom Analysis Prompts

```bash
# In analyze_code() function  
gpt5 "Custom analysis prompt: $(cat code_diff.txt)" --effort medium
```

### New File Type Support

```bash
# Add to diff command
git diff ... -- '*.newext' >> code_diff.txt
```

## Troubleshooting

### Common Issues

**"Failed to get PR information"**
- Run `gh auth login` to authenticate
- Ensure you're in the correct repository

**"Tavily/GPT-5 not available"**
- Check CLI tool installation
- Verify API keys are configured
- Bot will work with degraded functionality

**"Review too large"**
- Split PR into smaller chunks (< 500 lines recommended)
- Use `git diff --stat` to see change distribution

### Debug Mode

```bash
# Enable verbose logging
set -x
./scripts/ai-pr-reviewer.sh 123
```

## Performance

### Typical Processing Times
- **Small PR** (< 100 lines): ~30 seconds
- **Medium PR** (100-500 lines): ~60 seconds  
- **Large PR** (500-2000 lines): ~90 seconds
- **Oversized PR** (> 2000 lines): ~5 seconds (summary only)

### Resource Usage
- **Memory**: < 100MB during execution
- **Network**: 2-5 API calls per review
- **Storage**: Temporary files cleaned automatically

## Examples

### Dependency Update Review
```markdown
## 🔐 Security Analysis
Found: CVE-2023-12345 in lodash < 4.17.21
Recommendation: Update to lodash@4.17.21+

## 📝 Code Analysis  
✅ Dependency update looks safe
⚡ No performance impact expected
```

### Feature Addition Review
```markdown
## 📝 Code Analysis

🐛 **Potential Issues:**
- Line 45: Null check missing for user input
- Line 67: Possible race condition in async code

🔒 **Security:**
- ✅ Input validation implemented correctly
- ⚠️ Consider rate limiting for API endpoint

⚡ **Performance:**
- Line 123: Consider caching database query results
- Database connection not closed in error path
```

## Contributing

The bot follows standard contribution practices:

1. **Issues**: Use GitHub issues for bug reports and feature requests
2. **PRs**: The bot will review its own improvements! 
3. **Testing**: Test locally before submitting changes
4. **Documentation**: Update this file for new features

---

*Built with ❤️ using Tavily research and GPT-5 analysis*