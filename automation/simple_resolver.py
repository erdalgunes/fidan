#!/usr/bin/env python3
"""
MINIMAL Autonomous GitHub Issue Resolver
Following KISS, YAGNI, DRY, SOLID principles
"""

import subprocess
import json
import sys
import os
from pathlib import Path

def run(cmd):
    """DRY: Single function for all shell commands"""
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    return result.stdout if result.returncode == 0 else None

def get_issue(number):
    """Single Responsibility: Get issue details"""
    data = run(f"gh issue view {number} --json title,body,labels")
    return json.loads(data) if data else None

def research_solution(issue):
    """Single Responsibility: Research best practices"""
    # Search for relevant solutions
    search_query = f"{issue['title']} Android Kotlin implementation best practices"
    research = run(f'tavily "{search_query}" --depth advanced --max-results 3')
    
    # Synthesize with GPT-5
    prompt = f"""Given this issue:
Title: {issue['title']}
Body: {issue['body']}

And this research:
{research[:1000] if research else 'No research available'}

Provide a SIMPLE implementation approach following KISS/YAGNI.
Just the essential steps, no over-engineering."""
    
    plan = run(f'gpt5 "{prompt}" --effort medium')
    return plan

def generate_fix(issue, plan):
    """Single Responsibility: Generate code fix"""
    prompt = f"""Implement fix for: {issue['title']}

Plan: {plan[:500]}

Generate ONLY the essential code changes needed.
Follow existing patterns in Android/Kotlin.
Output format:
FILE: path/to/file
```
code here
```"""
    
    implementation = run(f'gpt5 "{prompt}" --effort high')
    return implementation

def apply_changes(implementation):
    """Single Responsibility: Apply code changes"""
    if not implementation:
        return False
    
    # Parse implementation (simplified)
    lines = implementation.split('\n')
    current_file = None
    code_lines = []
    in_code = False
    
    for line in lines:
        if line.startswith('FILE:'):
            # Save previous file if exists
            if current_file and code_lines:
                filepath = Path(current_file)
                filepath.parent.mkdir(parents=True, exist_ok=True)
                filepath.write_text('\n'.join(code_lines))
                run(f"git add {current_file}")
            
            current_file = line.replace('FILE:', '').strip()
            code_lines = []
            in_code = False
        elif '```' in line:
            in_code = not in_code
        elif in_code and current_file:
            code_lines.append(line)
    
    # Save last file
    if current_file and code_lines:
        filepath = Path(current_file)
        filepath.parent.mkdir(parents=True, exist_ok=True)
        filepath.write_text('\n'.join(code_lines))
        run(f"git add {current_file}")
    
    return True

def create_pr(issue_number, issue_title):
    """Single Responsibility: Create pull request"""
    branch = f"auto-fix-{issue_number}"
    
    # Create branch
    run(f"git checkout -b {branch}")
    
    # Commit
    commit_msg = f"fix: Resolve issue #{issue_number}\\n\\n{issue_title}\\n\\nAutomated fix"
    run(f'git commit -m "{commit_msg}"')
    
    # Push
    run(f"git push -u origin {branch}")
    
    # Create PR
    pr_body = f"Automated fix for #{issue_number}\\n\\nThis PR was generated automatically."
    result = run(f'gh pr create --title "Auto: Fix issue #{issue_number}" --body "{pr_body}"')
    
    return result

def main():
    """Main orchestration - KISS principle"""
    if len(sys.argv) != 2:
        print("Usage: python simple_resolver.py <issue_number>")
        sys.exit(1)
    
    issue_number = sys.argv[1]
    
    # Step 1: Get issue
    print(f"📋 Getting issue #{issue_number}...")
    issue = get_issue(issue_number)
    if not issue:
        print("❌ Could not fetch issue")
        sys.exit(1)
    
    # Step 2: Research
    print("🔍 Researching solution...")
    plan = research_solution(issue)
    if not plan:
        print("❌ Could not research solution")
        sys.exit(1)
    
    # Step 3: Generate fix
    print("💻 Generating fix...")
    implementation = generate_fix(issue, plan)
    if not implementation:
        print("❌ Could not generate fix")
        sys.exit(1)
    
    # Step 4: Apply changes
    print("📝 Applying changes...")
    if not apply_changes(implementation):
        print("❌ Could not apply changes")
        sys.exit(1)
    
    # Step 5: Create PR
    print("🔀 Creating pull request...")
    pr_url = create_pr(issue_number, issue['title'])
    
    if pr_url:
        print(f"✅ PR created: {pr_url}")
    else:
        print("❌ Could not create PR")
        sys.exit(1)

if __name__ == "__main__":
    main()