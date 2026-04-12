package ch.uzh.ifi.hase.soprafs26.service;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyStatus;
import ch.uzh.ifi.hase.soprafs26.constant.Role;
import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.websocket.handler.LobbyWebSocketHandler;

@Service
@Transactional
public class LobbyService {     

    private final LobbyRepository lobbyRepository;
    private final LobbyWebSocketHandler lobbyWebSocketHandler;

    public LobbyService(LobbyRepository lobbyRepository, LobbyWebSocketHandler lobbyWebSocketHandler) {
        this.lobbyRepository = lobbyRepository;
        this.lobbyWebSocketHandler = lobbyWebSocketHandler;
    }

    public Lobby createLobby(String hostUsername) {
        // Validation in service since it could maybe be called from other sources such as tests, WebSocket
        if (hostUsername == null || hostUsername.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Host username is required");
        }
        
        Lobby lobby = new Lobby();
        lobby.setLobbyCode(generateUniqueCode());
        lobby.setLobbyStatus(LobbyStatus.WAITING);

        Player host = new Player(hostUsername);
        host.setHost(true);
        lobby.addPlayer(host);
        lobby.setHostId(host.getId());

        Lobby savedLobby = lobbyRepository.save(lobby);
        // Broadcast lobby created event
        lobbyWebSocketHandler.broadcastLobbyCreated(savedLobby.getLobbyCode(), savedLobby);

        return savedLobby;
    }

    public Lobby getLobbyByCode(String lobbyCode) {
        return lobbyRepository.findByLobbyCode(lobbyCode)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby doesn't exist!"));
    }

    public void transferHost(String lobbyCode, Long currentHostId, Long newHostId) {
        Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby doesn't exist!"));

        // Verify current player
        if(!lobby.getHostId().equals(currentHostId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only host can transfer host role");
        }

        // Find new host
        Player newHost = lobby.getPlayerById(newHostId);
        if(newHost == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found in lobby");
        }

        // Find current host
        Player currentHost = lobby.getPlayerById(currentHostId);
        if(currentHost == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Current host not found in lobby");
        }

        currentHost.setHost(false);
        newHost.setHost(true);
        lobby.setHostId(newHostId);

        lobbyRepository.save(lobby);

        lobbyWebSocketHandler.broadcastHostChanged(lobbyCode, newHost);
    }

    public void leaveLobby(String lobbyCode, Long playerId) {
        Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby doesn't exist!"));

        Player leavingPlayer = lobby.getPlayerById(playerId);
        if(leavingPlayer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found in lobby");
        }

        boolean isHost = leavingPlayer.isHost();

        lobby.removePlayerById(playerId);

        // Close lobby if empty
        if(lobby.getPlayerList().isEmpty()) {
            lobbyRepository.delete(lobby);
            return;
        }

        if(isHost) {
            assignNewHostRandomly(lobby);
        }

        lobbyRepository.save(lobby);

        lobbyWebSocketHandler.broadcastPlayerLeft(lobbyCode, leavingPlayer);

        // If host changed, broadcast host changed event
        if(isHost && lobby.getHostId() != null) {
            Player newHost = lobby.getPlayerById(lobby.getHostId());
            lobbyWebSocketHandler.broadcastHostChanged(lobbyCode, newHost);
        }
    }

    public void assignTeam(String lobbyCode, Long playerID, TeamColor team) {          
        Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby doesn't exist!"));
        Player player = lobby.getPlayerById(playerID);

        player.setTeam(team);

        PlayerDTO playerDTO = DTOMapper.INSTANCE.convertEntityToPlayerDTO(player);      
        // Websocket --> broadcasts the update
        lobbyWebSocketHandler.broadcastTeamUpdated(lobbyCode, playerDTO);            
    }

    public void assignRole(String lobbyCode, Long playerID, Role role){  
        Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby doesn't exist!"));
        List<Player> playerList = lobby.getPlayerList();
        Player player = lobby.getPlayerById(playerID);
        
        // Checks if there is already one Spymaster (only if the changed role is Spymaster)
        if (role == Role.SPYMASTER) {
            long existingSpymasterCount = playerList.stream()
                .filter(p -> p.getRole() == Role.SPYMASTER && !p.getId().equals(playerID) && p.getTeam() == player.getTeam())
                .count();
            if (existingSpymasterCount > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team already has a spymaster");
            }
        }
        player.setRole(role); // Sets the player's role to the new value

        PlayerDTO playerDTO = DTOMapper.INSTANCE.convertEntityToPlayerDTO(player);      
        // Websocket --> broadcasts the update
        lobbyWebSocketHandler.broadcastRoleUpdated(lobbyCode, playerDTO);            
    }

    public boolean canStartGame(String lobbyCode) {
        Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby doesn't exist!"));
        List<Player> playerList = lobby.getPlayerList();

        Integer spyCountBlue = 0;
        Integer spymasterCountBlue = 0;
        Integer spyCountRed = 0;
        Integer spymasterCountRed = 0;
        for (Player playerFromList : playerList) {      // Count all the roles assigned
            Role playerRole = playerFromList.getRole();
            TeamColor playerTeamColor = playerFromList.getTeam();

            if (playerRole == Role.NONE) {return false;}
            if (playerRole == Role.SPY && playerTeamColor == TeamColor.BLUE) {spyCountBlue+=1;}
            if (playerRole == Role.SPYMASTER && playerTeamColor == TeamColor.BLUE) {spymasterCountBlue+=1;}
            if (playerRole == Role.SPY && playerTeamColor == TeamColor.RED) {spyCountRed+=1;}
            if (playerRole == Role.SPYMASTER && playerTeamColor == TeamColor.RED) {spymasterCountRed+=1;}
        }
        
        if (spyCountBlue > 0 && spymasterCountBlue == 1 && spyCountRed > 0 && spymasterCountRed == 1){return true;} // Check if all the roles have atleast one player (or exactly one)
        return false;
    }

    public Long joinLobby(String lobbyCode, String username) {
        // Check if lobby exists
        Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby doesn't exist!"));

        // Check username uniqueness
        List<Player> playerList = lobby.getPlayerList();
        boolean usernameExists = playerList.stream().anyMatch(p -> p.getUsername().equals(username));
        if (usernameExists) {throw new ResponseStatusException(HttpStatus.CONFLICT, "The username is not unique!");}

        // Check game state
        if (!(lobby.getLobbyStatus() == LobbyStatus.WAITING)) {throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The game is still running!");}  // Maybe a better HttpStatus?

        // Add player
        Player player = new Player(username);
        lobby.addPlayer(player);

        lobbyRepository.save(lobby);
        return player.getId();
    }

    public List<Player> getPlayerList(String lobbyCode) {       // Changed name to getPlayerList because it seems more intuitiv then getLobbyState (which I would think should return the actual LobbyState)
        // Check if lobby exists
        Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby doesn't exist!"));

        return lobby.getPlayerList();
    }

    // PlayerDisconnect comes in later part (waiting for merge into main branch/review since already halfway implemented by Timmy)
    // Helper method
    private String generateUniqueCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (lobbyRepository.existsByLobbyCode(code));
        return code;
    }

    private void assignNewHostRandomly(Lobby lobby) {
        List<Player> players = lobby.getPlayerList();
        if (players.isEmpty()) {
            return;
        }
        
        Random random = new Random();
        Player newHost = players.get(random.nextInt(players.size()));
        
        newHost.setHost(true);
        lobby.setHostId(newHost.getId());
    }
}
