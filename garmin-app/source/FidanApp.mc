using Toybox.Application;
using Toybox.WatchUi;
using Toybox.Timer;
using Toybox.Attention;
using Toybox.Background;
using Toybox.Time;

class FidanApp extends Application.AppBase {
    private var _sessionManager;
    private var _dataSync;

    function initialize() {
        AppBase.initialize();
        _sessionManager = new SessionManager();
        _dataSync = new DataSync();
    }

    function onStart(state) {
        _dataSync.initialize();
        
        // Register for background temporal events
        // This keeps the app alive in background for timer updates
        if (Background has :registerForTemporalEvent) {
            Background.registerForTemporalEvent(new Time.Duration(60)); // Check every minute
        }
    }

    function onStop(state) {
        _sessionManager.saveState();
        _dataSync.syncWithPhone();
    }

    function getInitialView() {
        var view = new FidanView(_sessionManager);
        var delegate = new FidanDelegate(_sessionManager, view);
        return [view, delegate];
    }

    function onBackgroundData(data) {
        _dataSync.handleBackgroundData(data);
    }

    function getGlanceView() {
        return [new FidanGlance(_sessionManager)];
    }

    function onSettingsChanged() {
        _sessionManager.updateSettings();
    }
}