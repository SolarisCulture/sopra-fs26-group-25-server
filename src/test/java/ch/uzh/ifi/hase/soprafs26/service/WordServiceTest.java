package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.Difficulty;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WordServiceTest {
    private WordService wordService = new WordService();

    @Test
    public void getWordsForGame_easy_returns25UniqueWords() {
        List<String> words = wordService.getWordsForGame(Difficulty.EASY);

        assertEquals(25, words.size());
        assertEquals(25, new HashSet<>(words).size()); // all unique
        words.forEach(word -> {
            assertNotNull(word);
            assertFalse(word.isEmpty());
            System.out.println(word); // see the words in test output
        });
    }

    @Test
    public void getWordsForGame_hard_returns25UniqueWords() {
        List<String> words = wordService.getWordsForGame(Difficulty.HARD);

        assertEquals(25, words.size());
        assertEquals(25, new HashSet<>(words).size());
        words.forEach(word -> System.out.println(word));
    }

    @Test
    public void getWordsForGame_allDifficulties_returnDifferentWords() {
        List<String> easy = wordService.getWordsForGame(Difficulty.EASY);
        List<String> hard = wordService.getWordsForGame(Difficulty.HARD);

        // Not all words should be the same
        assertNotEquals(easy, hard);
        System.out.println("EASY: " + easy);
        System.out.println("HARD: " + hard);
    }
}