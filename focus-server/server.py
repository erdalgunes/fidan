import asyncio
import json
import random
import string
import time
from datetime import datetime, timedelta
from typing import Dict, List, Optional
from uuid import uuid4

import socketio
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

# Create FastAPI app
app = FastAPI(title="Fidan Focus Server", version="1.0.0")

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Create Socket.IO server
sio = socketio.AsyncServer(
    async_mode='asgi',
    cors_allowed_origins="*",
    logger=True,
    engineio_logger=True
)

# Mount Socket.IO app to FastAPI
socket_app = socketio.ASGIApp(sio, app)

# Data models
class Participant(BaseModel):
    id: str
    user_name: str
    status: str = "ready"  # ready, focusing, paused, completed, failed
    joined_at: float
    
class FocusSession(BaseModel):
    id: str
    room_code: str
    created_by: str
    participants: List[Participant] = []
    max_participants: int = 4
    status: str = "waiting"  # waiting, active, completed
    session_duration: int = 25 * 60 * 1000  # 25 minutes in ms
    time_left_ms: int = 25 * 60 * 1000
    started_at: Optional[float] = None
    created_at: float
    last_update: float

# In-memory storage
active_sessions: Dict[str, FocusSession] = {}
user_sessions: Dict[str, str] = {}  # socket_id -> session_id
session_timers: Dict[str, asyncio.Task] = {}  # session_id -> timer_task

def generate_room_code() -> str:
    """Generate a 6-character room code"""
    return ''.join(random.choices(string.ascii_uppercase + string.digits, k=6))

def sanitize_session(session: FocusSession, user_id: str = "") -> dict:
    """Remove sensitive data and add user-specific info"""
    return {
        "id": session.id,
        "room_code": session.room_code,
        "participants": [
            {
                "id": p.id,
                "user_name": p.user_name,
                "status": p.status,
                "joined_at": p.joined_at
            }
            for p in session.participants
        ],
        "max_participants": session.max_participants,
        "status": session.status,
        "session_duration": session.session_duration,
        "time_left_ms": session.time_left_ms,
        "started_at": session.started_at,
        "created_at": session.created_at,
        "is_creator": session.created_by == user_id
    }

async def session_countdown(session_id: str):
    """Handle session countdown timer"""
    try:
        while session_id in active_sessions:
            session = active_sessions[session_id]
            
            if session.status != "active":
                break
                
            now = time.time() * 1000  # Convert to milliseconds
            elapsed = now - session.started_at if session.started_at else 0
            session.time_left_ms = max(0, session.session_duration - elapsed)
            session.last_update = now
            
            # Broadcast time update
            await sio.emit('time_update', {
                'time_left_ms': session.time_left_ms,
                'elapsed': elapsed
            }, room=session.room_code)
            
            # Check if session completed
            if session.time_left_ms <= 0:
                session.status = "completed"
                for participant in session.participants:
                    if participant.status == "focusing":
                        participant.status = "completed"
                
                await sio.emit('session_completed', {
                    'session': sanitize_session(session)
                }, room=session.room_code)
                
                # Clean up after 5 minutes
                await asyncio.sleep(300)  # 5 minutes
                if session_id in active_sessions:
                    del active_sessions[session_id]
                    print(f"Session {session.room_code} cleaned up after completion")
                
                break
            
            await asyncio.sleep(1)  # Update every second
            
    except asyncio.CancelledError:
        print(f"Session countdown cancelled for {session_id}")
    except Exception as e:
        print(f"Error in session countdown: {e}")

# FastAPI endpoints
@app.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "active_sessions": len(active_sessions),
        "timestamp": datetime.now().isoformat()
    }

@app.get("/session/{room_code}")
async def get_session_info(room_code: str):
    room_code = room_code.upper()
    session = None
    for s in active_sessions.values():
        if s.room_code == room_code:
            session = s
            break
    
    if not session:
        raise HTTPException(status_code=404, detail="Session not found")
    
    return {
        "room_code": session.room_code,
        "participants": len(session.participants),
        "max_participants": session.max_participants,
        "status": session.status,
        "time_left_ms": session.time_left_ms,
        "created_at": session.created_at
    }

# Socket.IO event handlers
@sio.event
async def connect(sid, environ):
    print(f"User connected: {sid}")

@sio.event
async def disconnect(sid):
    print(f"User disconnected: {sid}")
    
    if sid in user_sessions:
        session_id = user_sessions[sid]
        if session_id in active_sessions:
            session = active_sessions[session_id]
            
            # Remove participant
            session.participants = [p for p in session.participants if p.id != sid]
            session.last_update = time.time() * 1000
            
            if not session.participants:
                # Clean up empty session
                if session_id in session_timers:
                    session_timers[session_id].cancel()
                    del session_timers[session_id]
                del active_sessions[session_id]
                print(f"Session {session.room_code} cleaned up - no participants")
            else:
                # Notify remaining participants
                await sio.emit('participant_left', {
                    'participant_id': sid,
                    'session': sanitize_session(session)
                }, room=session.room_code)
                
                # Reassign creator if needed
                if session.created_by == sid:
                    session.created_by = session.participants[0].id
                    await sio.emit('creator_changed', {
                        'new_creator_id': session.created_by
                    }, room=session.room_code)
        
        del user_sessions[sid]

