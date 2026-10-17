import java.util.concurrent.atomic.AtomicInteger;

public class GameState {
    // 0 = no winner yet (players are 1-based)
    private final AtomicInteger winner = new AtomicInteger(0);

    public boolean isGameOver() {
        return winner.get() != 0;
    }

    public int getWinner() {
        return winner.get();
    }

    /**
     * Atomically declares playerId the winner iff no winner has been declared yet.
     * @return true if this call was the one that declared the winner.
     */
    public boolean declareWinner(int playerId) {
        if (playerId <= 0) {
            throw new IllegalArgumentException("Player id must be positive");
        }
        return winner.compareAndSet(0, playerId);
    }

    public synchronized boolean awaitCard(CardDeck deck)
            throws InterruptedException {
        while (!isGameOver() && deck.isEmpty()) {
            wait();
        }

        return !isGameOver();
    }

    public synchronized void signalChange() {
        notifyAll();
    }
}
