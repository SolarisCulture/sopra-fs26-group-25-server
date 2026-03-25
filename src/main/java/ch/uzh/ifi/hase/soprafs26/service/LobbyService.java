package ch.uzh.ifi.hase.soprafs26.service;

import org.springframework.stereotype.Service;

import ch.uzh.ifi.hase.soprafs26.constant.Role;
import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerDTO;
import jakarta.transaction.Transactional;

@Service
@Transactional  // needed?
public class LobbyService {     // temporary
    public PlayerDTO assignTeam(String a, Long b, TeamColor c){
        return new PlayerDTO();
    }

    public PlayerDTO assignRole(String a, Long b, Role c){  // temp
        return new PlayerDTO();
    }
}
