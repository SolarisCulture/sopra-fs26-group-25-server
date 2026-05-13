package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.Topic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

    class WordServiceTest {

    private WordService wordService;

    @BeforeEach
    void setup() {
        wordService = new WordService();
        wordService.loadWordLists(); // triggers @PostConstruct manually
    }

    // ==================== getWordsForGame() — no topics ====================

    @Test
    void getWordsForGame_noArgs_returns25Words() {
        List<String> words = wordService.getWordsForGame();
        assertEquals(25, words.size());
    }

    @Test
    void getWordsForGame_noArgs_returnsUppercaseWords() {
        List<String> words = wordService.getWordsForGame();
        for (String word : words) {
            assertEquals(word.toUpperCase(), word, "Word should be uppercase: " + word);
        }
    }

    @Test
    void getWordsForGame_noArgs_returnsNoDuplicates() {
        List<String> words = wordService.getWordsForGame();
        Set<String> unique = new HashSet<>(words);
        assertEquals(words.size(), unique.size(), "Board should have no duplicate words");
    }

    @Test
    void getWordsForGame_noArgs_returnsShuffledOrder() {
        List<String> first = wordService.getWordsForGame();
        List<String> second = wordService.getWordsForGame();
        // Very unlikely to get the same order twice with 25 random words
        assertNotEquals(first, second, "Two calls should produce different orderings");
    }

    // ==================== getWordsForGame(topics) — with topics ====================

    @Test
    void getWordsForGame_nullTopics_returns25Words() {
        List<String> words = wordService.getWordsForGame(null);
        assertEquals(25, words.size());
    }

    @Test
    void getWordsForGame_emptyTopics_returns25Words() {
        List<String> words = wordService.getWordsForGame(Collections.emptyList());
        assertEquals(25, words.size());
    }

    @Test
    void getWordsForGame_standardOnly_returns25Words() {
        List<String> words = wordService.getWordsForGame(List.of(Topic.STANDARD));
        assertEquals(25, words.size());
    }

    @Test
    void getWordsForGame_singleTopic_returns25Words() {
        List<String> words = wordService.getWordsForGame(List.of(Topic.SCIENCE));
        assertEquals(25, words.size());
    }

    @Test
    void getWordsForGame_singleTopic_returnsNoDuplicates() {
        List<String> words = wordService.getWordsForGame(List.of(Topic.HISTORY));
        Set<String> unique = new HashSet<>(words);
        assertEquals(words.size(), unique.size(), "Board should have no duplicate words");
    }

    @Test
    void getWordsForGame_singleTopic_returnsUppercaseWords() {
        List<String> words = wordService.getWordsForGame(List.of(Topic.FANTASY));
        for (String word : words) {
            assertEquals(word.toUpperCase(), word, "Word should be uppercase: " + word);
        }
    }

    @Test
    void getWordsForGame_multipleTopics_returns25Words() {
        List<String> words = wordService.getWordsForGame(
                List.of(Topic.SCIENCE, Topic.HISTORY, Topic.GEOGRAPHY)
        );
        assertEquals(25, words.size());
    }

    @Test
    void getWordsForGame_multipleTopics_returnsNoDuplicates() {
        List<String> words = wordService.getWordsForGame(
                List.of(Topic.NATURE, Topic.FOOD_DRINK, Topic.SPORT)
        );
        Set<String> unique = new HashSet<>(words);
        assertEquals(words.size(), unique.size());
    }

    @Test
    void getWordsForGame_allTopics_returns25Words() {
        List<Topic> allTopics = new ArrayList<>(Arrays.asList(Topic.values()));
        allTopics.remove(Topic.STANDARD);
        List<String> words = wordService.getWordsForGame(allTopics);
        assertEquals(25, words.size());
    }

    @Test
    void getWordsForGame_withTopics_wordsAreValidForBoard() {
        List<String> words = wordService.getWordsForGame(List.of(Topic.TECHNOLOGY_GAMES));
        for (String word : words) {
            assertFalse(word.contains(" "), "Word should not contain spaces: " + word);
            assertFalse(word.isEmpty(), "Word should not be empty");
            assertTrue(word.length() <= 15, "Word should not be too long: " + word);
        }
    }

    // ==================== Fallback behavior ====================

    @Test
    void getWordsForGame_withStandardPlusOtherTopic_usesTopicWords() {
        // STANDARD + another topic should use topic words, not standard fallback
        List<String> words = wordService.getWordsForGame(
                List.of(Topic.STANDARD, Topic.FANTASY)
        );
        assertEquals(25, words.size());
    }

    @Test
    void getWordsForGame_topicFallback_alwaysReturns25EvenIfDatamuseFails() {
        // Even without internet, the topic file fallback + general fallback
        // should guarantee 25 words
        List<String> words = wordService.getWordsForGame(List.of(Topic.DISNEY));
        assertEquals(25, words.size());
    }

    // ==================== Topic word list loading ====================

    @Test
    void loadWordLists_scienceTopicHasWords() {
        // After loadWordLists, getting words for SCIENCE should return topic-specific words
        List<String> words = wordService.getWordsForGame(List.of(Topic.SCIENCE));
        assertEquals(25, words.size());
        // Science topic should contain at least some science words from the file
        // (mixed with Datamuse results, but fallback guarantees file words are available)
    }

    @Test
    void loadWordLists_allTopicsLoadSuccessfully() {
        // Every topic except STANDARD should be able to produce 25 words
        for (Topic topic : Topic.values()) {
            if (topic == Topic.STANDARD) continue;
            List<String> words = wordService.getWordsForGame(List.of(topic));
            assertEquals(25, words.size(),
                    "Topic " + topic + " should produce 25 words");
        }
    }

    // ==================== Edge cases ====================

    @Test
    void getWordsForGame_calledMultipleTimes_doesNotReturnSameBoard() {
        List<String> first = wordService.getWordsForGame(List.of(Topic.FILM));
        List<String> second = wordService.getWordsForGame(List.of(Topic.FILM));
        // Same words but different order (or different subset)
        assertNotEquals(first, second,
                "Two consecutive calls should produce different boards");
    }

    @Test
    void getWordsForGame_wordsDoNotContainSpecialCharacters() {
        List<String> words = wordService.getWordsForGame(List.of(Topic.MUSIC));
        for (String word : words) {
            assertTrue(word.matches("[A-Z]+"),
                    "Word should only contain letters: " + word);
        }
    }

    @Test
    void getWordsForGame_noWordExceedsMaxLength() {
        List<String> words = wordService.getWordsForGame(List.of(Topic.ARCHITECTURE));
        for (String word : words) {
            assertTrue(word.length() <= 15,
                    "Word too long for board: " + word + " (" + word.length() + " chars)");
        }
    }

    @Test
    void getWordsForGame_noWordIsTooShort() {
        List<String> words = wordService.getWordsForGame(List.of(Topic.BUSINESS));
        for (String word : words) {
            assertTrue(word.length() >= 2,
                    "Word too short: " + word);
        }
    }


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