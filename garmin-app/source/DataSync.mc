using Toybox.Communications;
using Toybox.Application.Storage;
using Toybox.WatchUi;
using Toybox.Timer;

class DataSync {
    private const MSG_TYPE_SESSION_START = 1;
    private const MSG_TYPE_SESSION_STOP = 2;
    private const MSG_TYPE_SESSION_COMPLETE = 3;
    private const MSG_TYPE_SYNC_REQUEST = 4;
    private const MSG_TYPE_SETTINGS_UPDATE = 5;
    
    // Retry configuration
    private const MAX_RETRY_ATTEMPTS = 5;
    private const INITIAL_RETRY_DELAY = 2000; // 2 seconds in milliseconds
    private const MAX_RETRY_DELAY = 60000; // 60 seconds max delay
    
    private var _pendingSync = [];
    private var _retryQueue = {};
    private var _retryTimer;

    function initialize() {
        // Check if phone is connected
        if (System.getDeviceSettings().phoneConnected) {
            syncWithPhone();
        }
    }

    function sendSessionStart(startTime) {
        var message = {
            "type" => MSG_TYPE_SESSION_START,
            "timestamp" => startTime.value(),
            "deviceId" => System.getDeviceSettings().uniqueIdentifier
        };
        
        sendMessage(message);
    }

    function sendSessionStop(startTime, duration) {
        var message = {
            "type" => MSG_TYPE_SESSION_STOP,
            "timestamp" => startTime.value(),
            "duration" => duration,
            "deviceId" => System.getDeviceSettings().uniqueIdentifier
        };
        
        sendMessage(message);
    }

    function sendSessionComplete(startTime) {
        var message = {
            "type" => MSG_TYPE_SESSION_COMPLETE,
            "timestamp" => startTime.value(),
            "duration" => 1500, // 25 minutes
            "deviceId" => System.getDeviceSettings().uniqueIdentifier
        };
        
        sendMessage(message);
    }

    function syncWithPhone() {
        // Get unsent sessions from storage
        var unsentSessions = Storage.getValue("unsent_sessions");
        if (unsentSessions != null && unsentSessions.size() > 0) {
            for (var i = 0; i < unsentSessions.size(); i++) {
                sendMessage(unsentSessions[i]);
            }
        }
        
        // Request latest settings from phone
        var syncRequest = {
            "type" => MSG_TYPE_SYNC_REQUEST,
            "deviceId" => System.getDeviceSettings().uniqueIdentifier
        };
        sendMessage(syncRequest);
    }

    function sendMessage(message) {
        if (System.getDeviceSettings().phoneConnected) {
            // Add message ID for tracking retries
            if (!message.hasKey("messageId")) {
                message["messageId"] = System.getTimer();
            }
            
            Communications.transmit(
                message,
                null,
                new CommListener(method(:onTransmitComplete), method(:onTransmitError), message)
            );
        } else {
            // Store for later sync
            queueForSync(message);
        }
    }

    function queueForSync(message) {
        var unsentSessions = Storage.getValue("unsent_sessions");
        if (unsentSessions == null) {
            unsentSessions = [];
        }
        
        unsentSessions.add(message);
        
        // Keep only last 100 messages
        if (unsentSessions.size() > 100) {
            unsentSessions = unsentSessions.slice(-100, null);
        }
        
        Storage.setValue("unsent_sessions", unsentSessions);
    }

    function onTransmitComplete(message) {
        // Remove from retry queue if exists
        if (message.hasKey("messageId")) {
            _retryQueue.remove(message["messageId"]);
        }
        
        // Remove from unsent queue if exists
        var unsentSessions = Storage.getValue("unsent_sessions");
        if (unsentSessions != null) {
            var newUnsent = [];
            for (var i = 0; i < unsentSessions.size(); i++) {
                if (!messagesEqual(unsentSessions[i], message)) {
                    newUnsent.add(unsentSessions[i]);
                }
            }
            Storage.setValue("unsent_sessions", newUnsent);
        }
    }

