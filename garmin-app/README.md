# Fidan Garmin App

A Garmin Connect IQ widget for the Fidan focus timer application, allowing users to start and monitor Pomodoro sessions directly from their Garmin watch.

## Features

- ✅ Start/stop 25-minute focus sessions from watch
- ✅ Visual tree growth animation during sessions
- ✅ Remaining time display with progress indicator
- ✅ Vibration alerts (start, intervals, completion)
- ✅ Quick glance widget for session status
- ✅ Session history and statistics
- ✅ Works offline without phone connection
- ✅ Syncs sessions with mobile app when connected
- ✅ Battery-efficient implementation

## Supported Devices

The app supports popular Garmin models including:
- Fenix series (6, 7)
- Forerunner series (245, 255, 265, 935, 945, 955, 965)
- Venu series (2, 3)
- Vivoactive series (4, 5)
- Epix Gen 2

## Project Structure

```
garmin-app/
├── manifest.xml              # App configuration and device compatibility
├── resources/
│   ├── drawables/           # Icons and images
│   ├── strings/             # Localized strings
│   └── settings/            # User settings configuration
└── source/
    ├── FidanApp.mc          # Main application entry
    ├── FidanView.mc         # Main UI with tree visualization
    ├── FidanDelegate.mc     # User input handling
    ├── FidanGlance.mc       # Glance view for quick access
    ├── SessionManager.mc    # Timer and session logic
    ├── DataSync.mc          # Phone communication
    ├── StatsView.mc         # Statistics display
    └── AboutView.mc         # About screen
```

## Development Setup

1. Install Garmin Connect IQ SDK:
   ```bash
   # Download from https://developer.garmin.com/connect-iq/sdk/
   ```

2. Install Visual Studio Code with Monkey C extension

3. Build the app:
   ```bash
   monkeyc -d fenix7 -f garmin-app/monkey.jungle -o fidan.prg
   ```

4. Run in simulator:
   ```bash
   connectiq
   # Then load the .prg file in the simulator
   ```

## Key Components

### SessionManager
- Handles 25-minute timer countdown
- Manages session state persistence
- Provides vibration feedback at key moments
- Tracks session history for statistics

### DataSync
- Communicates with mobile app via Connect Mobile
- Queues sessions for sync when offline
- Handles settings synchronization
- Uses efficient message protocol

### Tree Visualization
- Progressive tree growth based on session progress
- Seed → Sapling → Growing tree → Full tree
- Visual feedback for user engagement
- Optimized drawing for battery efficiency

## User Interface

### Main View
- Large tree visualization showing progress
- Timer display (MM:SS format)
- Circular progress indicator
- Start/stop with SELECT button
- Menu access with MENU button

### Glance Widget
- Compact view for quick status check
- Shows remaining time if active
- Mini tree icon with progress
- Tap to open main app

### Statistics View
- Today's completed sessions
- Weekly session count
- Total focus time
- Success rate percentage

## Communication Protocol

The app communicates with the mobile app using these message types:
- `SESSION_START`: Notifies mobile app of session start
- `SESSION_STOP`: Sends early stop information
- `SESSION_COMPLETE`: Confirms successful completion
- `SYNC_REQUEST`: Requests settings/data from phone
- `SETTINGS_UPDATE`: Receives user preferences

## Battery Optimization

- Efficient timer implementation (1-second updates)
- Minimal graphics redraws
- Smart sync scheduling
- Glance view uses static updates
- Background processing only when needed

## Testing

The app includes comprehensive handling for:
- Session persistence across app restarts
- Offline functionality
- Various screen sizes and shapes
- Different Garmin OS versions (3.0.0+)
- Memory constraints on older devices

## Future Enhancements

- [ ] Custom session durations
- [ ] Break timer support
- [ ] More tree species
- [ ] Achievement system
- [ ] Heart rate integration
- [ ] Workout activity integration

## License

Part of the Fidan open-source project. See main repository for license details.