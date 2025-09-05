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

### GitHub Action (Automatic)

1. The bot is already configured in `.github/workflows/ai-pr-review.yml`
2. Triggers automatically on PR open/update events
3. Posts reviews as PR comments
4. Updates existing comments instead of creating duplicates

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