package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WordServiceTest {
    private final WordService wordService = new WordService();

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
    public void getWordsForGame_topics() {
        List<String> topics = new ArrayList<>(Arrays.asList("books", "animals", "politics"));
        List<String> words = wordService.getWordsForGame(topics);

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
        List<String> topics = new ArrayList<>(Arrays.asList("books", "animals", "politics"));

        long start = System.currentTimeMillis();
        List<String> words = wordService.getWordsForGame(topics);
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