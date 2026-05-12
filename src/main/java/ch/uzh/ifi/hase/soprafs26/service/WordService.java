package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.Topic;
import ch.uzh.ifi.hase.soprafs26.rest.dto.DatamuseWord;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class WordService {

    private static final Logger log = LoggerFactory.getLogger(WordService.class);

    private final RestTemplate restTemplate;
    private static final String DATAMUSE_URL = "https://api.datamuse.com/words";

    private static final List<String> DEFAULT_WORDS = List.of("APPLE", "BANK", "BRIDGE", "CAR", "CASTLE", "CLOUD", "DIAMOND", "DRAGON", "EAGLE", "ENGINE", "FALCON", "FIRE", "GARDEN", "GHOST", "HAMMER", "HORSE", "ICE", "ISLAND", "JUNGLE", "KING", "KNIGHT", "LAMP", "LEMON", "MARBLE", "MOON", "NEEDLE", "NIGHT", "OCEAN", "OPERA", "PIANO","QUEEN", "RIVER", "STAR", "TREE", "UMBRELLA", "VIOLET", "WAVE", "YARD", "ZERO", "TOWER");

    public WordService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(60));
        factory.setReadTimeout(Duration.ofSeconds(60));
        this.restTemplate = new RestTemplate(factory);
    }

    public List<String> getWordsForGame() {
        return getFallbackWordsFromFile();
    }

    // add words with corresponding topics
    public List<String> getWordsForGame(List<Topic> topics) {

        if (topics == null || topics.isEmpty() || (topics.size() == 1 && topics.contains(Topic.STANDARD))) {
            return getFallbackWordsFromFile();
        }

        // Collect all keywords from selected topics
        List<String> allKeywords = topics.stream()
                .flatMap(t -> t.getKeywords().stream())
                .toList();

        Set<String> topicWords = new HashSet<>();
        for (String keyword : allKeywords) {
            try {

                String encoded = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
                String url = DATAMUSE_URL + "?ml=" + encoded;
                DatamuseWord[] response = restTemplate.getForObject(url, DatamuseWord[].class);

                if (response != null) {
                    for (DatamuseWord dw : response) {
                        String word = dw.word;

                        // Remove plural 's' at the end
                        if (word.endsWith("s") && word.length() > 3) {
                            word = word.substring(0, word.length() - 1);
                        }

                        // Filter
                        if (!word.contains(" ")
                                && word.length() >= 3
                                && word.length() <= 12
                                && word.matches("[a-zA-Z]+")) {

                            topicWords.add(word.toUpperCase());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Datamuse failed for topic '{}': {}", keyword, e.getMessage());
            }
        }

        // Shuffle topic words and take what we need
        List<String> topicList = new ArrayList<>(topicWords);
        Collections.shuffle(topicList);

        // Take topic words (up to topicWordCount)
        List<String> boardWords = new ArrayList<>(topicList.subList(0, 25));

        Collections.shuffle(boardWords); // mix them so topic words aren't grouped
        return boardWords;
    }

    // Words from File
    private List<String> getFallbackWordsFromFile() {
        List<String> words = loadWordsFromFile();
        words.replaceAll(String::toUpperCase);
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
            return new ArrayList<>(DEFAULT_WORDS); // fallback
        }
    }
}