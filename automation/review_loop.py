#!/usr/bin/env python3
"""
Iterative Review Loop for Multi-Agent System
Implements 3-round review with improvements
"""

import asyncio
from dataclasses import dataclass, field
from typing import Dict, List, Optional
from enum import Enum

class ReviewStatus(Enum):
    """Review status for each round"""
    PENDING = "pending"
    APPROVED = "approved"
    CHANGES_REQUESTED = "changes_requested"
    FAILED = "failed"

@dataclass
class ReviewRound:
    """Represents a single review round"""
    round_number: int
    status: ReviewStatus = ReviewStatus.PENDING
    code_quality_score: float = 0.0
    test_coverage: float = 0.0
    principles_score: float = 0.0
    feedback: Dict[str, str] = field(default_factory=dict)
    improvements_made: List[str] = field(default_factory=list)

class IterativeReviewSystem:
    """Manages iterative review and improvement cycles"""
    
    def __init__(self, max_rounds: int = 3):
        self.max_rounds = max_rounds
        self.rounds: List[ReviewRound] = []
        self.current_round = 0
    
    async def conduct_review_cycle(self, context) -> bool:
        """
        Conduct up to 3 rounds of review and improvement
        Returns True if approved, False if failed after max rounds
        """
        
        for round_num in range(1, self.max_rounds + 1):
            print(f"\n🔄 Review Round {round_num}/{self.max_rounds}")
            
            review_round = ReviewRound(round_number=round_num)
            
            # Conduct reviews
            review_results = await self.run_review_agents(context)
            
            # Calculate scores
            review_round.code_quality_score = review_results.get('code_quality_score', 0)
            review_round.test_coverage = review_results.get('test_coverage', 0)
            review_round.principles_score = review_results.get('principles_score', 0)
            review_round.feedback = review_results.get('feedback', {})
            
            # Determine if approved
            if self.is_approved(review_round):
                review_round.status = ReviewStatus.APPROVED
                self.rounds.append(review_round)
                print(f"✅ Round {round_num}: All reviews passed!")
                return True
            
            # If not last round, try to improve
            if round_num < self.max_rounds:
                print(f"🔧 Round {round_num}: Improvements needed")
                review_round.status = ReviewStatus.CHANGES_REQUESTED
                
                # Apply improvements based on feedback
                improvements = await self.apply_improvements(
                    context, 
                    review_round.feedback
                )
                review_round.improvements_made = improvements
                
                # Use GPT-5 to refactor based on feedback
                await self.intelligent_refactor(context, review_round)
            else:
                review_round.status = ReviewStatus.FAILED
                print(f"❌ Round {round_num}: Max rounds reached without approval")
            
            self.rounds.append(review_round)
        
        return False
    
    async def run_review_agents(self, context) -> Dict:
        """Run all review agents and collect results"""
        
        results = {}
        
        # Code Quality Review
        quality_prompt = f"""
        Review this code diff for quality (score 0-100):
        {context.get('diff', '')}
        
        Evaluate:
        - Readability and clarity
        - Error handling
        - Performance considerations
        - Code organization
        
        Return: SCORE: [0-100] and specific feedback
        """
        
        quality_review = await self.run_gpt5(quality_prompt, "medium")
        results['code_quality_score'] = self.extract_score(quality_review)
        results['feedback'] = {'code_quality': quality_review}
        
        # Testing Review
        testing_prompt = f"""
        Review test coverage for these changes:
        {context.get('diff', '')}
        
        Check:
        - Are tests included?
        - Coverage percentage
        - Edge cases handled?
        
        Return: COVERAGE: [0-100]% and feedback
        """
        
        test_review = await self.run_gpt5(testing_prompt, "medium")
        results['test_coverage'] = self.extract_score(test_review)
        results['feedback']['testing'] = test_review
        
        # Principles Review
        principles_prompt = f"""
        Score adherence to SOLID/DRY/KISS/YAGNI (0-100):
        {context.get('diff', '')}
        
        Check for:
        - Single Responsibility violations
        - Code duplication (DRY)
        - Unnecessary complexity (KISS)
        - Over-engineering (YAGNI)
        
        Return: SCORE: [0-100] and specific violations
        """
        
        principles_review = await self.run_gpt5(principles_prompt, "high")
        results['principles_score'] = self.extract_score(principles_review)
        results['feedback']['principles'] = principles_review
        
        return results
    
    def is_approved(self, review_round: ReviewRound) -> bool:
        """Check if review round meets approval criteria"""
        return (
            review_round.code_quality_score >= 80 and
            review_round.test_coverage >= 70 and
            review_round.principles_score >= 85
        )
    
    async def apply_improvements(self, context, feedback: Dict) -> List[str]:
        """Apply specific improvements based on feedback"""
        improvements = []
        
        # Parse feedback and identify specific issues
        for category, review_text in feedback.items():
            if "error handling" in review_text.lower():
                improvements.append("Added try-catch blocks for error handling")
                await self.add_error_handling(context)
            
            if "test" in review_text.lower() and "missing" in review_text.lower():
                improvements.append("Added unit tests for new functionality")
                await self.generate_tests(context)
            
            if "duplicate" in review_text.lower() or "DRY" in review_text:
                improvements.append("Refactored duplicate code into reusable functions")
                await self.remove_duplication(context)
            
            if "complex" in review_text.lower() or "KISS" in review_text:
                improvements.append("Simplified complex logic")
                await self.simplify_code(context)
        
        return improvements
    
    async def intelligent_refactor(self, context, review_round: ReviewRound):
        """Use GPT-5 to intelligently refactor based on feedback"""
        
        refactor_prompt = f"""
        Refactor this code to address these review comments:
        
        Current Code:
        {context.get('current_code', '')}
        
        Review Feedback:
        - Code Quality ({review_round.code_quality_score}/100): {review_round.feedback.get('code_quality', '')}
        - Testing ({review_round.test_coverage}/100): {review_round.feedback.get('testing', '')}
        - Principles ({review_round.principles_score}/100): {review_round.feedback.get('principles', '')}
        
        Previous improvements: {', '.join(review_round.improvements_made)}
        
        Generate ONLY the improved code that addresses ALL feedback.
        Maintain functionality while improving quality scores.
        """
        
        improved_code = await self.run_gpt5(refactor_prompt, "high")
        context['current_code'] = improved_code
        context['diff'] = self.generate_diff(context.get('original_code', ''), improved_code)
    
    async def add_error_handling(self, context):
        """Add error handling to code"""
        prompt = f"""
        Add proper error handling to this code:
        {context.get('current_code', '')}
        
        Use try-catch blocks and proper error messages.
        Follow Kotlin error handling best practices.
        """
        
        improved = await self.run_gpt5(prompt, "medium")
        context['current_code'] = improved
    
    async def generate_tests(self, context):
        """Generate tests for the code"""
        prompt = f"""
        Generate unit tests for this code:
        {context.get('current_code', '')}
        
        Include:
        - Happy path tests
        - Edge cases
        - Error scenarios
        
        Use JUnit and Mockito as needed.
        """
        
        tests = await self.run_gpt5(prompt, "medium")
        context['tests'] = tests
    
    async def remove_duplication(self, context):
        """Remove code duplication"""
        prompt = f"""
        Refactor this code to remove duplication:
        {context.get('current_code', '')}
        
        Extract common logic into reusable functions.
        Follow DRY principle strictly.
        """
        
        improved = await self.run_gpt5(prompt, "medium")
        context['current_code'] = improved
    
    async def simplify_code(self, context):
        """Simplify complex code"""
        prompt = f"""
        Simplify this code following KISS principle:
        {context.get('current_code', '')}
        
        - Remove unnecessary complexity
        - Use simpler data structures if possible
        - Make the code more readable
        """
        
        improved = await self.run_gpt5(prompt, "medium")
        context['current_code'] = improved
    
    async def run_gpt5(self, prompt: str, effort: str) -> str:
        """Run GPT-5 command"""
        import subprocess
        result = subprocess.run(
            f'gpt5 "{prompt}" --effort {effort}',
            shell=True,
            capture_output=True,
            text=True
        )
        return result.stdout if result.returncode == 0 else ""
    
    def extract_score(self, text: str) -> float:
        """Extract numerical score from review text"""
        import re
        # Look for patterns like "SCORE: 85" or "85/100" or "85%"
        patterns = [
            r'SCORE:\s*(\d+)',
            r'(\d+)/100',
            r'(\d+)%',
            r'COVERAGE:\s*(\d+)'
        ]
        
        for pattern in patterns:
            match = re.search(pattern, text)
            if match:
                return float(match.group(1))
        
        return 0.0
    
    def generate_diff(self, original: str, modified: str) -> str:
        """Generate a diff between original and modified code"""
        import difflib
        diff = difflib.unified_diff(
            original.splitlines(keepends=True),
            modified.splitlines(keepends=True),
            fromfile='original',
            tofile='modified'
        )
        return ''.join(diff)
    
    def generate_report(self) -> str:
        """Generate a summary report of all review rounds"""
        report = ["# Review Cycle Report\n"]
        
        for round_data in self.rounds:
            report.append(f"\n## Round {round_data.round_number}")
            report.append(f"**Status**: {round_data.status.value}")
            report.append(f"**Scores**:")
            report.append(f"  - Code Quality: {round_data.code_quality_score}/100")
            report.append(f"  - Test Coverage: {round_data.test_coverage}%")
            report.append(f"  - Principles: {round_data.principles_score}/100")
            
            if round_data.improvements_made:
                report.append(f"**Improvements Made**:")
                for improvement in round_data.improvements_made:
                    report.append(f"  - {improvement}")
        
        final_status = "✅ APPROVED" if self.rounds[-1].status == ReviewStatus.APPROVED else "❌ FAILED"
        report.append(f"\n## Final Status: {final_status}")
        
        return '\n'.join(report)

# Integration with main orchestrator
async def enhanced_review_process(context) -> bool:
    """
    Enhanced review process with iterative improvements
    To be integrated with IssueResolverOrchestrator
    """
    review_system = IterativeReviewSystem(max_rounds=3)
    
    # Prepare context for review
    review_context = {
        'diff': context.get('code_changes', ''),
        'current_code': context.get('implementation', ''),
        'original_code': '',
        'tests': ''
    }
    
    # Run review cycle
    approved = await review_system.conduct_review_cycle(review_context)
    
    # Generate report
    report = review_system.generate_report()
    print(report)
    
    # Update main context with results
    context['review_report'] = report
    context['review_approved'] = approved
    context['final_code'] = review_context.get('current_code', '')
    context['final_tests'] = review_context.get('tests', '')
    
    return approved

if __name__ == "__main__":
    # Test the review system
    test_context = {
        'diff': '''
        +fun calculateSum(a: Int, b: Int): Int {
        +    return a + b
        +}
        ''',
        'current_code': '''
        fun calculateSum(a: Int, b: Int): Int {
            return a + b
        }
        '''
    }
    
    asyncio.run(enhanced_review_process(test_context))