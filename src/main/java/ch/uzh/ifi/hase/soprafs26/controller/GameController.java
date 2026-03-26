package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.Role;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameBoardDTO;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
public class GameController {

    private final GameService gameService;


    public GameController(GameService gameService) {
        this.gameService = gameService;
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


}
