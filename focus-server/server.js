const express = require('express');
const { createServer } = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
const { v4: uuidv4 } = require('uuid');

const app = express();
const server = createServer(app);
const io = new Server(server, {
  cors: {
    origin: "*",
    methods: ["GET", "POST"]
  }
});

// Middleware
app.use(cors());
app.use(express.json());

// In-memory storage for focus sessions (in production, use Redis or database)
const activeSessions = new Map();
const userSessions = new Map(); // userId -> sessionId mapping

// Generate short room codes (6 characters)
function generateRoomCode() {
  return Math.random().toString(36).substring(2, 8).toUpperCase();
}

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ 
    status: 'healthy', 
    activeSessions: activeSessions.size,
    timestamp: new Date().toISOString()
  });
});

// Get session info endpoint
app.get('/session/:code', (req, res) => {
  const session = Array.from(activeSessions.values()).find(s => s.roomCode === req.params.code.toUpperCase());
  if (!session) {
    return res.status(404).json({ error: 'Session not found' });
  }
  
  res.json({
    roomCode: session.roomCode,
    participants: session.participants.length,
    maxParticipants: session.maxParticipants,
    status: session.status,
    timeLeftMs: session.timeLeftMs,
    createdAt: session.createdAt
  });
});

io.on('connection', (socket) => {
  console.log(`User connected: ${socket.id}`);
  
  // Create new focus session
  socket.on('create-session', (data) => {
    const { userName, sessionDuration = 25 * 60 * 1000 } = data;
    
    const sessionId = uuidv4();
    const roomCode = generateRoomCode();
    
    const session = {
      id: sessionId,
      roomCode,
      createdBy: socket.id,
      participants: [{
        id: socket.id,
        userName,
        status: 'ready', // ready, focusing, paused, completed, failed
        joinedAt: Date.now()
      }],
      maxParticipants: 4, // Limit to 4 people for accountability
      status: 'waiting', // waiting, active, completed
      sessionDuration,
      timeLeftMs: sessionDuration,
      startedAt: null,
      createdAt: Date.now(),
      lastUpdate: Date.now()
    };
    
    activeSessions.set(sessionId, session);
    userSessions.set(socket.id, sessionId);
    
    socket.join(roomCode);
    
    socket.emit('session-created', {
      success: true,
      sessionId,
      roomCode,
      session: sanitizeSession(session)
    });
    
    console.log(`Session created: ${roomCode} by ${userName}`);
  });
  
  // Join existing session
  socket.on('join-session', (data) => {
    const { roomCode, userName } = data;
    const upperRoomCode = roomCode.toUpperCase();
    
    const session = Array.from(activeSessions.values()).find(s => s.roomCode === upperRoomCode);
    
    if (!session) {
      socket.emit('join-error', { error: 'Session not found' });
      return;
    }
    
    if (session.participants.length >= session.maxParticipants) {
      socket.emit('join-error', { error: 'Session is full' });
      return;
    }
    
    if (session.status !== 'waiting') {
      socket.emit('join-error', { error: 'Session already in progress' });
      return;
    }
    
    // Add participant
    const participant = {
      id: socket.id,
      userName,
      status: 'ready',
      joinedAt: Date.now()
    };
    
    session.participants.push(participant);
    session.lastUpdate = Date.now();
    
    userSessions.set(socket.id, session.id);
    socket.join(upperRoomCode);
    
    // Notify all participants
    io.to(upperRoomCode).emit('participant-joined', {
      participant,
      session: sanitizeSession(session)
    });
    
    socket.emit('session-joined', {
      success: true,
      sessionId: session.id,
      roomCode: upperRoomCode,
      session: sanitizeSession(session)
    });
    
    console.log(`${userName} joined session: ${upperRoomCode}`);
  });
  
  // Start focus session
  socket.on('start-session', () => {
    const sessionId = userSessions.get(socket.id);
    const session = activeSessions.get(sessionId);
    
    if (!session || session.createdBy !== socket.id) {
      socket.emit('error', { message: 'Only session creator can start the session' });
      return;
    }
    
    if (session.status !== 'waiting') {
      socket.emit('error', { message: 'Session cannot be started' });
      return;
    }
    
    // Start the session
    session.status = 'active';
    session.startedAt = Date.now();
    session.lastUpdate = Date.now();
    
    // Mark all participants as focusing
    session.participants.forEach(p => p.status = 'focusing');
    
    io.to(session.roomCode).emit('session-started', {
      session: sanitizeSession(session)
    });
    
    // Start server-side countdown
    startSessionCountdown(session);
    
    console.log(`Session started: ${session.roomCode}`);
  });
  
  // Update user status during session
  socket.on('update-status', (data) => {
    const { status } = data; // 'focusing', 'paused', 'failed'
    const sessionId = userSessions.get(socket.id);
    const session = activeSessions.get(sessionId);
    
    if (!session) return;
    
    const participant = session.participants.find(p => p.id === socket.id);
    if (participant) {
      participant.status = status;
      session.lastUpdate = Date.now();
      
      // Notify other participants
      socket.to(session.roomCode).emit('participant-status-updated', {
        participantId: socket.id,
        status,
        userName: participant.userName
      });
      
      console.log(`${participant.userName} status updated to: ${status}`);
    }
  });
  
  // Handle disconnection
  socket.on('disconnect', () => {
    console.log(`User disconnected: ${socket.id}`);
    
    const sessionId = userSessions.get(socket.id);
    if (sessionId) {
      const session = activeSessions.get(sessionId);
      if (session) {
        // Remove participant
        session.participants = session.participants.filter(p => p.id !== socket.id);
        session.lastUpdate = Date.now();
        
        if (session.participants.length === 0) {
          // Clean up empty session
          activeSessions.delete(sessionId);
          console.log(`Session ${session.roomCode} cleaned up - no participants`);
        } else {
          // Notify remaining participants
          io.to(session.roomCode).emit('participant-left', {
            participantId: socket.id,
            session: sanitizeSession(session)
          });
          
          // If creator left, assign new creator
          if (session.createdBy === socket.id && session.participants.length > 0) {
            session.createdBy = session.participants[0].id;
            io.to(session.roomCode).emit('creator-changed', {
              newCreatorId: session.createdBy
            });
          }
        }
      }
      userSessions.delete(socket.id);
    }
  });
});

