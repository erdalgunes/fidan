using Toybox.Application.Storage;
using Toybox.Application.Properties;
using Toybox.Timer;
using Toybox.Time;
using Toybox.Attention;

class SessionManager {
    public const DEFAULT_SESSION_DURATION = 1500; // 25 minutes in seconds
    private const VIBRATION_DURATION = 100; // milliseconds
    private const VIBRATION_INTENSITY = 25; // vibration strength
    
    private var SESSION_DURATION = DEFAULT_SESSION_DURATION; // Can be changed via settings
    private const STORAGE_KEY_SESSION = "current_session";
    private const STORAGE_KEY_HISTORY = "session_history";
    
    private var _timer;
    private var _sessionActive = false;
    private var _startTime;
    private var _remainingTime;
    private var _updateCallback;

    function initialize() {
        loadSettings();
        loadState();
    }
    
    function loadSettings() {
        var duration = Properties.getValue("sessionDuration");
        if (duration != null && duration > 0) {
            SESSION_DURATION = duration * 60; // Convert minutes to seconds
        }
    }

    function startSession() {
        if (_sessionActive) {
            return false;
        }
        
        _sessionActive = true;
        _startTime = Time.now();
        _remainingTime = SESSION_DURATION;
        
        _timer = new Timer.Timer();
        _timer.start(method(:onTimerTick), 1000, true);
        
        // Vibrate to indicate start (if enabled)
        if (Properties.getValue("vibrationEnabled") != false && Attention has :vibrate) {
            var vibePattern = [
                new Attention.VibeProfile(50, 200),
                new Attention.VibeProfile(0, 100),
                new Attention.VibeProfile(50, 200)
            ];
            Attention.vibrate(vibePattern);
        }
        
        saveState();
        return true;
    }

    function stopSession() {
        if (!_sessionActive) {
            return false;
        }
        
        _sessionActive = false;
        if (_timer != null) {
            _timer.stop();
            _timer = null;
        }
        
        // Save completed session
        var completedSession = {
            "startTime" => _startTime.value(),
            "duration" => SESSION_DURATION - _remainingTime,
            "completed" => _remainingTime <= 0
        };
        saveSessionToHistory(completedSession);
        
        // Vibrate pattern for stop (if enabled)
        if (Properties.getValue("vibrationEnabled") != false && Attention has :vibrate) {
            var vibePattern = [new Attention.VibeProfile(100, 500)];
            Attention.vibrate(vibePattern);
        }
        
        _startTime = null;
        _remainingTime = SESSION_DURATION;
        saveState();
        return true;
    }

    function onTimerTick() {
        if (!_sessionActive) {
            return;
        }
        
        _remainingTime--;
        
        if (_remainingTime <= 0) {
            // Session completed successfully
            completeSession();
        } else if (_remainingTime % 300 == 0) {
            // Vibrate every 5 minutes (if interval alerts enabled)
            if (Properties.getValue("intervalAlerts") != false && 
                Properties.getValue("vibrationEnabled") != false && 
                Attention has :vibrate) {
                var vibePattern = [new Attention.VibeProfile(VIBRATION_INTENSITY, VIBRATION_DURATION)];
                Attention.vibrate(vibePattern);
            }
        }
        
        if (_updateCallback != null) {
            _updateCallback.invoke();
        }
    }

    function completeSession() {
        _sessionActive = false;
        if (_timer != null) {
            _timer.stop();
            _timer = null;
        }
        
        // Strong vibration for completion (if enabled)
        if (Properties.getValue("vibrationEnabled") != false && Attention has :vibrate) {
            var vibePattern = [
                new Attention.VibeProfile(100, 300),
                new Attention.VibeProfile(0, 200),
                new Attention.VibeProfile(100, 300),
                new Attention.VibeProfile(0, 200),
                new Attention.VibeProfile(100, 500)
            ];
            Attention.vibrate(vibePattern);
        }
        
        // Play tone if available and enabled
        if (Properties.getValue("soundEnabled") != false && Attention has :playTone) {
            Attention.playTone(Attention.TONE_SUCCESS);
        }
        
        var completedSession = {
            "startTime" => _startTime.value(),
            "duration" => SESSION_DURATION,
            "completed" => true
        };
        saveSessionToHistory(completedSession);
        
        _startTime = null;
        _remainingTime = SESSION_DURATION;
        saveState();
    }

    function getRemainingTime() {
        return _remainingTime;
    }

    function getFormattedTime() {
        var minutes = _remainingTime / 60;
        var seconds = _remainingTime % 60;
        return minutes.format("%02d") + ":" + seconds.format("%02d");
    }

    function isActive() {
        return _sessionActive;
    }

    function getProgress() {
        if (!_sessionActive) {
            return 0.0;
        }
        return 1.0 - (_remainingTime.toFloat() / SESSION_DURATION.toFloat());
    }

    function setUpdateCallback(callback) {
        _updateCallback = callback;
    }

    function saveState() {
        var state = {
            "active" => _sessionActive,
            "startTime" => _startTime != null ? _startTime.value() : null,
            "remainingTime" => _remainingTime
        };
        Storage.setValue(STORAGE_KEY_SESSION, state);
    }

    function loadState() {
        var state = Storage.getValue(STORAGE_KEY_SESSION);
        if (state != null) {
            _sessionActive = state.get("active");
            var startTimeValue = state.get("startTime");
            _startTime = startTimeValue != null ? new Time.Moment(startTimeValue) : null;
            _remainingTime = state.get("remainingTime");
            
            // Resume timer if session was active
            if (_sessionActive && _remainingTime > 0) {
                _timer = new Timer.Timer();
                _timer.start(method(:onTimerTick), 1000, true);
            }
        } else {
            _sessionActive = false;
            _remainingTime = SESSION_DURATION;
        }
    }

    function saveSessionToHistory(session) {
        var history = Storage.getValue(STORAGE_KEY_HISTORY);
        if (history == null) {
            history = [];
        }
        
        // Check memory status and adjust history limit accordingly
        var memoryMonitor = MemoryMonitor.getInstance();
        var memoryStatus = memoryMonitor.checkMemoryStatus();
        var maxSessions = 50;
        
        if (memoryStatus == :critical) {
            maxSessions = 10;
        } else if (memoryStatus == :low) {
            maxSessions = 20;
        } else if (memoryMonitor.isLowEndDevice()) {
            maxSessions = 30;
        }
        
        // Keep only specified number of recent sessions
        if (history.size() >= maxSessions) {
            history = history.slice(1, null);
        }
        
        history.add(session);
        Storage.setValue(STORAGE_KEY_HISTORY, history);
        
        // Perform cleanup if needed
        memoryMonitor.performCleanup(memoryStatus);
    }

    function getSessionHistory() {
        var history = Storage.getValue(STORAGE_KEY_HISTORY);
        return history != null ? history : [];
    }

    function updateSettings() {
        // Reload settings when they change
        loadSettings();
    }
}