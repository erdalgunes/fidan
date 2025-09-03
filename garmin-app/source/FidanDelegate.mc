using Toybox.WatchUi;
using Toybox.System;

class FidanDelegate extends WatchUi.BehaviorDelegate {
    private var _sessionManager;
    private var _view;

    function initialize(sessionManager, view) {
        BehaviorDelegate.initialize();
        _sessionManager = sessionManager;
        _view = view;
    }

    function onSelect() {
        if (_sessionManager.isActive()) {
            // Show confirmation dialog to cancel session (no pause allowed)
            var dialog = new WatchUi.Confirmation("Cancel session?\n(No credit given)");
            WatchUi.pushView(dialog, new StopConfirmationDelegate(_sessionManager), WatchUi.SLIDE_IMMEDIATE);
        } else {
            // Start new session
            if (_sessionManager.startSession()) {
                WatchUi.requestUpdate();
            }
        }
        return true;
    }

    function onBack() {
        if (_sessionManager.isActive()) {
            // Don't exit if session is active, show warning instead
            var dialog = new WatchUi.Confirmation("Session in progress. Exit anyway?");
            WatchUi.pushView(dialog, new ExitConfirmationDelegate(), WatchUi.SLIDE_IMMEDIATE);
            return true;
        }
        return false;
    }

    function onMenu() {
        var menu = new WatchUi.Menu2({:title => "Fidan"});
        menu.addItem(new WatchUi.MenuItem("Statistics", "View your progress", "stats", {}));
        menu.addItem(new WatchUi.MenuItem("Settings", "Configure app", "settings", {}));
        menu.addItem(new WatchUi.MenuItem("About", "App information", "about", {}));
        
        WatchUi.pushView(menu, new MenuDelegate(_sessionManager), WatchUi.SLIDE_UP);
        return true;
    }
}

class StopConfirmationDelegate extends WatchUi.ConfirmationDelegate {
    private var _sessionManager;

    function initialize(sessionManager) {
        ConfirmationDelegate.initialize();
        _sessionManager = sessionManager;
    }

    function onResponse(response) {
        if (response == WatchUi.CONFIRM_YES) {
            _sessionManager.stopSession();
            WatchUi.requestUpdate();
        }
    }
}

class ExitConfirmationDelegate extends WatchUi.ConfirmationDelegate {
    function initialize() {
        ConfirmationDelegate.initialize();
    }

    function onResponse(response) {
        if (response == WatchUi.CONFIRM_YES) {
            WatchUi.popView(WatchUi.SLIDE_IMMEDIATE);
        }
    }
}

class MenuDelegate extends WatchUi.Menu2InputDelegate {
    private var _sessionManager;

    function initialize(sessionManager) {
        Menu2InputDelegate.initialize();
        _sessionManager = sessionManager;
    }

    function onSelect(item) {
        var id = item.getId();
        
        if (id.equals("stats")) {
            var view = new StatsView(_sessionManager);
            WatchUi.pushView(view, new StatsDelegate(), WatchUi.SLIDE_UP);
        } else if (id.equals("settings")) {
            var view = new SettingsView();
            WatchUi.pushView(view, new SettingsDelegate(view), WatchUi.SLIDE_UP);
        } else if (id.equals("about")) {
            var view = new AboutView();
            WatchUi.pushView(view, new WatchUi.BehaviorDelegate(), WatchUi.SLIDE_UP);
        }
    }

    function onBack() {
        WatchUi.popView(WatchUi.SLIDE_DOWN);
    }
}