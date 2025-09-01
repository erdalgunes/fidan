using Toybox.Communications;
using Toybox.Application.Storage;
using Toybox.WatchUi;

class DataSync {
    private const MSG_TYPE_SESSION_START = 1;
    private const MSG_TYPE_SESSION_STOP = 2;
    private const MSG_TYPE_SESSION_COMPLETE = 3;
    private const MSG_TYPE_SYNC_REQUEST = 4;
    private const MSG_TYPE_SETTINGS_UPDATE = 5;
    
    private var _pendingSync = [];

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

    function onTransmitError(message) {
        // Keep in queue for retry
        queueForSync(message);
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