package ch.uzh.ifi.hase.soprafs26.service;

import java.util.List;
import java.util.Map;
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
import ch.uzh.ifi.hase.soprafs26.entity.LobbySettings;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UpdateLobbySettingsRequestDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.websocket.handler.LobbyWebSocketHandler;

@Service
@Transactional
public class LobbyService {     

    private final LobbyRepository lobbyRepository;
    private final LobbyWebSocketHandler lobbyWebSocketHandler;
    private static final String lobbyDoesNotExistString = "Lobby doesn't exist!";

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

        // dummy ID --> avoid NullException
        lobby.setHostId(-1L);

        Lobby savedLobby = lobbyRepository.save(lobby);
        
        Player persistedHost = savedLobby.getPlayerList().stream()
                .filter(Player::isHost)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Host player not found"));

        savedLobby.setHostId(persistedHost.getId());

        savedLobby = lobbyRepository.save(savedLobby);
        
        // Broadcast lobby created event
        lobbyWebSocketHandler.broadcastLobbyCreated(savedLobby.getLobbyCode(), savedLobby);

        return savedLobby;
    }

    public Lobby getLobbyByCode(String lobbyCode) {
        return lobbyRepository.findByLobbyCode(lobbyCode)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby doesn't exist!"));
    }

    public void transferHost(String lobbyCode, Long currentHostId, Long newHostId) {
        Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, lobbyDoesNotExistString));

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
        Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, lobbyDoesNotExistString));

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
        Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, lobbyDoesNotExistString));
        Player player = lobby.getPlayerById(playerID);

        player.setTeam(team);

        PlayerDTO playerDTO = DTOMapper.INSTANCE.convertEntityToPlayerDTO(player);      
        // Websocket --> broadcasts the update
        lobbyWebSocketHandler.broadcastTeamUpdated(lobbyCode, playerDTO);            
    }

    public void assignRole(String lobbyCode, Long playerID, Role role){  
        Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, lobbyDoesNotExistString));
        List<Player> playerList = lobby.getPlayerList();
        Player player = lobby.getPlayerById(playerID);
        
        // Checks if there is already one Spymaster (only if the changed role is Spymaster)
        if (role == Role.SPYMASTER) {
            Player existingSpymaster = playerList.stream()
            .filter(p -> p.getRole() == Role.SPYMASTER)
            .filter(p -> p.getTeam() == player.getTeam())
            .findFirst()
            .orElse(null);
            System.out.println(existingSpymaster);
            if (existingSpymaster != null) {existingSpymaster.setRole(Role.SPY);}
        }
        player.setRole(role); // Sets the player's role to the new value

        PlayerDTO playerDTO = DTOMapper.INSTANCE.convertEntityToPlayerDTO(player);      
        // Websocket --> broadcasts the update
        lobbyWebSocketHandler.broadcastRoleUpdated(lobbyCode, playerDTO);            
    }

    public boolean canStartGame(String lobbyCode) {
        Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, lobbyDoesNotExistString));
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
        Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, lobbyDoesNotExistString));
        String newUsername = username.replaceAll("^\"|\"$", "");

        // Check username uniqueness
        List<Player> playerList = lobby.getPlayerList();
        boolean usernameExists = playerList.stream().anyMatch(p -> p.getUsername().equals(newUsername));
        if (usernameExists) {throw new ResponseStatusException(HttpStatus.CONFLICT, "The username is not unique!");}

        // Check game state
        if (!(lobby.getLobbyStatus() == LobbyStatus.WAITING)) {throw new ResponseStatusException(HttpStatus.CONFLICT, "The game is still running!");} 

        // Add player
        Player player = new Player(newUsername);
        lobby.addPlayer(player);

        lobby = lobbyRepository.save(lobby);

        Player savedPlayer = lobby.getPlayerById(player.getId());
        // Fallback: find by username
        if(savedPlayer == null){
            savedPlayer = lobby.getPlayerList().stream()
                            .filter(p -> p.getUsername().equals(newUsername))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Player not found after save"));
        }

        lobbyWebSocketHandler.broadcastPlayerJoined(lobbyCode, savedPlayer);

        return savedPlayer.getId();
    }

    public void updateSettings(String lobbyCode, UpdateLobbySettingsRequestDTO request) {
        Lobby lobby = getLobbyByCode(lobbyCode);
        
        LobbySettings settings = lobby.getSettings();
        // Validate spymaster time limit (0 = unlimited)
        if(request.getSpymasterTimeLimit() != null) {
            int val = request.getSpymasterTimeLimit();
            if(val < 0 || val > 3600) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Spymaster time limit must be between 0 and 3600 seconds");
            }
            settings.setSpymasterTimeLimit(val == 0 ? null : val);
        }

        // Validate spy time limit
        if(request.getSpyTimeLimit() != null) {
            int val = request.getSpyTimeLimit();
            if(val < 0 || val > 3600) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Spy time limit must be between 0 and 3600 seconds");
            }
            settings.setSpyTimeLimit(val == 0 ? null : val);
        }

        // Validate rounds
        if(request.getRounds() != null) {
            int val = request.getRounds();
            if(val < 1 || val > 100) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rounds must be between 1 and 100");
            }
            settings.setRounds(val);
        }

        lobbyRepository.save(lobby);
        Map<String, Object> settingsData = Map.of(
            "spymasterTimeLimit", settings.getSpymasterTimeLimit(),
            "spyTimeLimit", settings.getSpyTimeLimit(),
            "rounds", settings.getRounds()
        );
        lobbyWebSocketHandler.broadcastSettingsUpdated(lobbyCode, settingsData);
    }

    public List<Player> getPlayerList(String lobbyCode) {       // Changed name to getPlayerList because it seems more intuitiv then getLobbyState (which I would think should return the actual LobbyState)
        // Check if lobby exists
        Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, lobbyDoesNotExistString));

        return lobby.getPlayerList();
    }

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
