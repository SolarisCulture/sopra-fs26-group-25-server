package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long>{
    Optional<Game> findByLobbyLobbyCode(String lobbyCode); // Lobby - lobby (field in Game); LobbyCode - (field in Lobby)
    List<Game> findByStatus(GameStatus status);

}
