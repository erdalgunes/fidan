using Toybox.Application.Storage;
using Toybox.Timer;
using Toybox.Time;
using Toybox.Attention;

class SessionManager {
    private const SESSION_DURATION = 1500; // 25 minutes in seconds
    private const STORAGE_KEY_SESSION = "current_session";
    private const STORAGE_KEY_HISTORY = "session_history";
    
    private var _timer;
    private var _sessionActive = false;
    private var _startTime;
    private var _remainingTime;
    private var _updateCallback;

    function initialize() {
        loadState();
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
        
        // Vibrate to indicate start
        if (Attention has :vibrate) {
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
        
        // Vibrate pattern for stop
        if (Attention has :vibrate) {
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
            // Vibrate every 5 minutes
            if (Attention has :vibrate) {
                var vibePattern = [new Attention.VibeProfile(25, 100)];
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
        
        // Strong vibration for completion
        if (Attention has :vibrate) {
            var vibePattern = [
                new Attention.VibeProfile(100, 300),
                new Attention.VibeProfile(0, 200),
                new Attention.VibeProfile(100, 300),
                new Attention.VibeProfile(0, 200),
                new Attention.VibeProfile(100, 500)
            ];
            Attention.vibrate(vibePattern);
        }
        
        // Play tone if available
        if (Attention has :playTone) {
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
        
        // Keep only last 50 sessions
        if (history.size() >= 50) {
            history = history.slice(1, null);
        }
        
        history.add(session);
        Storage.setValue(STORAGE_KEY_HISTORY, history);
    }

    function getSessionHistory() {
        var history = Storage.getValue(STORAGE_KEY_HISTORY);
        return history != null ? history : [];
    }

    function updateSettings() {
        // Future: Handle user preferences from settings
    }
}