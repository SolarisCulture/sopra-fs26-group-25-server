package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.rest.dto.DatamuseWord;
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
    private static final String DATAMUSE_URL = "https://api.datamuse.com/words";

    private static final List<String> DEFAULT_WORDS = List.of(
            "APPLE", "BANK", "BRIDGE", "CAR", "CASTLE", "CLOUD", "DIAMOND", "DRAGON", "EAGLE", "ENGINE",
            "FALCON", "FIRE", "GARDEN", "GHOST", "HAMMER", "HORSE", "ICE", "ISLAND", "JUNGLE", "KING",
            "KNIGHT", "LAMP", "LEMON", "MARBLE", "MOON", "NEEDLE", "NIGHT", "OCEAN", "OPERA", "PIANO",
            "QUEEN", "RIVER", "STAR", "TREE", "UMBRELLA", "VIOLET", "WAVE", "YARD", "ZERO", "TOWER"
    );

    public WordService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(60));
        factory.setReadTimeout(Duration.ofSeconds(60));
        this.restTemplate = new RestTemplate(factory);
    }

    public List<String> getWordsForGame() {
        return getFallbackWordsFromFile();
    }

    public List<String> getWordsForGame(List<String> topics) {

        int topicWordCount = 15;

        Set<String> topicWords = new HashSet<>();
        for (String topic : topics) {
            try {
                String url = DATAMUSE_URL + "?rel_trg=" + topic;
                DatamuseWord[] response = restTemplate.getForObject(url, DatamuseWord[].class);

                if (response != null) {
                    for (DatamuseWord dw : response) {
                        if (!dw.word.contains(" ") && dw.word.length() >= 3 && dw.word.length() <= 12) {
                            topicWords.add(dw.word.toUpperCase());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Datamuse failed for topic '{}': {}", topic, e.getMessage());
            }
        }

        List<String> topicList = new ArrayList<>(topicWords);
        Collections.shuffle(topicList);

        int actualTopicCount = Math.min(topicWordCount, topicList.size());
        List<String> boardWords = new ArrayList<>(topicList.subList(0, actualTopicCount));

        List<String> defaults = getFallbackWordsFromFile();
        defaults.removeAll(boardWords);
        int remaining = 25 - boardWords.size();
        boardWords.addAll(defaults.subList(0, remaining));

        Collections.shuffle(boardWords);
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
