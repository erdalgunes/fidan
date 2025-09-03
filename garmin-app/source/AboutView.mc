using Toybox.WatchUi;
using Toybox.Graphics;
using Toybox.System;

class AboutView extends WatchUi.View {
    private const LINE_SPACING_SMALL = 20;
    private const LINE_SPACING_MEDIUM = 25; 
    private const LINE_SPACING_LARGE = 30;
    private const LINE_SPACING_XLARGE = 35;
    private const FOOTER_MARGIN = 25;
    private const FOOTER_MARGIN_LARGE = 40;
    
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
        y += LINE_SPACING_XLARGE;
        
        dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.drawText(centerX, y, Graphics.FONT_XTINY, "Version 1.0.0", Graphics.TEXT_JUSTIFY_CENTER);
        y += LINE_SPACING_MEDIUM;
        
        // Description
        dc.drawText(centerX, y, Graphics.FONT_XTINY, "Focus Timer", Graphics.TEXT_JUSTIFY_CENTER);
        y += LINE_SPACING_SMALL;
        dc.drawText(centerX, y, Graphics.FONT_XTINY, "for Productivity", Graphics.TEXT_JUSTIFY_CENTER);
        y += LINE_SPACING_LARGE;
        
        // Tree icon
        drawAboutTree(dc, centerX, y + 20);
        
        // Footer
        dc.setColor(Graphics.COLOR_DK_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.drawText(centerX, height - FOOTER_MARGIN_LARGE, Graphics.FONT_XTINY, "Plant trees", Graphics.TEXT_JUSTIFY_CENTER);
        dc.drawText(centerX, height - FOOTER_MARGIN, Graphics.FONT_XTINY, "Stay focused", Graphics.TEXT_JUSTIFY_CENTER);
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