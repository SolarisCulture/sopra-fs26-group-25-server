package ch.uzh.ifi.hase.soprafs26.entity;

import ch.uzh.ifi.hase.soprafs26.constant.Role;
import ch.uzh.ifi.hase.soprafs26.constant.Topic;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
public class PersistenceTests {

    @Autowired
    private TestEntityManager em;

    @Autowired
    EntityManager entityManager;

    /*@Test
    void shouldPersistLobbyWithTopics() {
        Lobby lobby = new Lobby();
        lobby.getSettings().setTopics(List.of(Topic.ANIMALS));

        em.persistAndFlush(lobby);
    }*/

    @Test
    void shouldReturnPlayerWhoSubmittedClue() {
        // 2. Create clue linked to player
        Clue clue = new Clue();
        clue.setWord("animal");
        clue.setCount(2);

        em.persistAndFlush(clue);
        em.clear();

        // 3. Load clue from DB
        Clue loadedClue = em.find(Clue.class, clue.getId());

        // 4. Verify player
        assertNotNull(loadedClue.getId());
        assertEquals(2, loadedClue.getCount());
        assertEquals("animal", loadedClue.getWord());
    }
    @Test
    void currentTurnShouldBelongToSameGame() {
        Game game1 = new Game();
        Game game2 = new Game();

        Turn turn = new Turn();
        turn.setGame(game2);

        em.persist(game1);
        em.persist(game2);
        em.persist(turn);

        game1.setCurrentTurn(turn);

        em.persistAndFlush(game1);
    }

    // If mapping broken: app fails before tests run
    @Test
    void contextLoads() {
    }

}