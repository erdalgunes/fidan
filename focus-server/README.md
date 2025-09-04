# Fidan Focus Server

A Python WebSocket server using FastAPI and Socket.IO for real-time collaborative focus sessions.

## Features

- **Room-based Sessions**: Users can create/join focus sessions using 6-character room codes
- **Real-time Synchronization**: Live timer updates, participant status, and session events
- **Accountability**: Track who's focusing, paused, or left during sessions
- **Scalable**: Built with FastAPI for high performance and async support
- **Cloud Ready**: Docker support for easy hcloud deployment

## API Endpoints

### REST API
- `GET /health` - Health check endpoint
- `GET /session/{room_code}` - Get session information

### Socket.IO Events

**Client → Server:**
- `create_session` - Create new focus session
- `join_session` - Join existing session by room code  
- `start_session` - Start the focus timer (creator only)
- `update_status` - Update user status (focusing, paused, failed)

**Server → Client:**
- `session_created` - Session created successfully
- `session_joined` - Successfully joined session
- `participant_joined` - New participant joined
- `participant_left` - Participant disconnected
- `session_started` - Focus session timer started
- `time_update` - Timer countdown updates (every second)
- `session_completed` - Session finished
- `participant_status_updated` - Participant changed status

## Quick Start

### Local Development

```bash
# Install dependencies
pip install -r requirements.txt

# Run server
python server.py
```

### Docker

```bash
# Build and run
docker-compose up --build

# Or use Docker directly
docker build -t fidan-focus-server .
docker run -p 3000:3000 fidan-focus-server
```

### hcloud Deployment

1. Build and push to container registry:
```bash
docker build -t your-registry.com/fidan-focus-server .
docker push your-registry.com/fidan-focus-server
```

2. Deploy to hcloud server:
```bash
# SSH to your hcloud instance
ssh root@your-server-ip

# Pull and run container
docker pull your-registry.com/fidan-focus-server
docker run -d -p 3000:3000 --name focus-server your-registry.com/fidan-focus-server
```

## Usage Flow

1. **Create Session**: User creates session → Gets room code (e.g., "ABC123")
2. **Invite Friends**: Share room code with others  
3. **Join Session**: Friends join using room code
4. **Start Focus**: Session creator starts 25-minute timer
5. **Focus Together**: All participants see live timer + status updates
6. **Accountability**: System tracks if users leave app/fail session
7. **Complete**: Session ends, results shared with all participants

## Configuration

Environment variables:
- `PORT` - Server port (default: 3000)
- `PYTHONUNBUFFERED` - Enable real-time logging

## Architecture

```
Android App ↔ Socket.IO ↔ FastAPI Server ↔ In-Memory Storage
     ↓              ↓            ↓              ↓
- WebSocket    - Real-time    - REST API    - Sessions
- Client       - Events      - Health       - Users  
- UI          - Rooms        - Metrics      - Timers
```

## Scalability Notes

- **Current**: In-memory storage (single server)
- **Production**: Add Redis for session storage + horizontal scaling
- **Monitoring**: Add metrics endpoint for session analytics
- **Auth**: Add user authentication for persistent profiles