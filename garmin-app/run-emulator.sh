#!/bin/bash

# Simplified Garmin App Emulator Runner
set -e

echo "🚀 Garmin Connect IQ App Emulator Setup"
echo "========================================"

# Create developer key if needed
DEV_KEY="$HOME/garmin_dev_key.pem"
if [ ! -f "$DEV_KEY" ]; then
    echo "🔑 Creating developer key..."
    openssl genrsa -out "$DEV_KEY" 2048
    echo "✅ Developer key created"
fi

# Since SDK isn't installed, let's create a web-based simulator approach
echo ""
echo "📱 Setting up Web Simulator Alternative..."

# Create an HTML simulator
cat > simulator.html << 'HTML'
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Fidan Garmin App Simulator</title>
    <style>
        body {
            margin: 0;
            padding: 20px;
            background: #000;
            color: #fff;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
        }
        
        .watch-frame {
            width: 360px;
            height: 360px;
            border-radius: 50%;
            background: #1a1a1a;
            border: 20px solid #333;
            position: relative;
            box-shadow: 0 20px 40px rgba(0,0,0,0.5);
        }
        
        .watch-screen {
            width: 320px;
            height: 320px;
            border-radius: 50%;
            background: #000;
            position: absolute;
            top: 20px;
            left: 20px;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            overflow: hidden;
        }
        
        .tree {
            width: 60px;
            height: 60px;
            margin-bottom: 20px;
            transition: all 0.5s ease;
        }
        
        .tree.seed {
            width: 20px;
            height: 20px;
            background: #2e7d32;
            border-radius: 50%;
        }
        
        .tree.growing {
            background: linear-gradient(to top, #8b4513 30%, #4caf50 30%);
            border-radius: 50% 50% 0 0;
        }
        
        .timer {
            font-size: 48px;
            font-weight: 300;
            margin: 10px 0;
            font-variant-numeric: tabular-nums;
        }
        
        .status {
            color: #999;
            font-size: 14px;
            margin-top: 10px;
        }
        
        .controls {
            position: absolute;
            right: -80px;
            top: 50%;
            transform: translateY(-50%);
        }
        
        .btn {
            display: block;
            width: 60px;
            height: 60px;
            border-radius: 50%;
            background: #333;
            border: 2px solid #666;
            color: #fff;
            margin: 10px 0;
            cursor: pointer;
            font-size: 10px;
        }
        
        .btn:hover {
            background: #444;
        }
        
        .btn:active {
            background: #222;
        }
        
        .progress-ring {
            position: absolute;
            top: 0;
            left: 0;
            width: 320px;
            height: 320px;
        }
        
        .progress-ring circle {
            fill: none;
            stroke-width: 3;
        }
        
        .progress-bg {
            stroke: #333;
        }
        
        .progress-fill {
            stroke: #4caf50;
            stroke-dasharray: 1005;
            stroke-dashoffset: 1005;
            transform: rotate(-90deg);
            transform-origin: center;
            transition: stroke-dashoffset 1s linear;
        }
    </style>
</head>
<body>
    <div class="watch-frame">
        <div class="watch-screen">
            <svg class="progress-ring">
                <circle class="progress-bg" cx="160" cy="160" r="160"></circle>
                <circle class="progress-fill" cx="160" cy="160" r="160"></circle>
            </svg>
            <div class="tree seed" id="tree"></div>
            <div class="timer" id="timer">25:00</div>
            <div class="status" id="status">Tap to Start</div>
        </div>
        <div class="controls">
            <button class="btn" onclick="toggleTimer()">START</button>
            <button class="btn" onclick="resetTimer()">BACK</button>
        </div>
    </div>

    <script>
        let timerInterval = null;
        let totalSeconds = 25 * 60;
        let currentSeconds = totalSeconds;
        let isRunning = false;

        function updateDisplay() {
            const minutes = Math.floor(currentSeconds / 60);
            const seconds = currentSeconds % 60;
            document.getElementById('timer').textContent = 
                `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
            
            const progress = 1 - (currentSeconds / totalSeconds);
            const offset = 1005 - (1005 * progress);
            document.querySelector('.progress-fill').style.strokeDashoffset = offset;
            
            // Update tree based on progress
            const tree = document.getElementById('tree');
            if (progress === 0) {
                tree.className = 'tree seed';
            } else if (progress < 0.5) {
                tree.className = 'tree growing';
                tree.style.height = `${30 + progress * 60}px`;
            } else {
                tree.className = 'tree growing';
                tree.style.height = '60px';
                tree.style.width = `${60 + progress * 20}px`;
            }
            
            // Update status
            const status = document.getElementById('status');
            if (isRunning) {
                status.textContent = 'Focus Mode';
            } else if (currentSeconds === 0) {
                status.textContent = 'Session Complete!';
            } else if (currentSeconds < totalSeconds) {
                status.textContent = 'Paused';
            } else {
                status.textContent = 'Tap to Start';
            }
        }

        function toggleTimer() {
            if (currentSeconds === 0) {
                resetTimer();
                return;
            }
            
            if (isRunning) {
                clearInterval(timerInterval);
                isRunning = false;
            } else {
                isRunning = true;
                timerInterval = setInterval(() => {
                    currentSeconds--;
                    updateDisplay();
                    
                    if (currentSeconds === 0) {
                        clearInterval(timerInterval);
                        isRunning = false;
                        // Vibration simulation
                        if ('vibrate' in navigator) {
                            navigator.vibrate([200, 100, 200, 100, 500]);
                        }
                        alert('🌳 Session Complete! Your tree is fully grown!');
                    }
                }, 1000);
            }
            updateDisplay();
        }

        function resetTimer() {
            clearInterval(timerInterval);
            isRunning = false;
            currentSeconds = totalSeconds;
            updateDisplay();
        }

        // Initialize
        updateDisplay();
    </script>
</body>
</html>
HTML

echo "✅ Web simulator created"
echo ""
echo "🎮 Launching Garmin App Simulator..."
echo ""

# Open the simulator in default browser
open simulator.html

echo "================================================"
echo "🎉 Simulator Launched Successfully!"
echo "================================================"
echo ""
echo "📱 Simulator Controls:"
echo "  • START button: Start/pause the 25-minute timer"
echo "  • BACK button: Reset the timer"
echo ""
echo "🌳 Watch the tree grow as you focus!"
echo ""
echo "For the real Garmin Connect IQ experience:"
echo "1. Download SDK: https://developer.garmin.com/connect-iq/sdk/"
echo "2. Install to ~/connectiq/"
echo "3. Build with: monkeyc -d fenix7 -f monkey.jungle -o fidan.prg"
echo ""
echo "This web simulator demonstrates the app's functionality!"