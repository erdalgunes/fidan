# Pull Request Scope Guidelines

## PR Size Limits

To maintain code quality and ensure efficient reviews, we enforce the following PR size limits:

### Hard Limits (PR will be blocked)
- **Maximum changes**: 1,500 lines (additions + deletions)
- **Maximum files**: 20 files changed

### Warning Thresholds
- **Warning at**: 1,000 lines or 15 files
- **Ideal size**: < 500 lines, < 10 files

## Size Categories

PRs are automatically labeled based on total changes:

| Label | Lines Changed | Review Time | Description |
|-------|--------------|-------------|-------------|
| `size/XS` | < 50 | 5-10 min | Tiny changes, typos, config updates |
| `size/S` | 50-199 | 10-20 min | Small features or bug fixes |
| `size/M` | 200-499 | 20-40 min | Medium features, refactoring |
| `size/L` | 500-999 | 40-60 min | Large features (consider splitting) |
| `size/XL` | 1000-1499 | 1-2 hours | Very large (should be split) |
| `size/XXL` | 1500+ | Blocked | Too large, must be split |

## How to Split Large PRs

### 1. By Feature Layer
Split vertically through the application stack:
- PR 1: Backend/API changes
- PR 2: Service/business logic layer
- PR 3: UI/frontend changes
- PR 4: Tests and documentation

### 2. By Functionality
Split horizontally by feature:
- PR 1: Core functionality
- PR 2: Enhanced features
- PR 3: Edge cases and error handling
- PR 4: Performance optimizations

### 3. By File Type
Group similar changes:
- PR 1: Model/data changes
- PR 2: UI components
- PR 3: Configuration and setup
- PR 4: Tests

## Best Practices

### ✅ DO
- Keep PRs focused on a single issue or feature
- Include tests with implementation
- Update documentation in the same PR
- Use feature flags for incremental rollout
- Create draft PRs early for feedback

### ❌ DON'T
- Mix unrelated changes in one PR
- Include auto-generated files (unless necessary)
- Refactor while adding features (separate PRs)
- Bundle multiple bug fixes together
- Include debugging code or console logs

## Examples

### Good PR Scope
```
feat: Add user authentication
- 8 files changed
- 350 lines added, 50 deleted
- Includes implementation, tests, and docs
- Single, clear purpose
```

### Bad PR Scope
```
feat: Multiple improvements
- 45 files changed
- 3000 lines added, 500 deleted
- Mixes authentication, UI updates, bug fixes
- Multiple unrelated changes
```

## Exceptions

Large PRs may be acceptable for:
- Initial project setup
- Major dependency updates (with justification)
- Generated code (should be marked as such)
- Large-scale automated refactoring (with separate review)

In these cases, add a comment explaining why the PR couldn't be split.

## Enforcement

The PR size check workflow will:
1. Analyze every PR automatically
2. Add size labels for visibility
3. Post analysis comments with recommendations
4. Block PRs exceeding hard limits
5. Warn about PRs approaching limits

This helps maintain code quality, review efficiency, and project velocity.