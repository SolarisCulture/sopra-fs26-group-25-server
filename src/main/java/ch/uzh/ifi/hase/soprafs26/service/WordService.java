package ch.uzh.ifi.hase.soprafs26.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class WordService {
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

    // Call this method also from WordServiceTest
    public List<String> getWordsForGame() {
        List<String> pool = new ArrayList<>(DEFAULT_WORDS);
        Collections.shuffle(pool);
        return pool.subList(0, 25);
    }
}
