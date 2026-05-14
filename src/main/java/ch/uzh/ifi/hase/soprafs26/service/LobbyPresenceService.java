package ch.uzh.ifi.hase.soprafs26.service;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

// Used for disconnect/refresh handling
@Service
public class LobbyPresenceService {

    // diconnect after 3 seconds --> before it counts as refresh
    private static final long DISCONNECT_GRACE_PERIOD_MS = 3_000;
    private static final long GAME_DISCONNECT_GRACE_PERIOD_MS = 30_000L;
    private static final Logger log = LoggerFactory.getLogger(LobbyPresenceService.class);

    private final LobbyService lobbyService;
    private final ScheduledExecutorService disconnectExecutor = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, PlayerSession> sessionsBySessionId = new ConcurrentHashMap<>();
    private final Map<PlayerKey, Set<String>> sessionsByPlayer = new ConcurrentHashMap<>();
    private final Map<PlayerKey, AtomicInteger> disconnectTokens = new ConcurrentHashMap<>();

    public LobbyPresenceService(@Lazy LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    public void registerConnection(String sessionId, String lobbyCode, Long playerId) {
        registerConnection(sessionId, lobbyCode, playerId, ConnectionContext.LOBBY);
    }

    public void registerGameConnection(String sessionId, String lobbyCode, Long playerId) {
        registerConnection(sessionId, lobbyCode, playerId, ConnectionContext.GAME);
    }

    private void registerConnection(String sessionId, String lobbyCode, Long playerId, ConnectionContext context) {
        if (sessionId == null || lobbyCode == null || playerId == null) {
            log.warn("Attempt to register connection with null values: session={}, lobby={}, player={}", 
                    sessionId, lobbyCode, playerId);
            return;
        }

        PlayerKey key = new PlayerKey(lobbyCode, playerId);
        sessionsBySessionId.put(sessionId, new PlayerSession(key, context));
        sessionsByPlayer.computeIfAbsent(key, ignored -> ConcurrentHashMap.newKeySet()).add(sessionId);

        // Cancel any pending disconnect for this player in the appropriate map
        Map<PlayerKey, ScheduledFuture<?>> targetMap = (context == ConnectionContext.GAME) 
            ? pendingGameDisconnects : pendingLobbyDisconnects;
        ScheduledFuture<?> pending = targetMap.remove(key);
        if (pending != null) {
            pending.cancel(false);
            // Invalidate token to abort any running task
            AtomicInteger tokenCounter = disconnectTokens.get(key);
            if (tokenCounter != null) {
                int newToken = tokenCounter.incrementAndGet();
                log.debug("Cancelled pending disconnect for player {}, new token={}", playerId, newToken);
            }
        }
        
        log.debug("Registered {} connection: session={}, player={}, lobby={}", context, sessionId, playerId, lobbyCode);
    }

    public void handleDisconnect(String sessionId) {
        if (sessionId == null) {
            return;
        }

        log.debug("Handling disconnect for session: {}", sessionId);

        PlayerSession session = sessionsBySessionId.remove(sessionId);
        if (session == null) {
            log.debug("No session found for sessionId: {}", sessionId);
            return;
        }

        Set<String> activeSessions = sessionsByPlayer.get(session.key());
        if (activeSessions == null) {
            scheduleDisconnect(session.key(), session.context());
            return;
        }

        activeSessions.remove(sessionId);

        if (!activeSessions.isEmpty()) {
            return;
        }

        sessionsByPlayer.remove(session.key());
        scheduleDisconnect(session.key(), session.context());
    }

    Duration getDisconnectGracePeriod() {
        return Duration.ofMillis(DISCONNECT_GRACE_PERIOD_MS);
    }

    // Separate maps for game vs lobby disconnect tracking
    private final Map<PlayerKey, ScheduledFuture<?>> pendingGameDisconnects = new ConcurrentHashMap<>();
    private final Map<PlayerKey, ScheduledFuture<?>> pendingLobbyDisconnects = new ConcurrentHashMap<>();

    private void scheduleDisconnect(PlayerKey key, ConnectionContext context) {
        long delay = (context == ConnectionContext.GAME) ? GAME_DISCONNECT_GRACE_PERIOD_MS : DISCONNECT_GRACE_PERIOD_MS;
        Map<PlayerKey, ScheduledFuture<?>> targetMap = 
            (context == ConnectionContext.GAME) ? pendingGameDisconnects : pendingLobbyDisconnects;
        
        // Token mechanism as before
        AtomicInteger tokenCounter = disconnectTokens.computeIfAbsent(key, k -> new AtomicInteger(0));
        int expectedToken = tokenCounter.incrementAndGet();

        ScheduledFuture<?> pending = disconnectExecutor.schedule(() -> {
            try {
                AtomicInteger currentCounter = disconnectTokens.get(key);
                if (currentCounter == null || currentCounter.get() != expectedToken) {
                    return;
                }
                
                targetMap.remove(key);
                if (!sessionsByPlayer.containsKey(key)) {
                    if (context == ConnectionContext.GAME) {
                        lobbyService.handleGameDisconnectTimeout(key.lobbyCode(), key.playerId());
                    } else {
                        lobbyService.leaveLobbyAfterDisconnect(key.lobbyCode(), key.playerId());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to process disconnect for player {}: {}", key, e.getMessage(), e);
            } 
        }, delay, TimeUnit.MILLISECONDS);
        targetMap.put(key, pending);
    }

    @PreDestroy
    public void shutdown() {
        disconnectExecutor.shutdownNow();
    }

    private enum ConnectionContext {
        LOBBY,
        GAME
    }

    private record PlayerSession(PlayerKey key, ConnectionContext context) {}

    private record PlayerKey(String lobbyCode, Long playerId) {}

    public boolean hasActiveSessions(String lobbyCode, Long playerId) {
        PlayerKey key = new PlayerKey(lobbyCode, playerId);
        Set<String> sessions = sessionsByPlayer.get(key);
        return sessions != null && !sessions.isEmpty();
    }
}
