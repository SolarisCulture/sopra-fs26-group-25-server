package ch.uzh.ifi.hase.soprafs26.service;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

// Used for disconnect/refresh handling
@Service
public class LobbyPresenceService {

    // diconnect after 3 seconds --> before it counts as refresh
    private static final long DISCONNECT_GRACE_PERIOD_MS = 3_000;

    private final LobbyService lobbyService;
    private final ScheduledExecutorService disconnectExecutor = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, PlayerSession> sessionsBySessionId = new ConcurrentHashMap<>();
    private final Map<PlayerKey, Set<String>> sessionsByPlayer = new ConcurrentHashMap<>();
    private final Map<PlayerKey, ScheduledFuture<?>> pendingDisconnects = new ConcurrentHashMap<>();

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
            return;
        }

        PlayerKey key = new PlayerKey(lobbyCode, playerId);
        sessionsBySessionId.put(sessionId, new PlayerSession(key, context));
        sessionsByPlayer.computeIfAbsent(key, ignored -> ConcurrentHashMap.newKeySet()).add(sessionId);

        ScheduledFuture<?> pendingDisconnect = pendingDisconnects.remove(key);
        if (pendingDisconnect != null) {
            pendingDisconnect.cancel(false);
        }
    }

    public void handleDisconnect(String sessionId) {
        if (sessionId == null) {
            return;
        }

        PlayerSession session = sessionsBySessionId.remove(sessionId);
        if (session == null) {
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

    private void scheduleDisconnect(PlayerKey key, ConnectionContext context) {
        // executes this after the grace period delay
        ScheduledFuture<?> pendingDisconnect = disconnectExecutor.schedule(
                () -> {
                    pendingDisconnects.remove(key);
                    if (!sessionsByPlayer.containsKey(key)) {
                        if (context == ConnectionContext.GAME) {
                            lobbyService.leaveGameAfterDisconnect(key.lobbyCode(), key.playerId());
                        } else {
                            lobbyService.leaveLobbyAfterDisconnect(key.lobbyCode(), key.playerId());
                        }
                    }
                },
                DISCONNECT_GRACE_PERIOD_MS,
                TimeUnit.MILLISECONDS
        );
        pendingDisconnects.put(key, pendingDisconnect);
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
}
