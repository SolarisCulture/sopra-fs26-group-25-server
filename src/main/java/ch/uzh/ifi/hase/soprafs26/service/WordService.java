package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.Topic;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DatamuseWord;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class WordService {

    private static final Logger log = LoggerFactory.getLogger(WordService.class);
    private static final String DATAMUSE_URL = "https://api.datamuse.com/words";
    private static final int BOARD_SIZE = 25;

    private final RestTemplate restTemplate;
    private final Map<Topic, List<String>> wordsByTopic = new EnumMap<>(Topic.class);

    public WordService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restTemplate = new RestTemplate(factory);
    }

    // ==================== Public API ====================

    public List<String> getWordsForGame() {
        return getRandomWordsFromFile(BOARD_SIZE);
    }

    public List<String> getWordsForGame(List<Topic> topics) {
        if (topics == null || topics.isEmpty() || (topics.size() == 1 && topics.contains(Topic.STANDARD))) {
            return getRandomWordsFromFile(BOARD_SIZE);
        }

        // 1. Try Datamuse API
        Set<String> words = fetchFromDatamuse(topics);

        // 2. Fill from topic-specific fallback lists
        if (words.size() < BOARD_SIZE) {
            List<String> topicFallback = getWordsForTopics(topics);
            for (String word : topicFallback) {
                if (words.add(word) && words.size() >= BOARD_SIZE) break;
            }
        }

        // 3. Fill from general word list
        if (words.size() < BOARD_SIZE) {
            List<String> generalFallback = getRandomWordsFromFile(BOARD_SIZE * 2);
            for (String word : generalFallback) {
                if (words.add(word) && words.size() >= BOARD_SIZE) break;
            }
        }

        List<String> boardWords = new ArrayList<>(words);
        Collections.shuffle(boardWords);
        return boardWords.subList(0, Math.min(BOARD_SIZE, boardWords.size()));
    }

    // ==================== Datamuse ====================

    private Set<String> fetchFromDatamuse(List<Topic> topics) {
        List<String> allKeywords = topics.stream()
                .flatMap(t -> t.getKeywords().stream())
                .toList();

        Set<String> words = new HashSet<>();
        for (String keyword : allKeywords) {
            try {
                String encoded = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
                String url = DATAMUSE_URL + "?rel_trg=" + encoded;
                DatamuseWord[] response = restTemplate.getForObject(url, DatamuseWord[].class);

                if (response != null) {
                    for (DatamuseWord dw : response) {
                        String word = dw.word;
                        if (!word.contains(" ")
                                && word.length() >= 3
                                && word.length() <= 12
                                && word.matches("[a-zA-Z]+")) {
                            words.add(word.toUpperCase());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Datamuse failed for '{}': {}", keyword, e.getMessage());
            }
        }
        return words;
    }

    // ==================== Topic word lists ====================

    @PostConstruct
    public void loadWordLists() {
        try {
            ClassPathResource resource = new ClassPathResource("topics.txt");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
            );

            Topic currentTopic = null;
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("Topic:")) {
                    String topicName = line.substring(6).trim().toUpperCase()
                            .replace(" & ", "_")
                            .replace("& ", "_")
                            .replace(" ", "_");
                    try {
                        currentTopic = Topic.valueOf(topicName);
                        wordsByTopic.putIfAbsent(currentTopic, new ArrayList<>());
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown topic in file: {}", topicName);
                        currentTopic = null;
                    }
                    continue;
                }

                if (line.startsWith("Words:") && currentTopic != null) {
                    String wordsPart = line.substring(6);
                    String[] words = wordsPart.split(",");
                    for (String word : words) {
                        String cleaned = word.trim().toUpperCase();
                        if (!cleaned.isEmpty()
                                && !cleaned.contains(" ")
                                && cleaned.matches("[A-Z]+")
                                && cleaned.length() >= 2
                                && cleaned.length() <= 15) {
                            wordsByTopic.get(currentTopic).add(cleaned);
                        }
                    }
                }
            }
            reader.close();

            log.info("Loaded topic word lists: {}", wordsByTopic.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue().size())
                    .toList());

        } catch (Exception e) {
            log.error("Failed to load topic word lists: {}", e.getMessage());
        }
    }

    private List<String> getWordsForTopics(List<Topic> topics) {
        Set<String> words = new HashSet<>();
        for (Topic topic : topics) {
            List<String> topicWords = wordsByTopic.getOrDefault(topic, Collections.emptyList());
            words.addAll(topicWords);
        }
        List<String> shuffled = new ArrayList<>(words);
        Collections.shuffle(shuffled);
        return shuffled;
    }

    // ==================== General word list ====================

    private List<String> getRandomWordsFromFile(int count) {
        List<String> words = loadWordsFromFile();
        Collections.shuffle(words);
        return words.subList(0, Math.min(count, words.size()));
    }

    private List<String> loadWordsFromFile() {
        try {
            InputStream inputStream = getClass()
                    .getClassLoader()
                    .getResourceAsStream("wordlist.txt");

            if (inputStream == null) {
                log.error("wordlist.txt not found in resources");
                return new ArrayList<>(getDefaultWords());
            }

            List<String> words = new ArrayList<>();
            try (Scanner scanner = new Scanner(inputStream)) {
                while (scanner.hasNextLine()) {
                    String word = scanner.nextLine().trim().toUpperCase();
                    if (!word.isEmpty()) {
                        words.add(word);
                    }
                }
            }
            return words;

        } catch (Exception e) {
            log.error("Failed to load words from file: {}", e.getMessage());
            return new ArrayList<>(getDefaultWords());
        }
    }

    private List<String> getDefaultWords() {
        return List.of(
                "APPLE", "BANK", "BRIDGE", "CAR", "CASTLE",
                "CLOUD", "DIAMOND", "DRAGON", "EAGLE", "ENGINE",
                "FALCON", "FIRE", "GARDEN", "GHOST", "HAMMER",
                "HORSE", "ICE", "ISLAND", "JUNGLE", "KING",
                "KNIGHT", "LAMP", "LEMON", "MARBLE", "MOON",
                "NEEDLE", "NIGHT", "OCEAN", "OPERA", "PIANO",
                "QUEEN", "RIVER", "STAR", "TREE", "UMBRELLA"
        );
    }
}