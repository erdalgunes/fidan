#!/usr/bin/env python3
"""
Multi-Agent GitHub Issue Resolver
Inspired by GPT-Researcher architecture
Uses LangGraph for orchestration
"""

import asyncio
import json
import subprocess
from dataclasses import dataclass
from typing import Dict, List, Optional, Tuple
from enum import Enum

class AgentState(Enum):
    """Workflow states for the multi-agent system"""
    ANALYZING = "analyzing"
    RESEARCHING = "researching"
    IMPLEMENTING = "implementing"
    REVIEWING = "reviewing"
    MERGING = "merging"
    COMPLETED = "completed"
    FAILED = "failed"

@dataclass
class IssueContext:
    """Shared context passed between agents"""
    issue_number: int
    issue_title: str
    issue_body: str
    research_findings: List[str] = None
    implementation_plan: str = ""
    code_changes: Dict[str, str] = None
    review_results: Dict[str, bool] = None
    review_feedback: Dict[str, str] = None
    branch_name: str = ""
    
    def __post_init__(self):
        if self.research_findings is None:
            self.research_findings = []
        if self.code_changes is None:
            self.code_changes = {}
        if self.review_results is None:
            self.review_results = {}
        if self.review_feedback is None:
            self.review_feedback = {}

class BaseAgent:
    """Base class for all agents"""
    
    def __init__(self, name: str):
        self.name = name
    
    async def execute(self, context: IssueContext) -> IssueContext:
        """Execute agent logic"""
        raise NotImplementedError
    
    def run_command(self, command: str) -> str:
        """Execute shell command and return output"""
        result = subprocess.run(command, shell=True, capture_output=True, text=True)
        return result.stdout if result.returncode == 0 else result.stderr

class IssueAnalyzerAgent(BaseAgent):
    """Analyzes GitHub issue and creates implementation plan"""
    
    async def execute(self, context: IssueContext) -> IssueContext:
        print(f"🔍 {self.name}: Analyzing issue #{context.issue_number}")
        
        # Use GPT-5 to analyze the issue
        analysis_prompt = f"""
        Analyze GitHub issue #{context.issue_number}: {context.issue_title}
        
        Issue Description:
        {context.issue_body}
        
        Create an implementation plan following:
        - SOLID principles
        - DRY (Don't Repeat Yourself)
        - KISS (Keep It Simple)
        - YAGNI (You Aren't Gonna Need It)
        
        Output a step-by-step implementation plan.
        """
        
        plan = self.run_command(f'gpt5 "{analysis_prompt}" --effort medium')
        context.implementation_plan = plan
        
        # Create feature branch
        context.branch_name = f"auto-issue-{context.issue_number}"
        self.run_command(f"git checkout -b {context.branch_name}")
        
        return context

class ResearchAgent(BaseAgent):
    """Researches best practices and solutions using Tavily"""
    
    async def execute(self, context: IssueContext) -> IssueContext:
        print(f"🔬 {self.name}: Researching best practices")
        
        # Extract key topics from issue
        topics = self.extract_topics(context)
        
        # Research each topic
        for topic in topics:
            search_query = f"{topic} best practices implementation Android Kotlin 2024"
            result = self.run_command(
                f'tavily "{search_query}" --depth advanced --max-results 3'
            )
            context.research_findings.append(result)
        
        # Synthesize findings with GPT-5
        synthesis_prompt = f"""
        Based on these research findings for issue #{context.issue_number}:
        {' '.join(context.research_findings)}
        
        Provide specific implementation recommendations following SOLID/DRY/KISS/YAGNI.
        """
        
        synthesis = self.run_command(f'gpt5 "{synthesis_prompt}" --effort high')
        context.research_findings.append(synthesis)
        
        return context
    
    def extract_topics(self, context: IssueContext) -> List[str]:
        """Extract key topics from issue for research"""
        # Simple keyword extraction - could be enhanced
        keywords = []
        if "timer" in context.issue_title.lower():
            keywords.append("Android timer persistence CountDownTimer")
        if "notification" in context.issue_title.lower():
            keywords.append("Android notification NotificationCompat")
        if "api" in context.issue_title.lower():
            keywords.append("Retrofit API client error handling")
        return keywords if keywords else ["Android Kotlin Jetpack Compose"]

