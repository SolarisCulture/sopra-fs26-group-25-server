package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Turn;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;


@Service
public class TimerService {
    private final GameRepository gameRepository;
    private final TurnService turnService;

    public TimerService(GameRepository gameRepository, TurnService turnService) {
        this.gameRepository = gameRepository;
        this.turnService = turnService;
    }

    // TIMER COMMENTED OUT BECAUSE WE IMPLEMENT THE FRONTEND LOGIC LATER --> CAUSES ISSUES CURRENTLY 
    // @Scheduled(fixedRate = 1000) // run method every second
    public void checkTimers() {
        List<Game> activeGames = gameRepository.findByStatus(GameStatus.ACTIVE);

        for (Game game : activeGames) {
            Turn turn = game.getCurrentTurn();
            if (turn == null || turn.getStartTime() == null) continue;
            if (game.getLobby() == null || game.getLobby().getSettings() == null) continue;

            long elapsed = Duration.between(turn.getStartTime(), LocalDateTime.now()).getSeconds();
            long timeLimit = game.getLobby().getSettings().getTimeLimit();

            if (elapsed >= timeLimit) {
                String lobbyCode = game.getLobby().getLobbyCode();
                turnService.endTurn(lobbyCode);
            }
        }
    }
}
