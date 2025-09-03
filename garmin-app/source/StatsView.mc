using Toybox.WatchUi;
using Toybox.Graphics;
using Toybox.Time;
using Toybox.Time.Gregorian;

class StatsView extends WatchUi.View {
    private const LINE_SPACING = 25;
    
    private var _sessionManager;
    private var _stats;

    function initialize(sessionManager) {
        View.initialize();
        _sessionManager = sessionManager;
        calculateStats();
    }

    function calculateStats() {
        var history = _sessionManager.getSessionHistory();
        _stats = {
            "total" => history.size(),
            "completed" => 0,
            "totalTime" => 0,
            "todayCount" => 0,
            "weekCount" => 0
        };
        
        var now = Time.now();
        var todayStart = Time.today();
        var weekStart = now.subtract(new Time.Duration(7 * 24 * 60 * 60));
        
        for (var i = 0; i < history.size(); i++) {
            var session = history[i];
            var sessionTime = new Time.Moment(session.get("startTime"));
            
            if (session.get("completed")) {
                _stats["completed"]++;
            }
            
            _stats["totalTime"] += session.get("duration");
            
            if (sessionTime.greaterThan(todayStart)) {
                _stats["todayCount"]++;
            }
            
            if (sessionTime.greaterThan(weekStart)) {
                _stats["weekCount"]++;
            }
        }
    }

    function onLayout(dc) {
    }

    function onUpdate(dc) {
        dc.setColor(Graphics.COLOR_BLACK, Graphics.COLOR_BLACK);
        dc.clear();
        
        var width = dc.getWidth();
        var centerX = width / 2;
        var y = 30;
        
        // Title
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
        dc.drawText(centerX, y, Graphics.FONT_SMALL, "Statistics", Graphics.TEXT_JUSTIFY_CENTER);
        y += 35;
        
        // Today's sessions
        drawStatLine(dc, 20, y, "Today:", _stats["todayCount"].toString() + " sessions");
        y += LINE_SPACING;
        
        // This week
        drawStatLine(dc, 20, y, "Week:", _stats["weekCount"].toString() + " sessions");
        y += LINE_SPACING;
        
        // Total completed
        drawStatLine(dc, 20, y, "Completed:", _stats["completed"].toString() + "/" + _stats["total"].toString());
        y += LINE_SPACING;
        
        // Total focus time
        var hours = _stats["totalTime"] / 3600;
        var minutes = (_stats["totalTime"] % 3600) / 60;
        var timeStr = hours.toString() + "h " + minutes.toString() + "m";
        drawStatLine(dc, 20, y, "Total Time:", timeStr);
        y += LINE_SPACING;
        
        // Success rate
        if (_stats["total"] > 0) {
            var successRate = (_stats["completed"] * 100) / _stats["total"];
            drawStatLine(dc, 20, y, "Success:", successRate.toString() + "%");
        }
    }

    function drawStatLine(dc, x, y, label, value) {
        dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.drawText(x, y, Graphics.FONT_XTINY, label, Graphics.TEXT_JUSTIFY_LEFT);
        
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
        dc.drawText(x + 80, y, Graphics.FONT_XTINY, value, Graphics.TEXT_JUSTIFY_LEFT);
    }

    function onShow() {
    }

    function onHide() {
    }
}

class StatsDelegate extends WatchUi.BehaviorDelegate {
    function initialize() {
        BehaviorDelegate.initialize();
    }

    function onBack() {
        WatchUi.popView(WatchUi.SLIDE_DOWN);
        return true;
    }
}