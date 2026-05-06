package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.constant.TurnPhase;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Turn;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.websocket.handler.GameWebSocketHandler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;


@Service
public class TimerService {
    private final GameRepository gameRepository;
    private final TurnService turnService;
    private final GameWebSocketHandler gameWebSocketHandler;

    public TimerService(GameRepository gameRepository, TurnService turnService, GameWebSocketHandler gameWebSocketHandler) {
        this.gameRepository = gameRepository;
        this.turnService = turnService;
        this.gameWebSocketHandler = gameWebSocketHandler;
    }

    //@Scheduled(fixedRate = 1000) // run method every second
    public void checkTimers() {
        List<Game> activeGames = gameRepository.findByStatus(GameStatus.ACTIVE);
        LocalDateTime now = LocalDateTime.now();

        for (Game game : activeGames) {
            Turn turn = game.getCurrentTurn();
            if (turn == null || turn.getStartTime() == null) continue;
            if (game.getLobby() == null || game.getLobby().getSettings() == null) continue;

            // if the timeLimit is 0 for this phase then ignore --> no limit
            Long timeLimit = getTimeLimitForPhase(game);
            if (timeLimit == null || timeLimit == 0) continue;

            long remainingSeconds = calculateRemainingSeconds(turn.getStartTime(), timeLimit, now);
            String lobbyCode = game.getLobby().getLobbyCode();

            gameWebSocketHandler.broadcastTimer(lobbyCode, remainingSeconds);

            if (remainingSeconds == 0) {
                turnService.endTurn(lobbyCode);
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

    // helps with issue that sometimes seconds were skipped
    private long calculateRemainingSeconds(LocalDateTime startTime, long timeLimit, LocalDateTime now) {
        LocalDateTime deadline = startTime.plusSeconds(timeLimit);
        long remainingMillis = Duration.between(now, deadline).toMillis();

        if (remainingMillis <= 0) {
            return 0;
        }

        return (long) Math.ceil(remainingMillis / 1000.0);
    }
}
