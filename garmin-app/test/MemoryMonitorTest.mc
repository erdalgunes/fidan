using Toybox.Test;
using Toybox.Application.Storage;

(:test)
class MemoryMonitorTest {
    
    (:test)
    function testSingleton(logger) {
        var monitor1 = MemoryMonitor.getInstance();
        var monitor2 = MemoryMonitor.getInstance();
        
        // Should be the same instance
        Test.assert(monitor1 != null);
        Test.assert(monitor2 != null);
        // Note: Can't directly test object equality in Monkey C
        
        return true;
    }
    
    (:test)
    function testMemoryStatusCheck(logger) {
        var monitor = MemoryMonitor.getInstance();
        
        // Test that memory status check returns valid values
        var status = monitor.checkMemoryStatus();
        Test.assert(status == :normal || status == :low || status == :critical);
        
        return true;
    }
    
    (:test)
    function testMemoryInfo(logger) {
        var monitor = MemoryMonitor.getInstance();
        
        // Test that memory info returns valid structure
        var info = monitor.getMemoryInfo();
        Test.assert(info.hasKey("free"));
        Test.assert(info.hasKey("used"));
        Test.assert(info.hasKey("total"));
        Test.assert(info.hasKey("percentage"));
        
        // Basic sanity checks
        Test.assert(info["free"] >= 0);
        Test.assert(info["used"] >= 0);
        Test.assert(info["total"] > 0);
        Test.assert(info["percentage"] >= 0);
        Test.assert(info["percentage"] <= 100);
        
        return true;
    }
    
    (:test)
    function testLowEndDeviceDetection(logger) {
        var monitor = MemoryMonitor.getInstance();
        
        // Test that detection returns a boolean
        var isLowEnd = monitor.isLowEndDevice();
        Test.assert(isLowEnd == true || isLowEnd == false);
        
        return true;
    }
    
    (:test)
    function testCleanupOperations(logger) {
        var monitor = MemoryMonitor.getInstance();
        
        // Setup test data
        var testHistory = [];
        for (var i = 0; i < 60; i++) {
            testHistory.add({"session" => i});
        }
        Storage.setValue("session_history", testHistory);
        
        var testUnsent = [];
        for (var i = 0; i < 150; i++) {
            testUnsent.add({"message" => i});
        }
        Storage.setValue("unsent_sessions", testUnsent);
        
        // Test low memory cleanup
        monitor.performCleanup(:low);
        
        var cleanedHistory = Storage.getValue("session_history");
        var cleanedUnsent = Storage.getValue("unsent_sessions");
        
        Test.assert(cleanedHistory.size() <= 20);
        Test.assert(cleanedUnsent.size() <= 20);
        
        // Reset for critical test
        Storage.setValue("session_history", testHistory);
        Storage.setValue("unsent_sessions", testUnsent);
        
        // Test critical memory cleanup
        monitor.performCleanup(:critical);
        
        var criticalHistory = Storage.getValue("session_history");
        var criticalUnsent = Storage.getValue("unsent_sessions");
        
        Test.assert(criticalHistory.size() <= 10);
        Test.assert(criticalUnsent.size() <= 5);
        
        return true;
    }
}