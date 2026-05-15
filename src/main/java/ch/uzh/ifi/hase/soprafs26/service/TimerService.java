package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.constant.TurnPhase;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.GameTimer;
import ch.uzh.ifi.hase.soprafs26.entity.Turn;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.websocket.handler.GameWebSocketHandler;

import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.Duration; 
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TimerService {
    private final GameRepository gameRepository;
    private final TurnService turnService;
    private final GameWebSocketHandler gameWebSocketHandler;
    
    // Store timers in memory instead of querying database every second
    private final Map<String, GameTimer> activeTimers = new ConcurrentHashMap<>();

    public TimerService(GameRepository gameRepository, TurnService turnService, @Lazy GameWebSocketHandler gameWebSocketHandler) {
        this.gameRepository = gameRepository;
        this.turnService = turnService;
        this.gameWebSocketHandler = gameWebSocketHandler;
    }

    // Call this when a turn starts
    public void startTimer(String lobbyCode, long timeLimitSeconds) {
        if (timeLimitSeconds > 0) {
            activeTimers.put(lobbyCode, new GameTimer(timeLimitSeconds));
        }
    }
    
    // Call this when a turn ends early
    public void stopTimer(String lobbyCode) {
        activeTimers.remove(lobbyCode);
    }
    
    // Get current remaining time (for UI sync)
    public long getRemainingSeconds(String lobbyCode) {
        GameTimer timer = activeTimers.get(lobbyCode);
        if (timer == null) return 0;
        return timer.getRemainingSeconds(LocalDateTime.now());
    }

    public void pauseTimer(String lobbyCode, boolean paused) {
        Game game = gameRepository.findByLobbyLobbyCode(lobbyCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));

        if (game.getStatus() == GameStatus.FINISHED || game.getStatus() == GameStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot pause a finished game");
        }

        GameTimer timer = activeTimers.get(lobbyCode);
        LocalDateTime now = LocalDateTime.now();

        if (paused) {
            if (timer != null) {
                timer.pause(now);
                gameWebSocketHandler.broadcastTimer(lobbyCode, timer.getRemainingSeconds(now));
            }
            game.setStatus(GameStatus.PAUSE);
            gameRepository.save(game);
            gameWebSocketHandler.broadcastGamePaused(lobbyCode);
            return;
        }

        if (timer != null) {
            timer.resume(now);
            gameWebSocketHandler.broadcastTimer(lobbyCode, timer.getRemainingSeconds(now));
        }
        game.setStatus(GameStatus.ACTIVE);
        gameRepository.save(game);
        gameWebSocketHandler.broadcastGameResumed(lobbyCode);
    }

    @Scheduled(fixedRate = 1000)
    public void checkTimers() {
        if (activeTimers.isEmpty()) return; // Nothing to do
        
        LocalDateTime now = LocalDateTime.now();
        
        // Iterate over in-memory timers
        for (Map.Entry<String, GameTimer> entry : activeTimers.entrySet()) {
            String lobbyCode = entry.getKey();
            GameTimer timer = entry.getValue();
            if (timer.isPaused()) {
                continue;
            }
            
            long remainingSeconds = timer.getRemainingSeconds(now);
            
            // Send WebSocket update (make this async if needed)
            gameWebSocketHandler.broadcastTimer(lobbyCode, remainingSeconds);
            
            if (remainingSeconds == 0) {
                // Time's up - remove timer and end the turn
                activeTimers.remove(lobbyCode);
                turnService.endTurn(lobbyCode);
            }
        }
    }
    
    // Load active timers when server starts (for crash recovery)
    public void loadActiveTimersFromDatabase() {
        List<Game> activeGames = gameRepository.findByStatus(GameStatus.ACTIVE);
        LocalDateTime now = LocalDateTime.now();
        
        for (Game game : activeGames) {
            Turn currentTurn = game.getCurrentTurn();
            if (currentTurn != null && currentTurn.getStartTime() != null) {
                Long timeLimit = getTimeLimitForPhase(game);
                if (timeLimit != null && timeLimit > 0) {
                    LocalDateTime startTime = currentTurn.getStartTime();
                    long elapsed = Duration.between(startTime, now).getSeconds();
                    long remaining = timeLimit - elapsed;
                    
                    if (remaining > 0) {
                        activeTimers.put(game.getLobby().getLobbyCode(), new GameTimer(remaining));
                    } else if (remaining == 0) {
                        // Should have ended already - trigger now
                        turnService.endTurn(game.getLobby().getLobbyCode());
                    }
                }
            }
        }
    }
    
    private Long getTimeLimitForPhase(Game game) {
        if (game.getCurrentTurn().getPhase() == TurnPhase.SPYMASTER_TURN) {
            Integer timeLimit = game.getLobby().getSettings().getSpymasterTimeLimit();
            return timeLimit == null ? null : timeLimit.longValue();
        }
        if (game.getCurrentTurn().getPhase() == TurnPhase.SPY_TURN) {
            Integer timeLimit = game.getLobby().getSettings().getSpyTimeLimit();
            return timeLimit == null ? null : timeLimit.longValue();
        }
        return null;
    }
}
