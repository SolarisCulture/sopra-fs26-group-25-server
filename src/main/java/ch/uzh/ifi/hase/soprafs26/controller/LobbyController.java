package ch.uzh.ifi.hase.soprafs26.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RoleUpdateDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TeamUpdateDTO;
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
	public PlayerDTO assignPlayerToTeam(@PathVariable String lobbyCode, @PathVariable Long playerId, @RequestBody TeamUpdateDTO teamColorDTO) {    //TeamColor as String (+ convert it) or TeamColor object? 
		return lobbyService.assignTeam(lobbyCode, playerId, teamColorDTO.getTeamColor());
	}

    @PutMapping("/api/lobbies/{lobbyCode}/player/{playerId}/role")  
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public PlayerDTO assignPlayerToRole(@PathVariable String lobbyCode, @PathVariable Long playerId, @RequestBody RoleUpdateDTO roleDTO) {  
		return lobbyService.assignRole(lobbyCode, playerId, roleDTO.getRole());
	}
}
