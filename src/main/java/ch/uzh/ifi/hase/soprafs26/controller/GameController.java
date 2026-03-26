package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.Role;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameBoardDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
public class GameController {

    private final GameService gameService;


    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/api/games/{lobbyCode}/start")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public void startGame(@PathVariable String lobbyCode) {
        gameService.startGame(lobbyCode);
    }

    @GetMapping("/api/games/{lobbyCode}/board")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameBoardDTO getAllUsers(@PathVariable String lobbyCode, @RequestParam Role role) {
        return gameService.getBoard(lobbyCode, role);
    }


}
