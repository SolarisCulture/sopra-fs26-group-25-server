package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.Turn;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Autowired
    private LobbyRepository lobbyRepository;

    @GetMapping("/game/{lobbyCode}")
    public Map<String, Object> debugGame(@PathVariable String lobbyCode) {
        Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode).orElse(null);
        if (lobby == null || lobby.getGame() == null) return Map.of("error", "not found");

        Game game = lobby.getGame();
        Turn turn = game.getCurrentTurn();
        return Map.of(
                "status", game.getStatus(),
                "currentTurnPhase", turn != null ? turn.getPhase() : "null",
                "currentTeam", turn != null ? turn.getCurrentTeamColor() : "null",
                "redScore", game.getRedScore(),
                "blueScore", game.getBlueScore()
        );
    }
}