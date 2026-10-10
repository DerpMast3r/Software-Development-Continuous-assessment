import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class CardDeck {
    private final int id;
    private final Deque<Card> cards = new ArrayDeque<>();

    public CardDeck(int id) {
        this.id = id;
    }

    public synchronized void add(Card card) {
        cards.addLast(card);
    }

    public synchronized Card draw() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("Deck " + id + " is empty");
        }
        return cards.removeFirst();
    }

    public synchronized boolean isEmpty() {
        return cards.isEmpty();
    }

    public synchronized List<Card> snapshot() {
        return new ArrayList<>(cards);
    }

    public int getId() {
        return id;
    }
}