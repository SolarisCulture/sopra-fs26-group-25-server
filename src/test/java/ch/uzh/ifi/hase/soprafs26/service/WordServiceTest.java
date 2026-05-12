package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.Topic;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class WordServiceTest {
    private WordService wordService = new WordService();

    @Test
    public void getWordsForGame_returns25UniqueWords() {
        List<String> words = wordService.getWordsForGame();

        assertEquals(25, words.size());
        assertEquals(25, new HashSet<>(words).size()); // all unique
        words.forEach(word -> {
            assertNotNull(word);
            assertFalse(word.isEmpty());
            System.out.println(word); // see the words in test output
        });
    }

    @Test
    public void getWordsForGame_few_topics() {
        List<String> words = wordService.getWordsForGame(List.of(Topic.LANGUAGE, Topic.NATURE));

        assertEquals(25, words.size());
        assertEquals(25, new HashSet<>(words).size()); // all unique
        words.forEach(word -> {
            assertNotNull(word);
            assertFalse(word.isEmpty());
            System.out.println(word); // see the words in test output
        });
    }

    @Test
    public void getWordsForGame_topics_with_space() {
        List<String> words = wordService.getWordsForGame(List.of(Topic.FANTASY));

        assertEquals(25, words.size());
        assertEquals(25, new HashSet<>(words).size()); // all unique
        words.forEach(word -> {
            assertNotNull(word);
            assertFalse(word.isEmpty());
            System.out.println(word); // see the words in test output
        });
    }

    @Test
    public void getWordsForGame_topics_timer() {
        long start = System.currentTimeMillis();
        List<String> words = wordService.getWordsForGame(List.of(Topic.SCIENCE));
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("Generation took: " + elapsed + "ms");

        assertEquals(25, words.size());
        assertEquals(25, new HashSet<>(words).size());
        words.forEach(word -> {
            assertNotNull(word);
            assertFalse(word.isEmpty());
            System.out.println(word);
        });
    }
}