class ImplementationAgent(BaseAgent):
    """Implements the solution based on research and plan"""
    
    async def execute(self, context: IssueContext) -> IssueContext:
        print(f"💻 {self.name}: Implementing solution")
        
        # Generate implementation based on plan and research
        implementation_prompt = f"""
        Implement solution for issue #{context.issue_number}:
        
        Plan:
        {context.implementation_plan}
        
        Research Findings:
        {' '.join(context.research_findings[-2:])}  # Last 2 findings
        
        Generate ONLY the code changes needed. Follow:
        - Existing patterns in the codebase
        - SOLID/DRY/KISS/YAGNI principles
        - Kotlin/Jetpack Compose best practices
        
        Output format:
        FILE: path/to/file.kt
        CODE:
        ```kotlin
        // actual code here
        ```
        """
        
        implementation = self.run_command(f'gpt5 "{implementation_prompt}" --effort high')
        
        # Parse and apply changes
        self.apply_code_changes(implementation, context)
        
        return context
    
    def apply_code_changes(self, implementation: str, context: IssueContext):
        """Parse GPT-5 output and apply code changes"""
        # This is simplified - real implementation would parse properly
        lines = implementation.split('\n')
        current_file = None
        code_buffer = []
        in_code_block = False
        
        for line in lines:
            if line.startswith("FILE:"):
                if current_file and code_buffer:
                    # Save previous file
                    context.code_changes[current_file] = '\n'.join(code_buffer)
                current_file = line.replace("FILE:", "").strip()
                code_buffer = []
            elif line.startswith("```"):
                in_code_block = not in_code_block
            elif in_code_block and current_file:
                code_buffer.append(line)
        
        # Save last file
        if current_file and code_buffer:
            context.code_changes[current_file] = '\n'.join(code_buffer)
        
        # Apply changes to files
        for filepath, code in context.code_changes.items():
            # Read existing file if it exists
            try:
                with open(filepath, 'r') as f:
                    existing = f.read()
                # For now, we'll append or create - real implementation would be smarter
            except FileNotFoundError:
                existing = ""
            
            with open(filepath, 'w') as f:
                f.write(code)
            
            self.run_command(f"git add {filepath}")

class CodeQualityReviewer(BaseAgent):
    """Reviews code quality and standards"""
    
    async def execute(self, context: IssueContext) -> IssueContext:
        print(f"✅ {self.name}: Reviewing code quality")
        
        # Get diff of changes
        diff = self.run_command("git diff --cached")
        
        review_prompt = f"""
        Review this code for quality:
        
        {diff}
        
        Check for:
        - Code clarity and readability
        - Proper error handling
        - Memory leaks or performance issues
        - Following Kotlin conventions
        
        Respond with:
        APPROVED: if code quality is good
        CHANGES_NEEDED: if improvements needed
        
        Provide specific feedback.
        """
        
        review = self.run_command(f'gpt5 "{review_prompt}" --effort medium')
        
        context.review_results["code_quality"] = "APPROVED" in review
        context.review_feedback["code_quality"] = review
        
        return context

class TestingReviewer(BaseAgent):
    """Reviews test coverage and quality"""
    
    async def execute(self, context: IssueContext) -> IssueContext:
        print(f"🧪 {self.name}: Reviewing test coverage")
        
        # Check if tests were added
        diff = self.run_command("git diff --cached --name-only")
        has_tests = "test" in diff.lower()
        
        review_prompt = f"""
        Review testing approach for issue #{context.issue_number}:
        
        Changes made:
        {self.run_command("git diff --cached --stat")}
        
        Verify:
        - Are tests needed for these changes?
        - Is test coverage adequate?
        - Are edge cases covered?
        
        Current test status: {'Tests found' if has_tests else 'No tests found'}
        
        Respond with APPROVED or CHANGES_NEEDED.
        """
        
        review = self.run_command(f'gpt5 "{review_prompt}" --effort medium')
        
        context.review_results["testing"] = "APPROVED" in review
        context.review_feedback["testing"] = review
        
        # Run existing tests
        test_result = self.run_command("cd android-app && ./gradlew test")
        if "BUILD SUCCESSFUL" not in test_result:
            context.review_results["testing"] = False
            context.review_feedback["testing"] += f"\nTests failed: {test_result}"
        
        return context

class PrinciplesReviewer(BaseAgent):
    """Reviews adherence to SOLID/DRY/KISS/YAGNI principles"""
    
    async def execute(self, context: IssueContext) -> IssueContext:
        print(f"📐 {self.name}: Reviewing design principles")
        
        diff = self.run_command("git diff --cached")
        
        review_prompt = f"""
        Review this code for SOLID/DRY/KISS/YAGNI principles:
        
        {diff}
        
        Check for:
        - Single Responsibility: Does each class/function do one thing?
        - DRY: Is there code duplication?
        - KISS: Is the solution unnecessarily complex?
        - YAGNI: Are we adding features not in requirements?
        
        Issue requirements:
        {context.issue_body}
        
        Respond with APPROVED or CHANGES_NEEDED with specific feedback.
        """
        
        review = self.run_command(f'gpt5 "{review_prompt}" --effort high')
        
        context.review_results["principles"] = "APPROVED" in review
        context.review_feedback["principles"] = review
        
        return context

