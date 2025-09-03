using Toybox.Test;
using Toybox.Application.Storage;
using Toybox.Time;

(:test)
class DataSyncTest {
    
    (:test)
    function testInitialization(logger) {
        var dataSync = new DataSync();
        Test.assert(dataSync != null);
        return true;
    }
    
    (:test)
    function testQueueForSync(logger) {
        var dataSync = new DataSync();
        
        // Clear existing unsent sessions
        Storage.deleteValue("unsent_sessions");
        
        // Create test message
        var testMessage = {
            "type" => 1,
            "timestamp" => Time.now().value(),
            "deviceId" => "test_device"
        };
        
        // Queue message
        dataSync.queueForSync(testMessage);
        
        // Verify message was queued
        var unsentSessions = Storage.getValue("unsent_sessions");
        Test.assert(unsentSessions != null);
        Test.assertEqual(unsentSessions.size(), 1);
        Test.assertEqual(unsentSessions[0]["type"], 1);
        
        return true;
    }
    
    (:test)
    function testQueueSizeLimit(logger) {
        var dataSync = new DataSync();
        
        // Clear existing unsent sessions
        Storage.deleteValue("unsent_sessions");
        
        // Add 105 messages (should be limited to 100)
        for (var i = 0; i < 105; i++) {
            var testMessage = {
                "type" => 1,
                "timestamp" => Time.now().value() + i,
                "deviceId" => "test_device"
            };
            dataSync.queueForSync(testMessage);
        }
        
        // Verify size limit was enforced
        var unsentSessions = Storage.getValue("unsent_sessions");
        Test.assert(unsentSessions != null);
        Test.assertEqual(unsentSessions.size(), 100);
        
        return true;
    }
    
    (:test)
    function testMessagesEqual(logger) {
        var dataSync = new DataSync();
        var timestamp = Time.now().value();
        
        var msg1 = {
            "type" => 1,
            "timestamp" => timestamp,
            "deviceId" => "device1"
        };
        
        var msg2 = {
            "type" => 1,
            "timestamp" => timestamp,
            "deviceId" => "device1"
        };
        
        var msg3 = {
            "type" => 2,
            "timestamp" => timestamp,
            "deviceId" => "device1"
        };
        
        // Test equal messages
        Test.assert(dataSync.messagesEqual(msg1, msg2));
        
        // Test different messages
        Test.assert(!dataSync.messagesEqual(msg1, msg3));
        
        // Test null messages
        Test.assert(!dataSync.messagesEqual(msg1, null));
        Test.assert(!dataSync.messagesEqual(null, msg1));
        Test.assert(!dataSync.messagesEqual(null, null));
        
        return true;
    }
    
    (:test)
    function testHandleBackgroundData(logger) {
        var dataSync = new DataSync();
        
        // Test null data
        dataSync.handleBackgroundData(null);
        
        // Test settings update message
        var settingsData = {
            "type" => 5, // MSG_TYPE_SETTINGS_UPDATE
            "settings" => {
                "vibrationEnabled" => true,
                "sessionDuration" => 30
            }
        };
        
        dataSync.handleBackgroundData(settingsData);
        
        // Verify settings were stored
        var storedSettings = Storage.getValue("app_settings");
        Test.assert(storedSettings != null);
        Test.assert(storedSettings["vibrationEnabled"]);
        Test.assertEqual(storedSettings["sessionDuration"], 30);
        
        return true;
    }
}