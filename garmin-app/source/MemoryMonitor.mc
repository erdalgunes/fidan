using Toybox.System;
using Toybox.Application.Storage;

// Memory monitoring utility for low-end devices
class MemoryMonitor {
    private const MEMORY_THRESHOLD_LOW = 20480;    // 20KB threshold for cleanup
    private const MEMORY_THRESHOLD_CRITICAL = 10240; // 10KB threshold for aggressive cleanup
    private const STORAGE_KEY_MEMORY_STATS = "memory_stats";
    
    private static var _instance;
    
    static function getInstance() {
        if (_instance == null) {
            _instance = new MemoryMonitor();
        }
        return _instance;
    }
    
    function initialize() {
        // Private constructor for singleton
    }
    
    // Check current memory status and return level
    function checkMemoryStatus() {
        var stats = System.getSystemStats();
        var freeMemory = stats.freeMemory;
        var usedMemory = stats.usedMemory;
        var totalMemory = stats.totalMemory;
        
        // Log memory stats for debugging (only if debug build)
        if (System.getDeviceSettings().monkeyVersion[0] != null) {
            var memStats = {
                "free" => freeMemory,
                "used" => usedMemory,  
                "total" => totalMemory,
                "timestamp" => System.getTimer()
            };
            Storage.setValue(STORAGE_KEY_MEMORY_STATS, memStats);
        }
        
        if (freeMemory < MEMORY_THRESHOLD_CRITICAL) {
            return :critical;
        } else if (freeMemory < MEMORY_THRESHOLD_LOW) {
            return :low;
        } else {
            return :normal;
        }
    }
    
    // Perform memory cleanup based on current status
    function performCleanup(level) {
        switch (level) {
            case :critical:
                performCriticalCleanup();
                break;
            case :low:
                performLowMemoryCleanup();
                break;
            default:
                // No cleanup needed
                break;
        }
    }
    
    // Light cleanup for low memory situations
    private function performLowMemoryCleanup() {
        // Clean up old session history (keep only last 20 instead of 50)
        var history = Storage.getValue("session_history");
        if (history != null && history.size() > 20) {
            var trimmedHistory = history.slice(-20, null);
            Storage.setValue("session_history", trimmedHistory);
        }
        
        // Clean up old unsent messages (keep only last 20 instead of 100)
        var unsentSessions = Storage.getValue("unsent_sessions");
        if (unsentSessions != null && unsentSessions.size() > 20) {
            var trimmedUnsent = unsentSessions.slice(-20, null);
            Storage.setValue("unsent_sessions", trimmedUnsent);
        }
    }
    
    // Aggressive cleanup for critical memory situations
    private function performCriticalCleanup() {
        // Perform low memory cleanup first
        performLowMemoryCleanup();
        
        // More aggressive trimming
        var history = Storage.getValue("session_history");
        if (history != null && history.size() > 10) {
            var trimmedHistory = history.slice(-10, null);
            Storage.setValue("session_history", trimmedHistory);
        }
        
        var unsentSessions = Storage.getValue("unsent_sessions");
        if (unsentSessions != null && unsentSessions.size() > 5) {
            var trimmedUnsent = unsentSessions.slice(-5, null);
            Storage.setValue("unsent_sessions", trimmedUnsent);
        }
        
        // Clear memory stats to free up space
        Storage.deleteValue(STORAGE_KEY_MEMORY_STATS);
    }
    
    // Get memory usage information for debugging
    function getMemoryInfo() {
        var stats = System.getSystemStats();
        return {
            "free" => stats.freeMemory,
            "used" => stats.usedMemory,
            "total" => stats.totalMemory,
            "percentage" => (stats.usedMemory * 100 / stats.totalMemory).toNumber()
        };
    }
    
    // Check if device is considered low-end based on total memory
    function isLowEndDevice() {
        var stats = System.getSystemStats();
        return stats.totalMemory < 65536; // Less than 64KB total memory
    }
}