    /**
     * Handles transmission errors with exponential backoff retry logic
     * 
     * Retry schedule:
     * - Attempt 1: 2s delay
     * - Attempt 2: 4s delay (±10% jitter)
     * - Attempt 3: 8s delay (±10% jitter)
     * - Attempt 4: 16s delay (±10% jitter)
     * - Attempt 5: 32s delay (±10% jitter, capped at 60s)
     * - After 5 attempts: Store message for later sync when connection available
     * 
     * Jitter prevents thundering herd problem when multiple messages fail simultaneously
     * 
     * @param message The message that failed to transmit
     */
    function onTransmitError(message) {
        // Ensure message has unique ID for tracking
        if (!message.hasKey("messageId")) {
            message["messageId"] = System.getTimer();
        }
        
        var messageId = message["messageId"];
        var retryInfo = _retryQueue.get(messageId);
        
        // Initialize retry tracking for new message
        if (retryInfo == null) {
            retryInfo = {
                "message" => message,
                "attempts" => 0,
                "nextDelay" => INITIAL_RETRY_DELAY
            };
        }
        
        retryInfo["attempts"]++;
        
        if (retryInfo["attempts"] < MAX_RETRY_ATTEMPTS) {
            // Add to retry queue and schedule retry
            _retryQueue[messageId] = retryInfo;
            scheduleRetry(messageId, retryInfo["nextDelay"]);
            
            // Calculate next delay with exponential backoff and jitter
            var nextDelay = retryInfo["nextDelay"] * 2;
            if (nextDelay > MAX_RETRY_DELAY) {
                nextDelay = MAX_RETRY_DELAY;
            }
            // Add random jitter (±10%) to prevent synchronized retries
            var jitter = (nextDelay * 0.2 * Math.rand()) - (nextDelay * 0.1);
            retryInfo["nextDelay"] = nextDelay + jitter.toNumber();
        } else {
            // Max retries exceeded - fallback to persistent storage for later sync
            queueForSync(message);
            _retryQueue.remove(messageId);
        }
    }
    
    function scheduleRetry(messageId, delay) {
        if (_retryTimer != null) {
            _retryTimer.stop();
        }
        _retryTimer = new Timer.Timer();
        _retryTimer.start(method(:processRetry), delay, false);
    }
    
    function processRetry() {
        // Process all pending retries
        var keys = _retryQueue.keys();
        for (var i = 0; i < keys.size(); i++) {
            var retryInfo = _retryQueue[keys[i]];
            if (retryInfo != null && System.getDeviceSettings().phoneConnected) {
                sendMessage(retryInfo["message"]);
            }
        }
    }

    function handleBackgroundData(data) {
        if (data == null) {
            return;
        }
        
        var msgType = data.get("type");
        
        if (msgType == MSG_TYPE_SETTINGS_UPDATE) {
            // Update local settings
            var settings = data.get("settings");
            if (settings != null) {
                Storage.setValue("app_settings", settings);
                // Notify session manager of settings change
                Application.getApp().onSettingsChanged();
            }
        }
    }

    function messagesEqual(msg1, msg2) {
        if (msg1 == null || msg2 == null) {
            return false;
        }
        
        return msg1.get("type") == msg2.get("type") &&
               msg1.get("timestamp") == msg2.get("timestamp") &&
               msg1.get("deviceId") == msg2.get("deviceId");
    }
}

class CommListener extends Communications.ConnectionListener {
    private var _successCallback;
    private var _errorCallback;
    private var _message;

    function initialize(successCallback, errorCallback, message) {
        Communications.ConnectionListener.initialize();
        _successCallback = successCallback;
        _errorCallback = errorCallback;
        _message = message;
    }

    function onComplete() {
        if (_successCallback != null) {
            _successCallback.invoke(_message);
        }
    }

    function onError() {
        if (_errorCallback != null) {
            _errorCallback.invoke(_message);
        }
    }
}