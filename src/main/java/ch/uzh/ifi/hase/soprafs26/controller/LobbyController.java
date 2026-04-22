package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TransferHostRequest;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UpdateLobbySettingsRequestDTO;
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
    public LobbyDTO createLobby(@RequestBody LobbyDTO request) {
		if (request == null || request.getHostUsername() == null || request.getHostUsername().trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Host username is required");
		}
		String hostUsername = request.getHostUsername();
        Lobby lobby = lobbyService.createLobby(hostUsername);
        return DTOMapper.INSTANCE.convertEntityToLobbyDTO(lobby);
    }

    @GetMapping("/api/lobbies/{lobbyCode}")
    @ResponseStatus(HttpStatus.OK)
    public LobbyDTO getLobby(@PathVariable("lobbyCode") String lobbyCode) {
		if (lobbyCode == null || lobbyCode.trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lobby code cannot be empty");
		}
        Lobby lobby = lobbyService.getLobbyByCode(lobbyCode);
        return DTOMapper.INSTANCE.convertEntityToLobbyDTO(lobby);
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

	@GetMapping("/api/lobbies/{lobbyCode}/players")
	@ResponseStatus(HttpStatus.OK)
	public List<PlayerDTO> getPlayers(@PathVariable("lobbyCode") String lobbyCode) {
		Lobby lobby = lobbyService.getLobbyByCode(lobbyCode);
		return lobby.getPlayerList().stream()
				.map(DTOMapper.INSTANCE::convertEntityToPlayerDTO)
				.collect(Collectors.toList());
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
	public void assignPlayerToTeam(@PathVariable("lobbyCode") String lobbyCode, @PathVariable Long playerId, @RequestBody PlayerDTO playerDTO) {
		if (lobbyCode == null || playerId == null || playerDTO == null){ throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One of the arguments is null");}
		lobbyService.assignTeam(lobbyCode, playerId, playerDTO.getTeam());
	}

    @PutMapping("/api/lobbies/{lobbyCode}/player/{playerId}/role")  
	@ResponseStatus(HttpStatus.OK)
	public void assignPlayerToRole(@PathVariable("lobbyCode") String lobbyCode, @PathVariable Long playerId, @RequestBody PlayerDTO playerDTO) {  	
		if (lobbyCode == null || playerId == null || playerDTO == null){ throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One of the arguments is null");}
		lobbyService.assignRole(lobbyCode, playerId, playerDTO.getRole());
	}

	@PostMapping("/api/lobbies/{lobbyCode}/join")
	@ResponseStatus(HttpStatus.OK)
	public Map<String, Long> joinLobby(@PathVariable("lobbyCode") String lobbyCode, @RequestBody String username) {
		if (lobbyCode == null || username == null){ throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One of the arguments is null");}
		Long id = lobbyService.joinLobby(lobbyCode, username);
		return Collections.singletonMap("id", id);
	}

	@PutMapping("/api/lobbies/{lobbyCode}")
	@ResponseStatus(HttpStatus.OK)
	public void updateSettings(@PathVariable("lobbyCode") String lobbyCode, @RequestBody UpdateLobbySettingsRequestDTO request) {
		System.out.println("Rounds in request (Controller): " + request.getRounds());
		if(lobbyCode == null || request == null){ throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One of the arguments is null");}
		lobbyService.updateSettings(lobbyCode, request);
	}
}
