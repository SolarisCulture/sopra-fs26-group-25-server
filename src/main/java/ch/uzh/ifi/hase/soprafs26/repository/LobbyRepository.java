package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.Player;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LobbyRepository extends JpaRepository<Lobby, Long> {

    Optional<Lobby> findByLobbyCode(String lobbyCode);
    boolean existsByLobbyCode(String lobbyCode);

    @Query("SELECT p FROM Lobby l JOIN l.playerList p WHERE l.lobbyCode = :lobbyCode")          // If it wasn't meant as a query, let me know
    List<Player> findPlayersInLobby(@Param("lobbyCode") String lobbyCode);
}