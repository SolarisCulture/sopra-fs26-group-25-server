package ch.uzh.ifi.hase.soprafs26.entity;

import ch.uzh.ifi.hase.soprafs26.constant.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class PlayerEntityTest {

    // Player Creation

    @Test
    void player_shouldHaveIdField() {
        Player player = new Player();
        player.setId(1L);
        assertEquals(1L, player.getId());
    }

    @Test
    void player_shouldHaveUsernameField() {
        Player player = new Player("a");
        assertEquals("a", player.getUsername());
    }
    
    @Test
    void player_shouldHaveTemaField() {
        Player player = new Player();
        player.setTeam(TeamColor.RED);
        assertEquals(TeamColor.RED, player.getTeam());
    }

    @Test
    void player_shouldHaveRoleField() {
        Player player = new Player();
        player.setRole(Role.SPYMASTER);
        assertEquals(Role.SPYMASTER, player.getRole());
    }

    @Test
    void player_shouldHaveIsHostField() {
        Player player = new Player();
        player.setHost(true);
        assertTrue(player.isHost());
    }

    @Test
    void player_shouldHaveJoinedAtField() {
        Player player = new Player();
        player.onCreate(); // Call PrePersist
        assertNotNull(player.getJoinedAt());
    }

    // Default Values

    @Test
    void player_defaultTeam_shouldBeUnassigned() {
        Player player = new Player();
        assertEquals(TeamColor.UNASSIGNED, player.getTeam());
    }

    @Test
    void player_defaultRole_shouldBeNone() {
        Player player = new Player();
        assertEquals(Role.NONE, player.getRole());
    }

    @Test
    void player_defaultIsHost_shouldBeFalse() {
        Player player = new Player();
        assertFalse(player.isHost());
    }

    @Test
    void player_joinedAt_shouldBeSetAutomatically() {
        LocalDateTime before = LocalDateTime.now();
        Player player = new Player();
        player.onCreate(); // Call PrePersist
        LocalDateTime after = LocalDateTime.now();

        assertNotNull(player.getJoinedAt());
        assertTrue(player.getJoinedAt().isAfter(before) || player.getJoinedAt().isEqual(before));
        assertTrue(player.getJoinedAt().isBefore(after) || player.getJoinedAt().isEqual(after));
    }

    // Constructor

    @Test
    void player_defaultConstructor_shouldSetDefaults(){
        Player player = new Player();
        player.onCreate(); // Call PrePersist

        assertEquals(TeamColor.UNASSIGNED, player.getTeam());
        assertEquals(Role.NONE, player.getRole());
        assertFalse(player.isHost());
        assertNotNull(player.getJoinedAt());
    }

    @Test
    void player_usernameConstructor_shouldSetUsername() {
        Player player = new Player("a");
        player.onCreate(); // Call PrePersist

        assertEquals("a", player.getUsername());
        assertEquals(TeamColor.UNASSIGNED, player.getTeam());
        assertEquals(Role.NONE, player.getRole());
        assertFalse(player.isHost());
        assertNotNull(player.getJoinedAt());
    }

    // Helper Methods

    @Test
    void isSpymaster_spymaster_shouldReturnTrue() {
        Player player = new Player();
        player.setRole(Role.SPYMASTER);
        
        assertTrue(player.isSpymaster());
        assertFalse(player.isSpy());
    }

    @Test
    void isSpy_spy_shouldReturnTrue() {
        Player player = new Player();
        player.setRole(Role.SPY);
        
        assertTrue(player.isSpy());
        assertFalse(player.isSpymaster());
    }

    @Test
    void isUnassigned_unassigned_shouldReturnTrue() {
        Player player = new Player();
        player.setTeam(TeamColor.UNASSIGNED);

        assertTrue(player.isUnassigned());
    }
    
    @Test
    void isUnassigned_assigned_shouldReturnFalse() {
        Player player = new Player();
        player.setTeam(TeamColor.RED);

        assertFalse(player.isUnassigned());
    }


}