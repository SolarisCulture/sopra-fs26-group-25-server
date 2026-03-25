package ch.uzh.ifi.hase.soprafs26.entity;

import ch.uzh.ifi.hase.soprafs26.constant.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class LobbyEntityTest {

    // Lobby Creation (Maybe put together?)

    @Test
    void lobby_shouldHaveIdField() {
        Lobby lobby= new Lobby();
        lobby.setId(1L);
        assertEquals(1L, lobby.getId());
    }

    @Test
    void lobby_shouldHaveLobbyCodeField() {
        Lobby lobby = new Lobby();
        lobby.setLobbyCode("ABC123");
        assertEquals("ABC123", lobby.getLobbyCode());
    }

    @Test
    void lobby_shouldHaveHostIdField() {
        Lobby lobby = new Lobby();
        lobby.setHostId(10L);
        assertEquals(10L, lobby.getHostId());
    }

    @Test
    void lobby_shouldHavePlayerList() {
        Lobby lobby = new Lobby();
        assertNotNull(lobby.getPlayerList());
        assertTrue(lobby.getPlayerList().isEmpty());
    }

    @Test
    void lobby_shouldHaveSettings() {
        Lobby lobby = new Lobby();
        assertNotNull(lobby.getSettings());
    }

    @Test
    void lobby_shouldHaveCreatedAt() {
        Lobby lobby = new Lobby();
        lobby.onCreate(); // Call PrePersist
        assertNotNull(lobby.getCreatedAt());
    }

    @Test
    void lobby_shouldHaveLobbyStatus() {
        Lobby lobby = new Lobby();
        lobby.setLobbyStatus(LobbyStatus.WAITING);
        assertEquals(LobbyStatus.WAITING, lobby.getLobbyStatus());
    }

    // Default Values

    @Test
    void lobby_createdAt_shouldBeSetAutomatically() {
        LocalDateTime before = LocalDateTime.now();
        Lobby lobby = new Lobby();
        lobby.onCreate();
        LocalDateTime after = LocalDateTime.now();

        assertNotNull(lobby.getCreatedAt());
        assertTrue(lobby.getCreatedAt().isAfter(before) || lobby.getCreatedAt().isEqual(before));
        assertTrue(lobby.getCreatedAt().isBefore(after) || lobby.getCreatedAt().isEqual(after));
    }

    @Test
    void lobby_lobbyStatus_defaultShouldBeWaiting() {
        Lobby lobby = new Lobby();
        lobby.onCreate();
        assertEquals(LobbyStatus.WAITING, lobby.getLobbyStatus());
    }

    // Player Helper Methods
    
    @Test
    void addPlayer_shouldAddToPlayerList() {
        Lobby lobby = new Lobby();
        Player player = new Player("a");

        lobby.addPlayer(player);

        assertEquals(1, lobby.getPlayerList().size());
        assertTrue(lobby.getPlayerList().contains(player));
    }

    @Test
    void removePlayer_shouldRemoveFromPlayerList() {
        Lobby lobby = new Lobby();
        Player player = new Player("a");
        lobby.addPlayer(player);

        lobby.removePlayer(player);

        assertTrue(lobby.getPlayerList().isEmpty());
    }


    @Test
    void getPlayerById_shouldReturnCorrectPlayer() {
        Lobby lobby = new Lobby();
        Player player1 = new Player("a");
        Player player2 = new Player("b");
        
        player1.setId(1L);
        player2.setId(2L);

        lobby.addPlayer(player1);
        lobby.addPlayer(player2);

        Player found = lobby.getPlayerById(2L);

        assertEquals(player2, found);
    }

    @Test
    void getPlayerById_invalidId_shouldReturnNull() {
        Lobby lobby = new Lobby();
        Player player = new Player("a");
        
        player.setId(1L);
        lobby.addPlayer(player);

        Player found = lobby.getPlayerById(99L);

        assertNull(found);
    }

    @Test
    void removePlayerById_shouldRemovefromPlayerList() {
        Lobby lobby = new Lobby();
        Player player1 = new Player("a");
        Player player2 = new Player("b");
        
        player1.setId(1L);
        player2.setId(2L);

        lobby.addPlayer(player1);
        lobby.addPlayer(player2);

        lobby.removePlayerById(2L);

        assertEquals(1, lobby.getPlayerList().size());
    }

    @Test
    void isUsernameTaken_taken_shouldReturnTrue() {
        Lobby lobby = new Lobby();
        Player player = new Player("a");
        
        lobby.addPlayer(player);

        assertTrue(lobby.isUsernameTaken("a"));
    }

    @Test
    void isUsernameTaken_notTaken_shouldReturnFalse() {
        Lobby lobby = new Lobby();
        Player player = new Player("a");
        lobby.addPlayer(player);

        assertFalse(lobby.isUsernameTaken("b"));
    }

}