package ch.uzh.ifi.hase.soprafs26.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerDTO;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;

@RestController
public class LobbyController {
    
    private LobbyService lobbyService;

    LobbyController(LobbyService lobbyService) { 
		this.lobbyService = lobbyService;
	}

	@PutMapping("/api/lobbies/{lobbyCode}/player/{playerId}/team") 
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void assignPlayerToTeam(@PathVariable String lobbyCode, @PathVariable Long playerId, @RequestBody PlayerDTO playerDTO) {
		if (lobbyCode == null || playerId == null || playerDTO == null){ throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One of the arguments is null");}
		lobbyService.assignTeam(lobbyCode, playerId, playerDTO.getTeamColor());
	}

    @PutMapping("/api/lobbies/{lobbyCode}/player/{playerId}/role")  
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void assignPlayerToRole(@PathVariable String lobbyCode, @PathVariable Long playerId, @RequestBody PlayerDTO playerDTO) {  	
		if (lobbyCode == null || playerId == null || playerDTO == null){ throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One of the arguments is null");}
		lobbyService.assignRole(lobbyCode, playerId, playerDTO.getRole());
	}
}
