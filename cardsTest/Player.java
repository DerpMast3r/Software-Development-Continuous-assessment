import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Player extends Thread {

    private final int id;
    private final List<Card> hand;

    private final int numberOfPlayers;
    private final List<CardDeck> decks;
    private final GameState state;

    // The constructor for use of the PlayerCheck test
    public Player(int id, List<Card> initialHand) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "The Player ID has to be positive");
        }
        if (initialHand == null || initialHand.size() != 4) {
            throw new IllegalArgumentException(
                    "A player must start with exactly four cards");
        }

        this.id = id;
        this.hand = new ArrayList<>(initialHand);

        this.numberOfPlayers = 0;
        this.decks = null;
        this.state = null;
    }

    // The constructor for use of CardGame
    public Player(int id, int numberOfPlayers,
                  List<Card> initialHand,
                  List<CardDeck> decks, GameState state) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "The ID of the Player must be positive");
        }
        if (numberOfPlayers <= 0
                || decks == null
                || decks.size() != numberOfPlayers
                || state == null) {
            throw new IllegalArgumentException("The game setup is invalid");
        }

        this.id = id;
        this.hand = new ArrayList<>(initialHand);

        this.numberOfPlayers = numberOfPlayers;
        this.decks = decks;
        this.state = state;
    }

    public int getPlayerId() {
        return id;
    }

    public synchronized List<Card> getHandSnapshot() {
        return new ArrayList<>(hand);
    }

    public synchronized void receiveCard(Card card) {
        if (card == null) {
            throw new IllegalArgumentException("The card cannot be null");
        }
        hand.add(card);
    }

    public synchronized boolean hasWinningHand() {
        for (Card candidate : hand) {
            int count = 0;
            for (Card card : hand) {
                if (card.getValue() == candidate.getValue()) {
                    count++;
                }
            }
            if (count >= 4) {
                return true;
            }
        }
        return false;
    }

    /**
     * Discards the card whose value occurs least often in the hand.
     * If several values tie for "least often", the first such card
     * (scanning from the front of the hand) is removed.
     */
    public synchronized Card discardLeastCommon() {
        Map<Integer, Integer> freq = new HashMap<>();
        for (Card c : hand) {
            freq.merge(c.getValue(), 1, Integer::sum);
        }

        int minCount = Integer.MAX_VALUE;
        for (int count : freq.values()) {
            if (count < minCount) {
                minCount = count;
            }
        }

        for (int i = 0; i < hand.size(); i++) {
            if (freq.get(hand.get(i).getValue()) == minCount) {
                return hand.remove(i);
            }
        }
        throw new IllegalStateException("Hand is empty");
    }

    private synchronized String handAsString() {
        StringBuilder result = new StringBuilder();
        for (Card card : hand) {
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(card.getValue());
        }
        return result.toString();
    }

    @Override
    public void run() {
        // This is a constructor made of two arguments for the PlayerCheck only
        if (numberOfPlayers == 0) {
            return;
        }

        String fileName = "player" + id + "_output.txt";

        try (PrintWriter output = new PrintWriter(new FileWriter(fileName))) {

            output.println("player " + id + " initial hand " + handAsString());

            // For winner found before the threads start
            if (state.isGameOver()) {
                finishGame(output);
                return;
            }

            boolean declaredWinner = false;

            while (!state.isGameOver()) {
                CardDeck drawDeck = decks.get(id - 1);
                CardDeck discardDeck = decks.get(id % numberOfPlayers);

                // Wait for card without holding each deck lock
                try {
                    if (!state.awaitCard(drawDeck)) {
                        break;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                // Acquire deck locks in ascending deck-id order to prevent
                // deadlock when two players share adjacent decks.
                // the per-deck locking below lets non-adjacent
                // players act concurrently.
                CardDeck first =
                        drawDeck.getId() <= discardDeck.getId() ? drawDeck : discardDeck;
                CardDeck second =
                        drawDeck.getId() <= discardDeck.getId() ? discardDeck : drawDeck;

                boolean wonThisTurn = false;

                synchronized (first) {
                    synchronized (second) {
                        if (state.isGameOver()) {
                            break;
                        }

                        // Another player can use the card
                        // (i.e. another thread may have drained the deck
                        // between awaitCard returning and us taking the lock)
                        if (drawDeck.isEmpty()) {
                            continue;
                        }

                        Card drawn = drawDeck.draw();
                        receiveCard(drawn);

                        Card discarded = discardLeastCommon();
                        discardDeck.add(discarded);

                        output.println("player " + id + " draws a "
                                + drawn.getValue() + " from deck "
                                + drawDeck.getId());
                        output.println("player " + id + " discards a "
                                + discarded.getValue() + " to deck "
                                + discardDeck.getId());
                        output.println("player " + id + " current hand is "
                                + handAsString());
                        output.flush();

                        if (hasWinningHand() && state.declareWinner(id)) {
                            output.println("player " + id + " wins");
                            System.out.println("player " + id + " wins");
                            wonThisTurn = true;
                        }
                    }
                }

                // A finished turn can make a card available to a different player
                // Players waiting on empty decks can be made to exit
                state.signalChange();

                if (wonThisTurn) {
                    declaredWinner = true;
                    break;
                }
            }

            if (declaredWinner) {
                output.println("player " + id + " exits");
                output.println("player " + id + " final hand: " + handAsString());
            } else {
                finishGame(output);
            }

        } catch (IOException e) {
            System.err.println(
                    "Was not able to write " + fileName + ": " + e.getMessage());
        }
    }

    private void finishGame(PrintWriter output) {
        int winner = state.getWinner();

        if (winner == id) {
            output.println("player " + id + " wins");
            System.out.println("player " + id + " wins");
            output.println("player " + id + " exits");
            output.println("player " + id + " final hand: " + handAsString());
        } else {
            output.println("player " + winner + " has informed player " + id
                    + " that player " + winner + " has won");
            output.println("player " + id + " exits");
            output.println("player " + id + " hand: " + handAsString());
        }
        output.flush();
    }
}