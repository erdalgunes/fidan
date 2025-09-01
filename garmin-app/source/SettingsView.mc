using Toybox.WatchUi;
using Toybox.Graphics;
using Toybox.Application.Storage;
using Toybox.Application.Properties;

class SettingsView extends WatchUi.View {
    private var _settings;
    
    function initialize() {
        View.initialize();
        loadSettings();
    }
    
    function loadSettings() {
        _settings = {
            "vibrationEnabled" => Properties.getValue("vibrationEnabled") != false,
            "soundEnabled" => Properties.getValue("soundEnabled") != false,
            "intervalAlerts" => Properties.getValue("intervalAlerts") != false,
            "sessionDuration" => Properties.getValue("sessionDuration") != null ? 
                                Properties.getValue("sessionDuration") : 25
        };
    }
    
    function onLayout(dc) {
        // No specific layout needed
    }
    
    function onUpdate(dc) {
        dc.setColor(Graphics.COLOR_BLACK, Graphics.COLOR_BLACK);
        dc.clear();
        
        var centerX = dc.getWidth() / 2;
        var y = 30;
        var lineHeight = 35;
        
        // Title
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
        dc.drawText(centerX, y, Graphics.FONT_SMALL, "Settings", Graphics.TEXT_JUSTIFY_CENTER);
        y += lineHeight + 10;
        
        // Settings items
        dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
        
        // Vibration setting
        var vibrationText = "Vibration: " + (_settings["vibrationEnabled"] ? "ON" : "OFF");
        dc.drawText(centerX, y, Graphics.FONT_TINY, vibrationText, Graphics.TEXT_JUSTIFY_CENTER);
        y += lineHeight;
        
        // Sound setting
        var soundText = "Sound: " + (_settings["soundEnabled"] ? "ON" : "OFF");
        dc.drawText(centerX, y, Graphics.FONT_TINY, soundText, Graphics.TEXT_JUSTIFY_CENTER);
        y += lineHeight;
        
        // Interval alerts
        var intervalText = "5-min Alerts: " + (_settings["intervalAlerts"] ? "ON" : "OFF");
        dc.drawText(centerX, y, Graphics.FONT_TINY, intervalText, Graphics.TEXT_JUSTIFY_CENTER);
        y += lineHeight;
        
        // Session duration
        var durationText = "Duration: " + _settings["sessionDuration"] + " min";
        dc.drawText(centerX, y, Graphics.FONT_TINY, durationText, Graphics.TEXT_JUSTIFY_CENTER);
        
        // Instructions at bottom
        dc.setColor(Graphics.COLOR_DK_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.drawText(centerX, dc.getHeight() - 30, Graphics.FONT_XTINY, 
                   "Use phone app to change", Graphics.TEXT_JUSTIFY_CENTER);
    }
    
    function getSettings() {
        return _settings;
    }
}

class SettingsDelegate extends WatchUi.BehaviorDelegate {
    private var _view;
    
    function initialize(view) {
        BehaviorDelegate.initialize();
        _view = view;
    }
    
    function onBack() {
        // Save any changes (though currently read-only)
        WatchUi.popView(WatchUi.SLIDE_DOWN);
        return true;
    }
    
    function onSelect() {
        // In future, could cycle through settings
        // For now, just show message
        WatchUi.showToast("Use phone app to change settings", {:icon => WatchUi.TOAST_ICON_WARNING});
        return true;
    }
}