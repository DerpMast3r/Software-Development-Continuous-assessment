import java.util.concurrent.atomic.AtomicInteger;

public class GameState {

    // Zero will mean that the game is still running
    private final AtomicInteger winner = new AtomicInteger(0);

    public boolean isGameOver() {
        return winner.get() != 0;
    }

    public int getWinner() {
        return winner.get();
    }

    public boolean declareWinner(int playerId) {
        if (playerId <= 0) {
            throw new IllegalArgumentException(
                "The Player ID has to be positive"
            );
        }

        // Only a singular thread will change the winner from 0 to player ID
        return winner.compareAndSet(0, playerId);
    }
}
