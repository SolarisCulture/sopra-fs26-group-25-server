package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.Difficulty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.time.Duration;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class WordService {

    private static final Logger log = LoggerFactory.getLogger(WordService.class);

    private final RestTemplate restTemplate;
    private static final String BASE_URL = "https://random-word-api.herokuapp.com/word";

    private static final List<String> DEFAULT_WORDS = List.of(
            "APPLE", "BANK", "BRIDGE", "CAR", "CASTLE",
            "CLOUD", "DIAMOND", "DRAGON", "EAGLE", "ENGINE",
            "FALCON", "FIRE", "GARDEN", "GHOST", "HAMMER",
            "HORSE", "ICE", "ISLAND", "JUNGLE", "KING",
            "KNIGHT", "LAMP", "LEMON", "MARBLE", "MOON",
            "NEEDLE", "NIGHT", "OCEAN", "OPERA", "PIANO",
            "QUEEN", "RIVER", "STAR", "TREE", "UMBRELLA",
            "VIOLET", "WAVE", "YARD", "ZERO", "TOWER"
    );

    public WordService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(60));
        factory.setReadTimeout(Duration.ofSeconds(60));
        this.restTemplate = new RestTemplate(factory);
    }

    public List<String> getWordsForGame(Difficulty difficulty) {
        /*try {
            int diffLevel = mapDifficulty(difficulty);
            Set<String> words = new HashSet<>();

            // API only supports difficulty for 5 words at a time
            // So we make 5 calls of 5 words each
            while (words.size() < 25) {
                String url = BASE_URL + "?number=5&diff=" + diffLevel;
                String[] batch = restTemplate.getForObject(url, String[].class);
                if (batch != null) {
                    for (String word : batch) {
                        words.add(word.toUpperCase());
                        if (words.size() >= 25) break;
                    }
                }
            }
            log.info("Words for game (difficulty {}): {}", difficulty, words);
            return new ArrayList<>(words);
        } catch (Exception e) {
            // API failed, use fallback
            log.warn("API failed, using fallback words: {}", e.getMessage());
            return getFallbackWords();
        }*/

        return getFallbackWordsFromFile();
    }

    private int mapDifficulty(Difficulty difficulty) {
        switch (difficulty) {
            case EASY: return 1;    // Very common words (e.g., "water", "house")
            case MEDIUM_EASY: return 2;  // Common words
            case MEDIUM: return 3;  // moderate words
            case MEDIUM_HARD: return 4;// Uncommon words
            case HARD: return 5;    // Rare words (e.g., "defenestration")
            default: return 1;
        }
    }

    private List<String> getFallbackWordsFromFile() {
        List<String> words = loadWordsFromFile();
        Collections.shuffle(words);
        return words.subList(0, 25);
    }

    private List<String> loadWordsFromFile() {
        try {
            InputStream inputStream = getClass()
                    .getClassLoader()
                    .getResourceAsStream("wordlist.txt");

            if (inputStream == null) {
                throw new RuntimeException("words.txt not found in resources");
            }

            List<String> words = new ArrayList<>();
            try (Scanner scanner = new Scanner(inputStream)) {
                while (scanner.hasNextLine()) {
                    words.add(scanner.nextLine().trim().toUpperCase());
                }
            }

            return words;
        } catch (Exception e) {
            log.error("Failed to load words from file: {}", e.getMessage());
            return new ArrayList<>(DEFAULT_WORDS); // fallback fallback
        }
    }

    private List<String> getFallbackWords() {
        List<String> words = new ArrayList<>(DEFAULT_WORDS);
        Collections.shuffle(words);
        return words.subList(0, 25);
    }
}
