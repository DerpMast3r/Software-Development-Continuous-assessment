import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class Player extends Thread {

    private final int id;
    private final int preferredValue;
    private final List<Card> hand;

    private final int numberOfPlayers;
    private final List<CardDeck> decks;
    private final GameState state;

    // The constructor for use of the PlayerCheck test
    public Player(int id, List<Card> initialHand) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "The Player ID has to be positive"
            );
        }

        if (initialHand == null || initialHand.size() != 4) {
            throw new IllegalArgumentException(
                    "A player must start with exactly four cards"
            );
        }

        this.id = id;
        this.preferredValue = id;
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
                    "The ID of the Player must be positive"
            );
        }

        if (numberOfPlayers <= 0
                || decks == null
                || decks.size() != numberOfPlayers
                || state == null) {
            throw new IllegalArgumentException(
                    "The game setup is invalid"
            );
        }

        this.id = id;
        this.preferredValue = id;
        this.hand = new ArrayList<>(initialHand);

        this.numberOfPlayers = numberOfPlayers;
        this.decks = decks;
        this.state = state;
    }

    public int getPlayerId() {
        return id;
    }

    public int getPreferredValue() {
        return preferredValue;
    }

    public synchronized List<Card> getHandSnapshot() {
        return new ArrayList<>(hand);
    }

    public synchronized void receiveCard(Card card) {
        if (card == null) {
            throw new IllegalArgumentException(
                    "The card cannot be null"
            );
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

    public synchronized Card discardedNonPreferred() {
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);

            if (card.getValue() != preferredValue) {
                return hand.remove(i);
            }
        }

        throw new IllegalStateException(
                "No card that is non-preferred is available to discard"
        );
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

        try (PrintWriter output
                = new PrintWriter(new FileWriter(fileName))) {

            output.println(
                    "player " + id + " initial hand " + handAsString()
            );

            // For winner found before the threads start
            if (state.isGameOver()) {
                finishGame(output);
                return;
            }

            boolean declaredWinner = false;

            while (!state.isGameOver()) {
                CardDeck drawDeck = decks.get(id - 1);
                CardDeck discardDeck
                        = decks.get(id % numberOfPlayers);

                // To make both decks locked in the ID order to avoid deadlock
                CardDeck firstLock = drawDeck.getId()
                        < discardDeck.getId()
                        ? drawDeck : discardDeck;

                CardDeck secondLock = firstLock == drawDeck
                        ? discardDeck : drawDeck;

                synchronized (firstLock) {
                    synchronized (secondLock) {

                        // Another play may have won while we were waiting
                        if (state.isGameOver()) {
                            continue;
                        }

                        Card drawn = drawDeck.draw();

                        synchronized (this) {
                            hand.add(drawn);
                        }

                        Card discarded = discardedNonPreferred();
                        discardDeck.add(discarded);

                        output.println(
                                "player " + id + " draws a "
                                + drawn.getValue() + " from deck "
                                + drawDeck.getId()
                        );

                        output.println(
                                "player " + id + " current hand is "
                                + handAsString()
                        );

                        if (hasWinningHand()
                                && state.declareWinner(id)) {
                            output.println("player " + id + " wins");
                            System.out.println("player " + id + " wins");
                            declaredWinner = true;
                        }
                    }
                }
            }

            if (declaredWinner) {
                output.println("player " + id + " leaves");
                output.println(
                        "player " + id + " final hand: "
                        + handAsString()
                );
            } else {
                finishGame(output);
            }

        } catch (IOException e) {
            System.err.println(
                    "Was not able to write " + fileName + ": " + e.getMessage()
            );
        }
    }

    private void finishGame(PrintWriter output) {
        int winner = state.getWinner();

        if (winner == id) {
            output.println("player " + id + " wins");
            System.out.println("player " + id + " wins");
            output.println("player " + id + " leaves");
            output.println(
                    "player " + id + " final hand: " + handAsString()
            );
        } else {
            output.println(
                    "player " + winner + " has told player " + id
                    + " that player " + winner + " has won"
            );
            output.println("player " + id + " leaves");
            output.println(
                    "player " + id + " final hand: " + handAsString()
            );

        }
    }
}
