using Toybox.WatchUi;
using Toybox.Graphics;
using Toybox.System;

class AboutView extends WatchUi.View {
    function initialize() {
        View.initialize();
    }

    function onLayout(dc) {
    }

    function onUpdate(dc) {
        dc.setColor(Graphics.COLOR_BLACK, Graphics.COLOR_BLACK);
        dc.clear();
        
        var width = dc.getWidth();
        var height = dc.getHeight();
        var centerX = width / 2;
        var y = 40;
        
        // App name and version
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
        dc.drawText(centerX, y, Graphics.FONT_MEDIUM, "Fidan", Graphics.TEXT_JUSTIFY_CENTER);
        y += 35;
        
        dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.drawText(centerX, y, Graphics.FONT_XTINY, "Version 1.0.0", Graphics.TEXT_JUSTIFY_CENTER);
        y += 25;
        
        // Description
        dc.drawText(centerX, y, Graphics.FONT_XTINY, "Focus Timer", Graphics.TEXT_JUSTIFY_CENTER);
        y += 20;
        dc.drawText(centerX, y, Graphics.FONT_XTINY, "for Productivity", Graphics.TEXT_JUSTIFY_CENTER);
        y += 30;
        
        // Tree icon
        drawAboutTree(dc, centerX, y + 20);
        
        // Footer
        dc.setColor(Graphics.COLOR_DK_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.drawText(centerX, height - 40, Graphics.FONT_XTINY, "Plant trees", Graphics.TEXT_JUSTIFY_CENTER);
        dc.drawText(centerX, height - 25, Graphics.FONT_XTINY, "Stay focused", Graphics.TEXT_JUSTIFY_CENTER);
    }

    function drawAboutTree(dc, x, y) {
        // Draw a decorative tree
        dc.setColor(0x8B4513, Graphics.COLOR_TRANSPARENT); // Brown trunk
        dc.fillRectangle(x - 3, y, 6, 15);
        
        // Green leaves
        dc.setColor(Graphics.COLOR_GREEN, Graphics.COLOR_TRANSPARENT);
        dc.fillCircle(x, y - 10, 15);
        dc.fillCircle(x - 8, y - 5, 10);
        dc.fillCircle(x + 8, y - 5, 10);
        
        // Darker green details
        dc.setColor(Graphics.COLOR_DK_GREEN, Graphics.COLOR_TRANSPARENT);
        dc.fillCircle(x, y - 15, 8);
    }

    function onShow() {
    }

    function onHide() {
    }
}