package ch.uzh.ifi.hase.soprafs26.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;

@RestController
public class LobbyController {
    
    private final LobbyService lobbyService;

    public LobbyController(LobbyService lobbyService) { 
		this.lobbyService = lobbyService;
	}

	@PostMapping("/api/lobbies")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public LobbyDTO createLobby() {
        Lobby lobby = lobbyService.createLobby();
        return DTOMapper.INSTANCE.convertEntityToLobbyDTO(lobby);
    }

    @GetMapping("/api/lobbies/{lobbyCode}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public LobbyDTO getLobby(@PathVariable String lobbyCode) {
		if (lobbyCode == null || lobbyCode.trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lobby code cannot be empty");
		}
        Lobby lobby = lobbyService.getLobbyByCode(lobbyCode);
        return DTOMapper.INSTANCE.convertEntityToLobbyDTO(lobby);
    }

	@PutMapping("/api/lobbies/{lobbyCode}/player/{playerId}/team") 
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void assignPlayerToTeam(@PathVariable String lobbyCode, @PathVariable Long playerId, @RequestBody PlayerDTO playerDTO) {
		if (lobbyCode == null || playerId == null || playerDTO == null){ throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One of the arguments is null");}
		lobbyService.assignTeam(lobbyCode, playerId, playerDTO.getTeam());
	}

    @PutMapping("/api/lobbies/{lobbyCode}/player/{playerId}/role")  
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void assignPlayerToRole(@PathVariable String lobbyCode, @PathVariable Long playerId, @RequestBody PlayerDTO playerDTO) {  	
		if (lobbyCode == null || playerId == null || playerDTO == null){ throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One of the arguments is null");}
		lobbyService.assignRole(lobbyCode, playerId, playerDTO.getRole());
	}
}