@sio.event
async def create_session(sid, data):
    user_name = data.get('user_name', f'User{sid[:6]}')
    session_duration = data.get('session_duration', 25 * 60 * 1000)
    
    session_id = str(uuid4())
    room_code = generate_room_code()
    
    # Ensure unique room code
    while any(s.room_code == room_code for s in active_sessions.values()):
        room_code = generate_room_code()
    
    now = time.time() * 1000
    participant = Participant(
        id=sid,
        user_name=user_name,
        status="ready",
        joined_at=now
    )
    
    session = FocusSession(
        id=session_id,
        room_code=room_code,
        created_by=sid,
        participants=[participant],
        session_duration=session_duration,
        time_left_ms=session_duration,
        created_at=now,
        last_update=now
    )
    
    active_sessions[session_id] = session
    user_sessions[sid] = session_id
    
    await sio.enter_room(sid, room_code)
    
    await sio.emit('session_created', {
        'success': True,
        'session_id': session_id,
        'room_code': room_code,
        'session': sanitize_session(session, sid)
    }, room=sid)
    
    print(f"Session created: {room_code} by {user_name}")

@sio.event
async def join_session(sid, data):
    room_code = data.get('room_code', '').upper()
    user_name = data.get('user_name', f'User{sid[:6]}')
    
    # Find session
    session = None
    for s in active_sessions.values():
        if s.room_code == room_code:
            session = s
            break
    
    if not session:
        await sio.emit('join_error', {'error': 'Session not found'}, room=sid)
        return
    
    if len(session.participants) >= session.max_participants:
        await sio.emit('join_error', {'error': 'Session is full'}, room=sid)
        return
    
    if session.status != 'waiting':
        await sio.emit('join_error', {'error': 'Session already in progress'}, room=sid)
        return
    
    # Add participant
    participant = Participant(
        id=sid,
        user_name=user_name,
        status="ready",
        joined_at=time.time() * 1000
    )
    
    session.participants.append(participant)
    session.last_update = time.time() * 1000
    
    user_sessions[sid] = session.id
    await sio.enter_room(sid, room_code)
    
    # Notify all participants
    await sio.emit('participant_joined', {
        'participant': {
            'id': participant.id,
            'user_name': participant.user_name,
            'status': participant.status,
            'joined_at': participant.joined_at
        },
        'session': sanitize_session(session)
    }, room=room_code)
    
    await sio.emit('session_joined', {
        'success': True,
        'session_id': session.id,
        'room_code': room_code,
        'session': sanitize_session(session, sid)
    }, room=sid)
    
    print(f"{user_name} joined session: {room_code}")

@sio.event
async def start_session(sid, data):
    if sid not in user_sessions:
        await sio.emit('error', {'message': 'No active session'}, room=sid)
        return
    
    session_id = user_sessions[sid]
    session = active_sessions.get(session_id)
    
    if not session or session.created_by != sid:
        await sio.emit('error', {'message': 'Only session creator can start the session'}, room=sid)
        return
    
    if session.status != 'waiting':
        await sio.emit('error', {'message': 'Session cannot be started'}, room=sid)
        return
    
    # Start the session
    session.status = 'active'
    session.started_at = time.time() * 1000
    session.last_update = session.started_at
    
    # Mark all participants as focusing
    for participant in session.participants:
        participant.status = 'focusing'
    
    await sio.emit('session_started', {
        'session': sanitize_session(session)
    }, room=session.room_code)
    
    # Start countdown timer
    timer_task = asyncio.create_task(session_countdown(session_id))
    session_timers[session_id] = timer_task
    
    print(f"Session started: {session.room_code}")

@sio.event
async def update_status(sid, data):
    status = data.get('status')  # 'focusing', 'paused', 'failed'
    
    if sid not in user_sessions:
        return
    
    session_id = user_sessions[sid]
    session = active_sessions.get(session_id)
    
    if not session:
        return
    
    # Update participant status
    for participant in session.participants:
        if participant.id == sid:
            participant.status = status
            session.last_update = time.time() * 1000
            
            # Notify other participants
            await sio.emit('participant_status_updated', {
                'participant_id': sid,
                'status': status,
                'user_name': participant.user_name
            }, room=session.room_code, skip_sid=sid)
            
            print(f"{participant.user_name} status updated to: {status}")
            break

# Cleanup task for old sessions
async def cleanup_old_sessions():
    while True:
        try:
            now = time.time() * 1000
            max_age = 2 * 60 * 60 * 1000  # 2 hours
            
            sessions_to_remove = []
            for session_id, session in active_sessions.items():
                if now - session.last_update > max_age:
                    sessions_to_remove.append((session_id, session.room_code))
            
            for session_id, room_code in sessions_to_remove:
                if session_id in session_timers:
                    session_timers[session_id].cancel()
                    del session_timers[session_id]
                del active_sessions[session_id]
                print(f"Cleaned up old session: {room_code}")
            
            await asyncio.sleep(1800)  # Run every 30 minutes
            
        except Exception as e:
            print(f"Error in cleanup task: {e}")
            await asyncio.sleep(60)

@app.on_event("startup")
async def startup_event():
    # Start cleanup task
    asyncio.create_task(cleanup_old_sessions())
    print("🚀 Fidan Focus Server started")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("server:socket_app", host="0.0.0.0", port=3000, reload=True)