class MergeAgent(BaseAgent):
    """Handles merging if all reviews pass"""
    
    async def execute(self, context: IssueContext) -> IssueContext:
        print(f"🔀 {self.name}: Processing merge decision")
        
        # Check if all reviews passed
        all_approved = all(context.review_results.values())
        
        if all_approved:
            print("✅ All reviews passed! Merging...")
            
            # Commit changes
            commit_message = f"""fix: Resolve issue #{context.issue_number}

{context.issue_title}

Implementation following SOLID/DRY/KISS/YAGNI principles.

Reviews passed:
- Code Quality: ✅
- Testing: ✅
- Design Principles: ✅

Automated by Issue Resolver Bot"""
            
            self.run_command(f'git commit -m "{commit_message}"')
            self.run_command(f"git push origin {context.branch_name}")
            
            # Create PR
            pr_body = f"""## Automated Resolution for #{context.issue_number}

### Implementation Summary
{context.implementation_plan[:500]}...

### Review Results
- **Code Quality**: {context.review_feedback['code_quality'][:200]}...
- **Testing**: {context.review_feedback['testing'][:200]}...
- **Principles**: {context.review_feedback['principles'][:200]}...

### Closes #{context.issue_number}
"""
            
            pr_command = f'''gh pr create --title "Auto: Fix issue #{context.issue_number}" --body "{pr_body}" --base main'''
            pr_url = self.run_command(pr_command)
            
            # Auto-merge if enabled
            if pr_url:
                self.run_command(f"gh pr merge --auto --squash {pr_url.strip()}")
                print(f"✅ PR created and set to auto-merge: {pr_url}")
        else:
            print("❌ Reviews failed. Manual intervention needed.")
            print("\nReview Feedback:")
            for reviewer, feedback in context.review_feedback.items():
                print(f"\n{reviewer}:\n{feedback[:500]}...")
        
        return context

class IssueResolverOrchestrator:
    """Main orchestrator that coordinates all agents"""
    
    def __init__(self):
        self.agents = {
            AgentState.ANALYZING: IssueAnalyzerAgent("Analyzer"),
            AgentState.RESEARCHING: ResearchAgent("Researcher"),
            AgentState.IMPLEMENTING: ImplementationAgent("Developer"),
            AgentState.REVIEWING: [
                CodeQualityReviewer("Quality Reviewer"),
                TestingReviewer("Test Reviewer"),
                PrinciplesReviewer("Principles Reviewer")
            ],
            AgentState.MERGING: MergeAgent("Merger")
        }
        self.state = AgentState.ANALYZING
    
    async def resolve_issue(self, issue_number: int) -> bool:
        """Main workflow to resolve a GitHub issue"""
        
        # Get issue details
        issue_data = self.get_issue_details(issue_number)
        if not issue_data:
            return False
        
        context = IssueContext(
            issue_number=issue_number,
            issue_title=issue_data['title'],
            issue_body=issue_data['body']
        )
        
        try:
            # Execute workflow
            print(f"\n🚀 Starting automated resolution for issue #{issue_number}\n")
            
            # Analysis phase
            self.state = AgentState.ANALYZING
            context = await self.agents[self.state].execute(context)
            
            # Research phase
            self.state = AgentState.RESEARCHING
            context = await self.agents[self.state].execute(context)
            
            # Implementation phase
            self.state = AgentState.IMPLEMENTING
            context = await self.agents[self.state].execute(context)
            
            # Review phase - run all reviewers in parallel
            self.state = AgentState.REVIEWING
            review_tasks = [
                reviewer.execute(context) 
                for reviewer in self.agents[self.state]
            ]
            await asyncio.gather(*review_tasks)
            
            # Merge phase
            self.state = AgentState.MERGING
            context = await self.agents[self.state].execute(context)
            
            self.state = AgentState.COMPLETED
            print(f"\n✅ Issue #{issue_number} resolution workflow completed!\n")
            return True
            
        except Exception as e:
            self.state = AgentState.FAILED
            print(f"\n❌ Error resolving issue #{issue_number}: {e}\n")
            # Cleanup - switch back to main branch
            subprocess.run("git checkout main", shell=True)
            return False
    
    def get_issue_details(self, issue_number: int) -> Dict:
        """Fetch issue details from GitHub"""
        result = subprocess.run(
            f"gh issue view {issue_number} --json title,body",
            shell=True,
            capture_output=True,
            text=True
        )
        
        if result.returncode == 0:
            return json.loads(result.stdout)
        return None

async def main():
    """Main entry point"""
    import sys
    
    if len(sys.argv) != 2:
        print("Usage: python issue_resolver.py <issue_number>")
        sys.exit(1)
    
    issue_number = int(sys.argv[1])
    orchestrator = IssueResolverOrchestrator()
    
    success = await orchestrator.resolve_issue(issue_number)
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    asyncio.run(main())