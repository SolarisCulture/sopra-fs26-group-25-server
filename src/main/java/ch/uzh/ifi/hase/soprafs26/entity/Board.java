package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "board")
public class Board {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Used when the database automatically generates the primary key using auto-increment for each new record
    private Long id;

    @OneToOne(mappedBy = "board")
    private Game game;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "board_id")
    @OrderColumn(name = "position") // need to save order in case player reload page
    private List<WordCard> cards = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Game getGame() { return game; }
    public void setGame(Game game) { this.game = game; }

    public List<WordCard> getCards() { return cards; }
    public void setCards(List<WordCard> cards) { this.cards = cards; }
}
