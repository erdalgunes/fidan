#!/usr/bin/env python3
"""
TEST version - Verify each step works
"""

import subprocess
import json

def test_commands():
    """Test each command individually"""
    
    print("Testing gh command...")
    result = subprocess.run("gh issue view 70 --json title", shell=True, capture_output=True, text=True)
    if result.returncode == 0:
        issue = json.loads(result.stdout)
        print(f"✅ gh works: {issue['title']}")
    else:
        print(f"❌ gh failed: {result.stderr}")
    
    print("\nTesting tavily command...")
    result = subprocess.run('tavily "Android timer persistence" --max-results 1', shell=True, capture_output=True, text=True)
    if result.returncode == 0:
        print(f"✅ tavily works: {result.stdout[:100]}...")
    else:
        print(f"❌ tavily failed: {result.stderr}")
    
    print("\nTesting gpt5 command...")
    result = subprocess.run('gpt5 "Say hello in 3 words" --effort low', shell=True, capture_output=True, text=True)
    if result.returncode == 0:
        print(f"✅ gpt5 works: {result.stdout.strip()}")
    else:
        print(f"❌ gpt5 failed: {result.stderr}")

if __name__ == "__main__":
    test_commands()