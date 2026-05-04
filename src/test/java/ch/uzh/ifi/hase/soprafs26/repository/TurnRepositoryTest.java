package ch.uzh.ifi.hase.soprafs26.repository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import ch.uzh.ifi.hase.soprafs26.constant.CardType;
import ch.uzh.ifi.hase.soprafs26.constant.GameEventType;
import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs26.constant.TurnPhase;
import ch.uzh.ifi.hase.soprafs26.entity.Clue;
import ch.uzh.ifi.hase.soprafs26.entity.Guess;
import ch.uzh.ifi.hase.soprafs26.entity.Turn;
import ch.uzh.ifi.hase.soprafs26.entity.WordCard;

@DataJpaTest
public class TurnRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TurnRepository turnRepository;

    @Test
    public void saveTurn_withClueAndGuesses_persistsAndRetrieves() { // TODO: Fails due to ConstraintViolationException
        /* Error Code:
        TurnRepositoryTest > saveTurn_withClueAndGuesses_persistsAndRetrieves() STANDARD_OUT
        Hibernate: insert into word_card (card_type,revealed,word,id) values (?,?,?,default)
        Hibernate: select next value for game_event_seq
        Hibernate: insert into game_event (description,player_id,time_stamp,type,count,word,dtype,id) values (?,?,?,?,?,?,'Clue',?)
        2026-05-04T18:48:52.199+02:00  WARN 26187 --- [    Test worker] org.hibernate.orm.jdbc.error             : HHH000247: ErrorCode: 23514, SQLState: 23514
        2026-05-04T18:48:52.199+02:00  WARN 26187 --- [    Test worker] org.hibernate.orm.jdbc.error             : Check constraint invalid: "CONSTRAINT_8C: "; SQL statement:
        insert into game_event (description,player_id,time_stamp,type,count,word,dtype,id) values (?,?,?,?,?,?,'Clue',?) [23514-240]
        */
        // Create Turn
        Turn turn = new Turn();
        turn.setPhase(TurnPhase.SPY_TURN);
        turn.setCurrentTeamColor(TeamColor.RED);
        turn.setStartTime(LocalDateTime.now());
        turn.setGuessesRemaining(3);

        // Create Clue
        Clue clue = new Clue();
        clue.setWord("animal");
        clue.setCount(2);
        clue.setType(GameEventType.CLUE);
        clue.setTimeStamp(LocalDateTime.now());
        clue.setDescription("Test clue");
        turn.setClue(clue);

        // Create WordCard for Guess
        WordCard card = new WordCard();
        card.setWord("APPLE");
        card.setCardType(CardType.AGENTRED);
        entityManager.persistAndFlush(card); // persist card first

        // Create Guess
        Guess guess = new Guess();
        guess.setWordCard(card);
        guess.setType(GameEventType.GUESS);
        guess.setTimeStamp(LocalDateTime.now());
        guess.setDescription("Guessed APPLE");
        turn.setGuesses(List.of(guess));
        
        // Persist Turn
        entityManager.persistAndFlush(turn);
        Long turnId = turn.getId();

        entityManager.clear(); // Clear persistence context

        Turn loaded = turnRepository.findById(turnId).orElseThrow();

        // Verify Clue
        assertNotNull(loaded.getClue());
        assertEquals("animal", loaded.getClue().getWord());
        assertEquals(2, loaded.getClue().getCount());

        // Verify Guesses
        assertNotNull(loaded.getGuesses());
        assertEquals(1, loaded.getGuesses().size());
        assertEquals("APPLE", loaded.getGuesses().get(0).getWordCard().getWord());

        // Verify Turn fields
        assertEquals(TurnPhase.SPY_TURN, loaded.getPhase());
        assertEquals(TeamColor.RED, loaded.getCurrentTeamColor());
        assertEquals(3, loaded.getGuessesRemaining());
    }

    @Test
    public void turn_savesUnlimitedGuessesCorrectly() {
        Turn turn = new Turn();
        turn.setGuessesRemaining(Integer.MAX_VALUE);
        turn.setPhase(TurnPhase.SPY_TURN);
        turn.setStartTime(LocalDateTime.now());

        entityManager.persistAndFlush(turn);
        Long id = turn.getId();

        entityManager.clear();

        Turn loaded = entityManager.find(Turn.class, id);
        assertEquals(Integer.MAX_VALUE, loaded.getGuessesRemaining());
    }
}
