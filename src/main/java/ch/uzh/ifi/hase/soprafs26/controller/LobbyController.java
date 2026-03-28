package ch.uzh.ifi.hase.soprafs26.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TransferHostRequest;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import org.springframework.web.bind.annotation.PostMapping;


@RestController
public class LobbyController {
    
    private LobbyService lobbyService;

    public LobbyController(LobbyService lobbyService) { 
		this.lobbyService = lobbyService;
	}

	@PutMapping("/api/lobbies/{lobbyCode}/host/transfer")
    @ResponseStatus(HttpStatus.OK)
    public void transferHost(
            @PathVariable String lobbyCode,
            @RequestBody TransferHostRequest request) {
        
        if (request.getCurrentHostId() == null || request.getNewHostId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Host IDs are required");
        }
        
        lobbyService.transferHost(lobbyCode, request.getCurrentHostId(), request.getNewHostId());
    }

	@DeleteMapping("/api/lobbies/{lobbyCode}/players/{playerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveLobby(
            @PathVariable String lobbyCode,
            @PathVariable Long playerId) {
        
        lobbyService.leaveLobby(lobbyCode, playerId);
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

	@PostMapping("/api/lobbies/{lobbyCode}/join")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void joinLobby(@PathVariable String lobbyCode, @RequestBody String username) {
		if (lobbyCode == null || username == null){ throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One of the arguments is null");}
		lobbyService.joinLobby(lobbyCode, username);
	}
}
