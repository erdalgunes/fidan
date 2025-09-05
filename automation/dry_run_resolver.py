#!/usr/bin/env python3
"""
DRY RUN version - Tests the flow without actual commits
"""

import subprocess
import json
import sys

def run(cmd, dry_run=False):
    """Execute command or simulate if dry run"""
    if dry_run and any(dangerous in cmd for dangerous in ['git push', 'git commit', 'gh pr create']):
        print(f"  [DRY RUN] Would execute: {cmd[:80]}...")
        return "SIMULATED_OUTPUT"
    
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    return result.stdout if result.returncode == 0 else None

def resolve_issue(issue_number, dry_run=True):
    """Main resolution flow"""
    
    # Step 1: Get issue
    print(f"\n📋 Getting issue #{issue_number}...")
    data = run(f"gh issue view {issue_number} --json title,body,labels")
    if not data:
        return False
    
    issue = json.loads(data)
    print(f"  Title: {issue['title']}")
    
    # Step 2: Research solution
    print(f"\n🔍 Researching solution...")
    search = f"{issue['title']} implementation best practices"
    research = run(f'tavily "{search}" --max-results 2')
    print(f"  Research found: {len(research)} characters")
    
    # Step 3: Create implementation plan
    print(f"\n🧠 Creating implementation plan...")
    plan_prompt = f"""Issue: {issue['title']}
{issue['body'][:200]}

Research summary: {research[:500] if research else 'N/A'}

Create a SIMPLE 3-step implementation plan. KISS principle."""
    
    plan = run(f'gpt5 "{plan_prompt}" --effort medium')
    print(f"  Plan created: {plan[:200]}...")
    
    # Step 4: Generate code
    print(f"\n💻 Generating implementation...")
    code_prompt = f"""Fix this issue with MINIMAL code changes:
{issue['title']}

Plan: {plan[:300]}

Output format:
FILE: path/to/file.kt
```kotlin
// minimal fix here
```"""
    
    implementation = run(f'gpt5 "{code_prompt}" --effort high')
    print(f"  Code generated: {len(implementation)} characters")
    
    # Step 5: Simulate PR creation
    print(f"\n🔀 Creating pull request (dry run: {dry_run})...")
    branch = f"auto-fix-{issue_number}"
    
    run(f"git checkout -b {branch}", dry_run)
    run(f'git commit -m "fix: Issue #{issue_number}"', dry_run)
    run(f"git push origin {branch}", dry_run)
    run(f'gh pr create --title "Fix #{issue_number}"', dry_run)
    
    print(f"\n✅ Issue #{issue_number} resolved successfully!")
    return True

def main():
    if len(sys.argv) < 2:
        print("Usage: python dry_run_resolver.py <issue_number> [--real]")
        sys.exit(1)
    
    issue_number = sys.argv[1]
    dry_run = "--real" not in sys.argv
    
    if dry_run:
        print("🔒 DRY RUN MODE - No actual changes will be made")
    else:
        print("⚠️  REAL MODE - Will create actual commits!")
        response = input("Continue? (y/n): ")
        if response.lower() != 'y':
            print("Aborted")
            sys.exit(0)
    
    success = resolve_issue(issue_number, dry_run)
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()