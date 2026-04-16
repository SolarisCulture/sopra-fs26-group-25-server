package ch.uzh.ifi.hase.soprafs26.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs26.constant.Role;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ClueDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameBoardDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameStatisticsDTO;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import ch.uzh.ifi.hase.soprafs26.service.TurnService;




@RestController
public class GameController {

    private final TurnService turnService;
    private final GameService gameService;


    public GameController(GameService gameService, TurnService turnService) {
        this.gameService = gameService;
        this.turnService = turnService;
    }

    // Host calls this to start the game — board sent via WebSocket
    @PostMapping("/api/games/{lobbyCode}/start")
    @ResponseStatus(HttpStatus.CREATED)
    public void startGame(@PathVariable String lobbyCode) {
        gameService.startGame(lobbyCode);
    }

    // Players call this on page load or reconnect
    @GetMapping("/api/games/{lobbyCode}/board")
    @ResponseStatus(HttpStatus.OK)
    public GameBoardDTO getBoard(@PathVariable String lobbyCode, @RequestParam Role role) {
        return gameService.getBoard(lobbyCode, role);
    }

    @GetMapping("/api/games/{lobbyCode}/statistics")
    @ResponseStatus(HttpStatus.OK)
    public GameStatisticsDTO getGameStatistics(@PathVariable String lobbyCode) {
        return gameService.getGameStatistics(lobbyCode);
    }
    
    @PostMapping("/api/games/{lobbyCode}/restart")
    public void restartGame(@PathVariable String lobbyCode) {
        // Maybe add host check here
        gameService.restartGame(lobbyCode);
    }
    
    @PostMapping("/api/games/{lobbyCode}/backToLobby")
    public void backToLobby(@PathVariable String lobbyCode) {
        // Maybe add host check here
        gameService.backToLobby(lobbyCode);
    }

    @PostMapping("/api/games/{lobbyCode}/regenerate")
    @ResponseStatus(HttpStatus.OK)
    public void regenerateBoard(@PathVariable String lobbyCode, @RequestParam Long spymasterId ) {
        gameService.regenerateBoard(lobbyCode, spymasterId);
    }

    @PostMapping("/api/games/{lobbyCode}/clue")
    @ResponseStatus(HttpStatus.OK)
    public void publishHint(@PathVariable String lobbyCode, 
                            @RequestBody ClueDTO clueDTO) {
        turnService.submitClue(lobbyCode, clueDTO);
    }
}
