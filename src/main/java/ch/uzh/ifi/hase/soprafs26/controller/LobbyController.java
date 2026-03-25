package ch.uzh.ifi.hase.soprafs26.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs26.constant.Role;
import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
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
	public PlayerDTO assignPlayerToTeam(@PathVariable String lobbyCode, @PathVariable Long playerId, @RequestBody String teamColorString) {    //TeamColor as String (+ convert it) or TeamColor object? 
        TeamColor teamColor = TeamColor.valueOf(teamColorString);   
		return lobbyService.assignTeam(lobbyCode, playerId, teamColor);
	}

    @PutMapping("/api/lobbies/{lobbyCode}/player/{playerId}/role")  
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public PlayerDTO assignPlayerToRole(@PathVariable String lobbyCode, @PathVariable Long playerId, @RequestBody String roleString) {  
        Role role = Role.valueOf(roleString);
		return lobbyService.assignRole(lobbyCode, playerId, role);
	}
}