// Server-side countdown management
function startSessionCountdown(session) {
  const countdown = setInterval(() => {
    if (!activeSessions.has(session.id)) {
      clearInterval(countdown);
      return;
    }
    
    const now = Date.now();
    const elapsed = now - session.startedAt;
    session.timeLeftMs = Math.max(0, session.sessionDuration - elapsed);
    session.lastUpdate = now;
    
    // Broadcast time update
    io.to(session.roomCode).emit('time-update', {
      timeLeftMs: session.timeLeftMs,
      elapsed: elapsed
    });
    
    // Check if session completed
    if (session.timeLeftMs <= 0) {
      session.status = 'completed';
      session.participants.forEach(p => {
        if (p.status === 'focusing') p.status = 'completed';
      });
      
      io.to(session.roomCode).emit('session-completed', {
        session: sanitizeSession(session)
      });
      
      clearInterval(countdown);
      
      // Clean up session after 5 minutes
      setTimeout(() => {
        activeSessions.delete(session.id);
        console.log(`Session ${session.roomCode} cleaned up after completion`);
      }, 5 * 60 * 1000);
      
      console.log(`Session completed: ${session.roomCode}`);
    }
  }, 1000);
}

// Remove sensitive data before sending to clients
function sanitizeSession(session) {
  return {
    id: session.id,
    roomCode: session.roomCode,
    participants: session.participants.map(p => ({
      id: p.id,
      userName: p.userName,
      status: p.status,
      joinedAt: p.joinedAt
    })),
    maxParticipants: session.maxParticipants,
    status: session.status,
    sessionDuration: session.sessionDuration,
    timeLeftMs: session.timeLeftMs,
    startedAt: session.startedAt,
    createdAt: session.createdAt,
    isCreator: false // This should be set per user in real implementation
  };
}

// Cleanup old sessions periodically
setInterval(() => {
  const now = Date.now();
  const maxAge = 2 * 60 * 60 * 1000; // 2 hours
  
  for (const [sessionId, session] of activeSessions.entries()) {
    if (now - session.lastUpdate > maxAge) {
      activeSessions.delete(sessionId);
      console.log(`Cleaned up old session: ${session.roomCode}`);
    }
  }
}, 30 * 60 * 1000); // Run every 30 minutes

const PORT = process.env.PORT || 3000;
server.listen(PORT, '0.0.0.0', () => {
  console.log(`🚀 Fidan Focus Server running on port ${PORT}`);
  console.log(`📊 Health check: http://localhost:${PORT}/health`);
});