using Toybox.Background;
using Toybox.System;
using Toybox.Time;

(:background)
class BackgroundServiceDelegate extends System.ServiceDelegate {
    private var _sessionManager;
    
    function initialize() {
        ServiceDelegate.initialize();
    }
    
    function onTemporalEvent() {
        // This gets called periodically when app is in background
        // Timer continues via the SessionManager's internal timer
        // State is persisted automatically
        
        // Check if session completed while in background
        var sessionState = Application.Storage.getValue("current_session");
        if (sessionState != null) {
            var remainingTime = sessionState.get("remainingTime");
            if (remainingTime != null && remainingTime <= 0) {
                // Session completed in background - send notification
                if (Attention has :playTone) {
                    Attention.playTone(Attention.TONE_SUCCESS);
                }
                if (Attention has :vibrate) {
                    var vibePattern = [
                        new Attention.VibeProfile(100, 300),
                        new Attention.VibeProfile(0, 200),
                        new Attention.VibeProfile(100, 500)
                    ];
                    Attention.vibrate(vibePattern);
                }
            }
        }
    }
}