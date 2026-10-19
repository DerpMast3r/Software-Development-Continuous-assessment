import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CardDeckJUnitTest {

    @Test 
    void testFIFO() {
        CardDeck deck = new CardDeck(1);

        deck.add(new Card(10));
        deck.add(new Card(20));

        assertEquals(10, deck.draw().getValue());
        assertEquals(20, deck.draw().getValue());
    }

    @Test 
    void testEmptyDeck() {
        CardDeck deck = new CardDeck(1);

        assertThrows(IllegalStateException.class, () -> deck.draw());
    }

    @Test 
    void testSingleCardFIFO() {
        CardDeck deck = new CardDeck(1);

        deck.add(new Card(42));

        assertEquals(42, deck.draw().getValue());
    }

    @Test 
    void testDuplicateCardValues() {
        CardDeck deck = new CardDeck(1);

        deck.add(new Card(5));
        deck.add(new Card(5));
        deck.add(new Card(9));

        assertEquals(5, deck.draw().getValue());
        assertEquals(5, deck.draw().getValue());
        assertEquals(9, deck.draw().getValue());
    }

    @Test 
    void testDrawAfterDeckBecomesEmpty() {
        CardDeck deck = new CardDeck(1);

        deck.add(new Card(7));

        assertEquals(7, deck.draw().getValue());

        assertThrows(IllegalStateException.class, () -> deck.draw());
    }
}
