using Toybox.WatchUi;
using Toybox.Graphics;

class FidanGlance extends WatchUi.GlanceView {
    private var _sessionManager;

    function initialize(sessionManager) {
        GlanceView.initialize();
        _sessionManager = sessionManager;
    }

    function onUpdate(dc) {
        dc.setColor(Graphics.COLOR_BLACK, Graphics.COLOR_BLACK);
        dc.clear();
        
        var width = dc.getWidth();
        var height = dc.getHeight();
        
        if (_sessionManager.isActive()) {
            // Show active session info
            var progress = _sessionManager.getProgress();
            var timeString = _sessionManager.getFormattedTime();
            
            // Draw mini tree icon
            drawMiniTree(dc, 30, height/2, progress);
            
            // Draw time
            dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
            dc.drawText(60, height/2 - 10, Graphics.FONT_SMALL, timeString, Graphics.TEXT_JUSTIFY_LEFT);
            
            // Draw progress bar
            var barWidth = width - 80;
            var barHeight = 4;
            var barX = 60;
            var barY = height/2 + 10;
            
            dc.setColor(Graphics.COLOR_DK_GRAY, Graphics.COLOR_TRANSPARENT);
            dc.fillRectangle(barX, barY, barWidth, barHeight);
            
            dc.setColor(Graphics.COLOR_GREEN, Graphics.COLOR_TRANSPARENT);
            dc.fillRectangle(barX, barY, (barWidth * progress).toNumber(), barHeight);
        } else {
            // Show idle state
            dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
            dc.drawText(width/2, height/2 - 10, Graphics.FONT_SMALL, "Fidan", Graphics.TEXT_JUSTIFY_CENTER);
            dc.drawText(width/2, height/2 + 10, Graphics.FONT_XTINY, "Tap to focus", Graphics.TEXT_JUSTIFY_CENTER);
        }
    }

    function drawMiniTree(dc, x, y, progress) {
        if (progress < 0.1) {
            // Seed
            dc.setColor(Graphics.COLOR_GREEN, Graphics.COLOR_TRANSPARENT);
            dc.fillCircle(x, y, 3);
        } else {
            // Growing tree
            var treeSize = (10 * progress).toNumber() + 5;
            
            // Trunk
            dc.setColor(0x8B4513, Graphics.COLOR_TRANSPARENT);
            dc.fillRectangle(x - 1, y, 2, 5);
            
            // Leaves
            dc.setColor(Graphics.COLOR_GREEN, Graphics.COLOR_TRANSPARENT);
            dc.fillCircle(x, y - treeSize/2, treeSize/2);
        }
    }
}