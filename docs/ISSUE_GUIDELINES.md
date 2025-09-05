# Fidan Issue Guidelines

## Principles

Our issue tracker follows these software engineering principles:

### KISS (Keep It Simple, Stupid)
- Each issue addresses ONE specific problem or feature
- Clear, actionable titles (< 10 words)
- Simple acceptance criteria

### YAGNI (You Aren't Gonna Need It)
- Only create issues for immediate needs
- No speculative features
- MVP focus: timer, trees, basic tracking

### SOLID
- **S**ingle Responsibility: One issue, one purpose
- **O**pen/Closed: Issues are complete when closed
- **L**iskov Substitution: N/A for issues
- **I**nterface Segregation: Small, focused issues
- **D**ependency Inversion: Issues don't depend on implementation details

### DRY (Don't Repeat Yourself)
- No duplicate issues
- Reuse existing patterns and code
- Reference related issues when relevant

## Issue Categories

### 1. Core MVP Issues (High Priority)
- Timer persistence (#70)
- Digital Humani API setup (#74)
- GitHub Sponsors webhook (#79)

### 2. Essential Features
- Session completion notification (#71)
- Break timer (#76)
- Settings persistence (#75)

### 3. User Experience
- Tree growth animation (#73)
- Session history (#72)
- Tree count display (#78)

### 4. Bug Fixes
- Screen timeout prevention (#77)
- Timer state persistence (#70)

## Issue Template

```markdown
## Problem/User Story
[One sentence describing the need]

## Solution/Implementation
- Bullet points of specific tasks
- Keep it simple
- Reference existing patterns

## Acceptance Criteria
- [ ] Clear, testable criteria
- [ ] User-facing outcomes
- [ ] Performance requirements if applicable

## Technical Notes (if needed)
- Single responsibility focus
- Existing code to reuse
- Patterns to follow
```

## What NOT to Create Issues For

❌ Complex gamification systems
❌ Social features before MVP
❌ Multiple tree species before basic trees work
❌ Advanced analytics before basic stats
❌ Animations before functionality
❌ Features "users might want" without validation

## Priority Levels

### High Priority
- Breaks core functionality
- Blocks other work
- Essential for MVP

### Medium Priority
- Improves user experience
- Nice to have for launch

### Low Priority
- Future enhancements
- Optimizations
- Polish

## Issue Lifecycle

1. **Created**: Problem identified, solution proposed
2. **In Progress**: Someone is actively working on it
3. **Review**: Code complete, needs review
4. **Closed**: Merged and deployed

## Tips for Good Issues

✅ Can be completed in < 1 day
✅ Has clear success criteria
✅ Follows existing patterns
✅ Solves a real problem
✅ Title starts with "Fix:", "Feature:", or "Refactor:"

## Example of a Good Issue

**Title**: Fix: Timer should persist state across app restarts

**Body**:
- Clear problem statement
- Simple solution approach
- Measurable acceptance criteria
- References existing TimerManager

## Example of a Bad Issue

**Title**: Implement comprehensive gamification system with achievements, leaderboards, and social sharing

**Why it's bad**:
- Too complex (violates KISS)
- Multiple responsibilities (violates SOLID)
- Speculative features (violates YAGNI)
- Would take weeks to complete

---

Remember: **Simple issues lead to simple solutions, which lead to maintainable code.**