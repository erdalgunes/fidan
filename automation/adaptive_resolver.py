#!/usr/bin/env python3
"""
ADAPTIVE Universal Issue Resolver
Intelligently analyzes any issue and generates appropriate solutions
Uses extensive research and reasoning
"""

import subprocess
import json
import sys
import re
from pathlib import Path
from typing import Dict, List, Tuple

class AdaptiveResolver:
    """Universal resolver that adapts to any issue type"""
    
    def __init__(self, issue_number: int):
        self.issue_number = issue_number
        self.issue = None
        self.affected_files = []
        self.implementation_strategy = None
        
    def run(self, cmd: str) -> str:
        """Execute command and return output"""
        result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
        return result.stdout if result.returncode == 0 else ""
    
    def analyze_issue(self) -> bool:
        """Deep analysis of the issue to understand what needs to be done"""
        print(f"\n📋 Analyzing issue #{self.issue_number}...")
        
        # Get issue details
        data = self.run(f"gh issue view {self.issue_number} --json title,body,labels")
        if not data:
            print("❌ Failed to fetch issue")
            return False
        
        self.issue = json.loads(data)
        print(f"  Title: {self.issue['title']}")
        print(f"  Type: {', '.join([l['name'] for l in self.issue.get('labels', [])])}")
        
        # Use GPT-5 to understand the issue deeply
        analysis_prompt = f"""Analyze this GitHub issue and determine:
1. What type of change is needed (bug fix, feature, enhancement)
2. Which files likely need modification
3. What technical approach to use

Issue: {self.issue['title']}
Description: {self.issue['body']}

Provide a structured analysis."""
        
        analysis = self.run(f'gpt5 "{analysis_prompt}" --effort high')
        print(f"\n📊 Analysis complete")
        
        # Identify affected files
        self.identify_affected_files()
        
        return True
    
    def identify_affected_files(self):
        """Intelligently identify which files need modification"""
        print("\n🔍 Identifying affected files...")
        
        # Extract keywords from issue
        keywords = self.extract_keywords()
        
        # Search codebase for relevant files
        for keyword in keywords:
            # Search for files containing the keyword
            search_result = self.run(f'grep -r "{keyword}" android-app --include="*.kt" -l 2>/dev/null | head -5')
            if search_result:
                files = search_result.strip().split('\n')
                self.affected_files.extend(files)
        
        # Use GPT-5 to determine likely files based on issue type
        file_prompt = f"""Given this issue: {self.issue['title']}
And these keywords: {keywords}

What files in an Android Kotlin app would typically need modification?
Consider standard Android architecture.

List only the likely filenames/paths."""
        
        suggested_files = self.run(f'gpt5 "{file_prompt}" --effort medium')
        
        # Deduplicate
        self.affected_files = list(set(self.affected_files))
        
        if self.affected_files:
            print(f"  Found {len(self.affected_files)} potentially affected files")
        else:
            print("  Will determine files based on issue context")
    
    def extract_keywords(self) -> List[str]:
        """Extract technical keywords from issue"""
        text = f"{self.issue['title']} {self.issue['body']}"
        
        # Common technical terms to look for
        patterns = [
            r'Timer\w*', r'Screen\w*', r'Session\w*', r'Forest\w*',
            r'Settings\w*', r'Notification\w*', r'MainActivity',
            r'wake.?lock', r'FLAG_KEEP_SCREEN_ON'
        ]
        
        keywords = []
        for pattern in patterns:
            matches = re.findall(pattern, text, re.IGNORECASE)
            keywords.extend(matches)
        
        return list(set(keywords))
    
    def research_solution(self) -> str:
        """Extensive research for the best solution"""
        print("\n🔬 Researching best practices...")
        
        # Create targeted search queries
        search_queries = []
        
        # Clean issue title for search
        clean_title = self.issue['title'].replace('Fix:', '').replace('Feature:', '').strip()
        
        # Based on issue content
        search_queries.append(f"Android {clean_title} implementation")
        
        # Based on technical keywords
        keywords = self.extract_keywords()
        if keywords:
            search_queries.append(f"Android {' '.join(keywords[:2])} best practice")
        
        # Specific to issue type
        if 'screen' in clean_title.lower() and 'timeout' in clean_title.lower():
            search_queries.append("Android FLAG_KEEP_SCREEN_ON timer app")
        elif 'timer' in clean_title.lower():
            search_queries.append("Android timer persistence SharedPreferences")
        
        # Perform searches
        all_research = []
        for query in search_queries[:2]:  # Limit to 2 searches
            print(f"  Searching: {query[:50]}...")
            result = self.run(f'tavily "{query}" --max-results 2')
            if result:
                all_research.append(result)
        
        # Synthesize research with GPT-5
        synthesis_prompt = f"""Based on this research for fixing: {self.issue['title']}

Research findings:
{' '.join(all_research[:2000])}

Synthesize the key implementation insights.
Focus on practical, working solutions."""
        
        synthesis = self.run(f'gpt5 "{synthesis_prompt}" --effort high')
        
        return synthesis
    
    def generate_implementation(self, research: str) -> str:
        """Generate the actual implementation"""
        print("\n💻 Generating implementation...")
        
        # Read existing code if files identified
        existing_code = ""
        if self.affected_files:
            for file in self.affected_files[:2]:  # Read max 2 files
                if Path(file).exists():
                    content = Path(file).read_text()
                    existing_code += f"\n\n=== Current {file} ===\n{content[:1000]}"
        
        # Generate implementation
        implementation_prompt = f"""Generate code to fix this issue:

ISSUE: {self.issue['title']}
DESCRIPTION: {self.issue['body']}

RESEARCH INSIGHTS:
{research[:1500]}

EXISTING CODE CONTEXT:
{existing_code[:1500]}

REQUIREMENTS:
- Generate ONLY the essential code changes
- Follow Android/Kotlin best practices
- Maintain existing code style
- Include proper error handling
- Follow KISS principle

OUTPUT FORMAT:
For each file that needs changes:
=== FILE: path/to/file.kt ===
// Complete updated code or specific methods to add/modify
<actual code here>
===

Generate working code only, no explanations."""
        
        implementation = self.run(f'gpt5 "{implementation_prompt}" --effort max --max-tokens 4000')
        
        return implementation
    
    def apply_implementation(self, implementation: str, dry_run: bool = True) -> bool:
        """Parse and apply the generated implementation"""
        print("\n📝 Applying implementation...")
        
        if not implementation:
            print("❌ No implementation generated")
            return False
        
        # Parse the implementation
        files_to_modify = {}
        current_file = None
        current_content = []
        
        for line in implementation.split('\n'):
            if '=== FILE:' in line:
                # Save previous file
                if current_file and current_content:
                    files_to_modify[current_file] = '\n'.join(current_content)
                
                # Extract filename
                match = re.search(r'FILE:\s*([\w/\-\.]+)', line)
                if match:
                    current_file = match.group(1).strip()
                    current_content = []
            elif '===' in line and current_file:
                # End of file marker
                if current_content:
                    files_to_modify[current_file] = '\n'.join(current_content)
                current_file = None
                current_content = []
            elif current_file:
                current_content.append(line)
        
        # Save last file if any
        if current_file and current_content:
            files_to_modify[current_file] = '\n'.join(current_content)
        
        # Apply changes
        for filepath, new_content in files_to_modify.items():
            # Determine full path
            if not filepath.startswith('/'):
                # Try to find the file
                if 'MainActivity' in filepath:
                    full_path = Path('android-app/app/src/main/java/com/erdalgunes/fidan/MainActivity.kt')
                elif 'TimerScreen' in filepath:
                    full_path = Path('android-app/app/src/main/java/com/erdalgunes/fidan/ui/screens/TimerScreen.kt')
                elif 'TimerManager' in filepath:
                    full_path = Path('android-app/app/src/main/java/com/erdalgunes/fidan/TimerManager.kt')
                else:
                    # Try to find it
                    search = self.run(f'find android-app -name "{Path(filepath).name}" -type f 2>/dev/null | head -1')
                    if search:
                        full_path = Path(search.strip())
                    else:
                        full_path = Path(f'android-app/{filepath}')
            else:
                full_path = Path(filepath)
            
            if dry_run:
                print(f"  [DRY RUN] Would modify: {full_path}")
                print(f"    Preview: {new_content[:150]}...")
            else:
                # Create parent directories if needed
                full_path.parent.mkdir(parents=True, exist_ok=True)
                
                # For existing files, we'll create a .patch file first
                if full_path.exists():
                    patch_path = full_path.with_suffix('.kt.patch')
                    patch_path.write_text(new_content)
                    print(f"  ✓ Created patch: {patch_path}")
                    
                    # Optionally apply the patch (careful approach)
                    # For now, just save as patch for manual review
                else:
                    # New file
                    full_path.write_text(new_content)
                    print(f"  ✓ Created: {full_path}")
        
        return len(files_to_modify) > 0
    
    def create_pr(self, dry_run: bool = True) -> str:
        """Create pull request"""
        if dry_run:
            print("\n🔀 [DRY RUN] Would create PR")
            return "DRY_RUN_PR"
        
        branch = f"auto-fix-{self.issue_number}"
        
        # Create and switch to branch
        self.run(f"git checkout -b {branch}")
        
        # Add all patches and new files
        self.run("git add -A")
        
        # Commit
        commit_msg = f"fix: {self.issue['title']} (closes #{self.issue_number})\n\nAutomated fix using adaptive resolver"
        self.run(f'git commit -m "{commit_msg}"')
        
        # Push
        self.run(f"git push -u origin {branch}")
        
        # Create PR
        pr_body = f"Automated fix for #{self.issue_number}\n\nGenerated by adaptive resolver with research and reasoning."
        pr_url = self.run(f'gh pr create --title "Auto: {self.issue["title"]}" --body "{pr_body}"')
        
        return pr_url
    
    def resolve(self, dry_run: bool = True) -> bool:
        """Main resolution workflow"""
        print(f"\n{'='*60}")
        print(f"🤖 ADAPTIVE RESOLVER - Issue #{self.issue_number}")
        print(f"{'='*60}")
        
        # Step 1: Analyze
        if not self.analyze_issue():
            return False
        
        # Step 2: Research
        research = self.research_solution()
        if not research:
            print("❌ Research failed")
            return False
        print(f"  ✓ Research complete ({len(research)} chars)")
        
        # Step 3: Generate
        implementation = self.generate_implementation(research)
        if not implementation:
            print("❌ Implementation generation failed")
            return False
        print(f"  ✓ Implementation generated ({len(implementation)} chars)")
        
        # Step 4: Apply
        if not self.apply_implementation(implementation, dry_run):
            print("❌ Failed to apply implementation")
            return False
        
        # Step 5: Create PR
        pr_url = self.create_pr(dry_run)
        if pr_url:
            print(f"\n✅ Resolution complete!")
            if not dry_run:
                print(f"  PR: {pr_url}")
        
        return True

def main():
    """Main entry point"""
    if len(sys.argv) < 2:
        print("Usage: python adaptive_resolver.py <issue_number> [--real]")
        print("\nExamples:")
        print("  python adaptive_resolver.py 77        # Dry run")
        print("  python adaptive_resolver.py 77 --real # Actually apply changes")
        sys.exit(1)
    
    issue_number = int(sys.argv[1])
    dry_run = "--real" not in sys.argv
    
    if dry_run:
        print("🔒 DRY RUN MODE - No actual changes")
    else:
        print("⚠️  REAL MODE - Will create actual changes")
        response = input("Continue? (y/n): ")
        if response.lower() != 'y':
            print("Aborted")
            sys.exit(0)
    
    resolver = AdaptiveResolver(issue_number)
    success = resolver.resolve(dry_run)
    
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()