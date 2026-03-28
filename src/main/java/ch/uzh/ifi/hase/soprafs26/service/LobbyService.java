package ch.uzh.ifi.hase.soprafs26.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.Role;
import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import jakarta.transaction.Transactional;

@Service
@Transactional  // needed?
public class LobbyService {     

    private final LobbyRepository lobbyRepository;

    public LobbyService(LobbyRepository lobbyRepository) {
        this.lobbyRepository = lobbyRepository;
    }

    public void assignTeam(String lobbyCode, Long playerID, TeamColor TeamColor) {          // lobbyCode or lobbyId --> Don't know if it matters much here
        Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby doesn't exist!"));
        Player player = lobby.getPlayerById(playerID);

        player.setTeam(TeamColor);
    }

    public void assignRole(String lobbyCode, Long playerID, Role role){  
        Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby doesn't exist!"));
        List<Player> playerList = lobby.getPlayerList();
        Player player = lobby.getPlayerById(playerID);
        
        // Checks if there is already one Spymaster (only if the changed role is Spymaster)
        if (role == Role.SPYMASTER) {
            Integer counter = 0;
            for (Player playerFromList : playerList) {
                if (playerFromList.getRole() == Role.SPYMASTER) {counter+=1;}
            }
            if (counter > 0) {throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "There are too many Spymasters!");}
            else {player.setRole(Role.SPYMASTER);}
        } else {
            player.setRole(Role.SPY);
        }
    }

    public boolean canStartGame(String lobbyCode) {
        Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby doesn't exist!"));
        List<Player> playerList = lobby.getPlayerList();

        Integer SpyCountBlue = 0;
        Integer SpymasterCountBlue = 0;
        Integer SpyCountRed = 0;
        Integer SpymasterCountRed = 0;
        for (Player playerFromList : playerList) {      // Count all the roles assigned
            Role playerRole = playerFromList.getRole();
            TeamColor playerTeamColor = playerFromList.getTeam();

            if (playerRole == Role.NONE) {return false;}
            if (playerRole == Role.SPY && playerTeamColor == TeamColor.BLUE) {SpyCountBlue+=1;}
            if (playerRole == Role.SPYMASTER && playerTeamColor == TeamColor.BLUE) {SpymasterCountBlue+=1;}
            if (playerRole == Role.SPY && playerTeamColor == TeamColor.RED) {SpyCountRed+=1;}
            if (playerRole == Role.SPYMASTER && playerTeamColor == TeamColor.BLUE) {SpymasterCountRed+=1;}
        }
        
        if (SpyCountBlue > 0 && SpymasterCountBlue == 1 && SpyCountRed > 0 && SpymasterCountRed == 1){return true;} // Check if all the roles have atleast one player (or exactly one)
        return false;
    }
}
