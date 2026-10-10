import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class GameState {
    private final AtomicBoolean gameOver = new AtomicBoolean(false);
    private final AtomicInteger winner = new AtomicInteger(0);

    public boolean isGameOver() {
        return gameOver.get();
    }

    public int getWinner() {
        return winner.get();
    }

    public boolean declareWinner(int playerId) {
        if (gameOver.compareAndSet(false, true)) {
            winner.set(playerId);
            return true;
        }
        return false;
    }
}