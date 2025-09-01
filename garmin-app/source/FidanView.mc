using Toybox.WatchUi;
using Toybox.Graphics;
using Toybox.Math;

class FidanView extends WatchUi.View {
    private var _sessionManager;
    private var _treeSprite;
    private var _centerX;
    private var _centerY;

    function initialize(sessionManager) {
        View.initialize();
        _sessionManager = sessionManager;
        _sessionManager.setUpdateCallback(method(:onUpdate));
    }

    function onLayout(dc) {
        _centerX = dc.getWidth() / 2;
        _centerY = dc.getHeight() / 2;
    }

    function onUpdate(dc) {
        // Clear background
        dc.setColor(Graphics.COLOR_BLACK, Graphics.COLOR_BLACK);
        dc.clear();
        
        var isActive = _sessionManager.isActive();
        var progress = _sessionManager.getProgress();
        
        // Draw tree visualization
        drawTree(dc, progress, isActive);
        
        // Draw timer
        drawTimer(dc, isActive);
        
        // Draw status text
        drawStatus(dc, isActive);
    }

    function drawTree(dc, progress, isActive) {
        var treeY = _centerY - 40;
        
        if (!isActive) {
            // Draw seed icon when inactive
            dc.setColor(Graphics.COLOR_DK_GREEN, Graphics.COLOR_TRANSPARENT);
            dc.fillCircle(_centerX, treeY, 8);
            dc.setColor(Graphics.COLOR_GREEN, Graphics.COLOR_TRANSPARENT);
            dc.fillCircle(_centerX, treeY, 6);
        } else {
            // Draw growing tree based on progress
            var treeHeight = (60 * progress).toNumber();
            var treeWidth = (40 * progress).toNumber();
            
            // Draw trunk
            dc.setColor(0x8B4513, Graphics.COLOR_TRANSPARENT); // Brown
            var trunkHeight = (20 * progress).toNumber();
            dc.fillRectangle(_centerX - 3, treeY, 6, trunkHeight);
            
            // Draw leaves/crown
            if (progress > 0.2) {
                dc.setColor(Graphics.COLOR_GREEN, Graphics.COLOR_TRANSPARENT);
                
                // Draw tree crown as overlapping circles for a more organic look
                var crownY = treeY - treeHeight / 2;
                var crownRadius = treeWidth / 2;
                
                // Main crown
                dc.fillCircle(_centerX, crownY, crownRadius);
                
                // Side branches
                if (progress > 0.4) {
                    dc.fillCircle(_centerX - crownRadius/2, crownY + 5, crownRadius * 0.7);
                    dc.fillCircle(_centerX + crownRadius/2, crownY + 5, crownRadius * 0.7);
                }
                
                // Top detail
                if (progress > 0.6) {
                    dc.setColor(Graphics.COLOR_DK_GREEN, Graphics.COLOR_TRANSPARENT);
                    dc.fillCircle(_centerX, crownY - crownRadius/2, crownRadius * 0.5);
                }
            }
            
            // Draw progress ring around tree
            drawProgressRing(dc, _centerX, treeY, 50, progress);
        }
    }

    function drawProgressRing(dc, x, y, radius, progress) {
        var startAngle = 90; // Start from top
        var sweepAngle = (360 * progress).toNumber();
        
        dc.setPenWidth(3);
        dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.drawCircle(x, y, radius);
        
        if (progress > 0) {
            dc.setColor(Graphics.COLOR_GREEN, Graphics.COLOR_TRANSPARENT);
            dc.drawArc(x, y, radius, Graphics.ARC_CLOCKWISE, startAngle, startAngle - sweepAngle);
        }
        dc.setPenWidth(1);
    }

    function drawTimer(dc, isActive) {
        var timerY = _centerY + 30;
        var timeString = _sessionManager.getFormattedTime();
        
        // Set font and color based on state
        dc.setColor(isActive ? Graphics.COLOR_WHITE : Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.drawText(_centerX, timerY, Graphics.FONT_NUMBER_MEDIUM, timeString, Graphics.TEXT_JUSTIFY_CENTER);
    }

    function drawStatus(dc, isActive) {
        var statusY = _centerY + 70;
        var statusText = isActive ? "Focus Mode" : "Tap to Start";
        
        dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.drawText(_centerX, statusY, Graphics.FONT_TINY, statusText, Graphics.TEXT_JUSTIFY_CENTER);
        
        // Draw instruction hint at bottom
        if (!isActive) {
            var hintY = dc.getHeight() - 30;
            dc.drawText(_centerX, hintY, Graphics.FONT_XTINY, "Press Start", Graphics.TEXT_JUSTIFY_CENTER);
        }
    }

    function onShow() {
    }

    function onHide() {
    }
}