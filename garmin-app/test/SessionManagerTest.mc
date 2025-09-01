using Toybox.Test;
using Toybox.Application.Storage;
using Toybox.Time;

(:test)
class SessionManagerTest {
    
    (:test)
    function testInitialize(logger) {
        var sessionManager = new SessionManager();
        Test.assert(!sessionManager.isActive());
        Test.assertEqual(sessionManager.getRemainingTime(), 1500);
        Test.assertEqual(sessionManager.getProgress(), 0.0);
        return true;
    }
    
    (:test)
    function testStartSession(logger) {
        var sessionManager = new SessionManager();
        
        // Test starting a session
        var result = sessionManager.startSession();
        Test.assert(result);
        Test.assert(sessionManager.isActive());
        Test.assert(sessionManager.getRemainingTime() <= 1500);
        
        // Test trying to start another session while active
        var result2 = sessionManager.startSession();
        Test.assert(!result2);
        
        return true;
    }
    
    (:test)
    function testStopSession(logger) {
        var sessionManager = new SessionManager();
        
        // Test stopping without active session
        var result = sessionManager.stopSession();
        Test.assert(!result);
        
        // Start and then stop session
        sessionManager.startSession();
        var stopResult = sessionManager.stopSession();
        Test.assert(stopResult);
        Test.assert(!sessionManager.isActive());
        Test.assertEqual(sessionManager.getRemainingTime(), 1500);
        
        return true;
    }
    
    (:test)
    function testFormattedTime(logger) {
        var sessionManager = new SessionManager();
        
        // Test initial time formatting
        Test.assertEqual(sessionManager.getFormattedTime(), "25:00");
        
        return true;
    }
    
    (:test)
    function testProgress(logger) {
        var sessionManager = new SessionManager();
        
        // Test initial progress
        Test.assertEqual(sessionManager.getProgress(), 0.0);
        
        // Start session - progress should still be near 0
        sessionManager.startSession();
        Test.assert(sessionManager.getProgress() >= 0.0);
        Test.assert(sessionManager.getProgress() <= 0.1); // Allow small progress due to timing
        
        return true;
    }
    
    (:test)
    function testSessionHistory(logger) {
        var sessionManager = new SessionManager();
        
        // Clear existing history
        Storage.deleteValue("session_history");
        
        // Get empty history
        var history = sessionManager.getSessionHistory();
        Test.assertEqual(history.size(), 0);
        
        // Simulate completing a session by calling saveSessionToHistory directly
        var testSession = {
            "startTime" => Time.now().value(),
            "duration" => 1500,
            "completed" => true
        };
        sessionManager.saveSessionToHistory(testSession);
        
        // Check history was saved
        var updatedHistory = sessionManager.getSessionHistory();
        Test.assertEqual(updatedHistory.size(), 1);
        Test.assertEqual(updatedHistory[0]["duration"], 1500);
        Test.assert(updatedHistory[0]["completed"]);
        
        return true;
    }
    
    (:test)
    function testSettingsUpdate(logger) {
        var sessionManager = new SessionManager();
        
        // Test settings update doesn't crash
        sessionManager.updateSettings();
        
        return true;
    }
}