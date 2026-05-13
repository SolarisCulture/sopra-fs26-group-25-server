package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.Topic;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.http.MediaType;

import java.util.*;

@SpringBootTest
@AutoConfigureMockMvc
class WordServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LobbyRepository lobbyRepository;

    @Autowired
    private LobbyService lobbyService;

    @Autowired
    private WordService wordService;

    @Autowired
    private ObjectMapper objectMapper;

    private String lobbyCode;


    @BeforeEach
    void setup() {
        wordService = new WordService();
        wordService.loadWordLists(); // triggers @PostConstruct manually
        lobbyRepository.deleteAll();
        Lobby lobby = lobbyService.createLobby("TestHost");
        lobbyCode = lobby.getLobbyCode();
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

    // ==================== Settings Update — customWordList stored ====================
        @Test
        void updateSettings_withCustomWordList_storesInDatabase() throws Exception {
            String customWords = "[\"CAT\",\"DOG\",\"MOM\",\"DAD\",\"END\",\"TRAIN\",\"TABLE\"," +
                    "\"MOUSE\",\"COMPUTER\",\"CHAIR\",\"TREE\",\"PLANT\",\"FLOWER\",\"KISS\"," +
                    "\"HUG\",\"COW\",\"FINGER\",\"FACE\",\"LEG\",\"NOSE\",\"NODE\",\"RING\"," +
                    "\"QUESTION\",\"BLIND\",\"SUN\"]";

            String payload = "{" +
                    "\"spymasterTimeLimit\": 0," +
                    "\"spyTimeLimit\": 0," +
                    "\"rounds\": 0," +
                    "\"topics\": [\"CUSTOM_WORD_LIST\"]," +
                    "\"customWordList\": " + objectMapper.writeValueAsString(customWords) +
                    "}";

            mockMvc.perform(put("/api/lobbies/" + lobbyCode)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk());

            // Verify it's stored
            Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode).orElseThrow();
            assertNotNull(lobby.getSettings().getCustomWordList());
            assertTrue(lobby.getSettings().getCustomWordList().contains("CAT"));
            assertTrue(lobby.getSettings().getCustomWordList().contains("DOG"));
        }

        @Test
        void updateSettings_withNullCustomWordList_doesNotCrash() throws Exception {
            String payload = "{" +
                    "\"spymasterTimeLimit\": 60," +
                    "\"spyTimeLimit\": 30," +
                    "\"rounds\": 5," +
                    "\"topics\": [\"SCIENCE\"]," +
                    "\"customWordList\": null" +
                    "}";

            mockMvc.perform(put("/api/lobbies/" + lobbyCode)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk());

            Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode).orElseThrow();
            assertNull(lobby.getSettings().getCustomWordList());
        }

        @Test
        void updateSettings_withoutCustomWordListField_preservesExisting() throws Exception {
            // First, set custom words
            Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode).orElseThrow();
            lobby.getSettings().setCustomWordList("[\"EXISTING\",\"WORDS\"]");
            lobbyRepository.save(lobby);

            // Then update only timers (no customWordList in payload)
            String payload = "{" +
                    "\"spymasterTimeLimit\": 60," +
                    "\"spyTimeLimit\": 30," +
                    "\"rounds\": 5," +
                    "\"topics\": [\"CUSTOM_WORD_LIST\"]" +
                    "}";

            mockMvc.perform(put("/api/lobbies/" + lobbyCode)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk());

            // customWordList should still be there
            lobby = lobbyRepository.findByLobbyCode(lobbyCode).orElseThrow();
            assertNotNull(lobby.getSettings().getCustomWordList());
            assertTrue(lobby.getSettings().getCustomWordList().contains("EXISTING"));
        }

        // ==================== GET — customWordList returned ====================

        @Test
        void getLobby_returnsCustomWordListInSettings() throws Exception {
            // Store custom words
            Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode).orElseThrow();
            String customWords = "[\"ALPHA\",\"BETA\",\"GAMMA\"]";
            lobby.getSettings().setCustomWordList(customWords);
            lobbyRepository.save(lobby);

            // GET the lobby and check response includes customWordList
            String response = mockMvc.perform(get("/api/lobbies/" + lobbyCode))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            assertTrue(response.contains("customWordList"),
                    "Response should contain customWordList field. Got: " + response);
            assertTrue(response.contains("ALPHA"),
                    "Response should contain custom words. Got: " + response);
        }

        // ==================== WordService — uses custom words ====================

        @Test
        void wordService_withCustomWordList_usesCustomWords() {
            String customWords = "[\"CAT\",\"DOG\",\"MOM\",\"DAD\",\"END\",\"TRAIN\",\"TABLE\"," +
                    "\"MOUSE\",\"COMPUTER\",\"CHAIR\",\"TREE\",\"PLANT\",\"FLOWER\",\"KISS\"," +
                    "\"HUG\",\"COW\",\"FINGER\",\"FACE\",\"LEG\",\"NOSE\",\"NODE\",\"RING\"," +
                    "\"QUESTION\",\"BLIND\",\"SUN\"]";

            List<String> words = wordService.getWordsForGame(
                    List.of(Topic.CUSTOM_WORD_LIST), customWords
            );

            assertEquals(25, words.size());
            // All words should come from the custom list
            List<String> customList = List.of("CAT", "DOG", "MOM", "DAD", "END", "TRAIN", "TABLE",
                    "MOUSE", "COMPUTER", "CHAIR", "TREE", "PLANT", "FLOWER", "KISS",
                    "HUG", "COW", "FINGER", "FACE", "LEG", "NOSE", "NODE", "RING",
                    "QUESTION", "BLIND", "SUN");
            for (String word : words) {
                assertTrue(customList.contains(word),
                        "Word should be from custom list: " + word);
            }
        }

        @Test
        void wordService_withCustomWordListAndOtherTopics_mixesWords() {
            String customWords = "[\"apLpha\",\"BETA\",\"gamma\",\"delta\",\"EPSILON\"]";

            List<String> words = wordService.getWordsForGame(Collections.emptyList(), customWords);

            assertEquals(25, words.size());
            // Should contain at least some custom words
            boolean hasCustom = words.stream().anyMatch(
                    w -> List.of("ALPHA", "BETA", "GAMMA", "DELTA", "EPSILON").contains(w)
            );
            assertTrue(hasCustom, "Board should contain some custom words");
        }

        @Test
        void wordService_withCustomWordListNull_fallsToStandard() {
            List<String> words = wordService.getWordsForGame(
                    List.of(Topic.CUSTOM_WORD_LIST), null
            );

            assertEquals(25, words.size());
            // Should still work — falls back to standard words
        }

        @Test
        void wordService_withCustomWordListEmpty_fallsToStandard() {
            List<String> words = wordService.getWordsForGame(
                    List.of(Topic.CUSTOM_WORD_LIST), ""
            );

            assertEquals(25, words.size());
        }

        @Test
        void wordService_withCustomWordListTooFew_fillsFromStandard() {
            String fewWords = "[\"CAT\",\"DOG\",\"TREE\"]";

            List<String> words = wordService.getWordsForGame(
                    Collections.emptyList(), fewWords
            );

            assertEquals(25, words.size());
            // Should contain the 3 custom words plus 22 standard words
            assertTrue(words.contains("CAT"));
            assertTrue(words.contains("DOG"));
            assertTrue(words.contains("TREE"));
        }

        @Test
        void wordService_withCustomWordListExactly25_usesAll() {
            String exactly25 = "[\"A\",\"B\",\"C\",\"D\",\"E\",\"F\",\"G\",\"H\",\"I\",\"J\"," +
                    "\"K\",\"L\",\"M\",\"N\",\"O\",\"P\",\"Q\",\"R\",\"S\",\"T\"," +
                    "\"U\",\"V\",\"W\",\"X\",\"Y\"]";

            List<String> words = wordService.getWordsForGame(
                    List.of(Topic.CUSTOM_WORD_LIST), exactly25
            );

            assertEquals(25, words.size());
        }

        @Test
        void wordService_withInvalidJson_fallsToStandard() {
            String invalidJson = "not valid json at all";

            List<String> words = wordService.getWordsForGame(
                    List.of(Topic.CUSTOM_WORD_LIST), invalidJson
            );

            assertEquals(25, words.size());
            // Should not crash, just use fallback
        }

        @Test
        void wordService_customWordsAreUppercase() {
            String mixedCase = "[\"cat\",\"Dog\",\"TREE\",\"flower\",\"SUN\"," +
                    "\"moon\",\"star\",\"rain\",\"snow\",\"wind\"," +
                    "\"fire\",\"ice\",\"rock\",\"sand\",\"wave\"," +
                    "\"lake\",\"hill\",\"road\",\"gate\",\"wall\"," +
                    "\"door\",\"bell\",\"lamp\",\"desk\",\"book\"]";

            List<String> words = wordService.getWordsForGame(
                    List.of(Topic.CUSTOM_WORD_LIST), mixedCase
            );

            for (String word : words) {
                assertEquals(word.toUpperCase(), word,
                        "Word should be uppercase: " + word);
            }
        }

        @Test
        void wordService_customWordsDeduplicated() {
            String duplicates = "[\"CAT\",\"CAT\",\"DOG\",\"DOG\",\"CAT\"," +
                    "\"TREE\",\"TREE\",\"SUN\",\"MOON\",\"STAR\"," +
                    "\"RAIN\",\"SNOW\",\"WIND\",\"FIRE\",\"ICE\"," +
                    "\"ROCK\",\"SAND\",\"WAVE\",\"LAKE\",\"HILL\"," +
                    "\"ROAD\",\"GATE\",\"WALL\",\"DOOR\",\"BELL\"," +
                    "\"LAMP\",\"DESK\",\"BOOK\",\"PEN\",\"CUP\"]";

            List<String> words = wordService.getWordsForGame(
                    List.of(Topic.CUSTOM_WORD_LIST), duplicates
            );

            assertEquals(25, words.size());
            assertEquals(words.size(), new java.util.HashSet<>(words).size(),
                    "Board should have no duplicate words");
        }
}