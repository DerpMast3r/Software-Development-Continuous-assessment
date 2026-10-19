import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class GameStateJUnitTest {

    @Test 
    void testInitialGameState() {
        GameState state = new GameState();

        assertEquals(0, state.getWinner());
        assertFalse(state.isGameOver());
    }

    @Test 
    void testOnlyWinnerCanBeDeclared() {
        GameState state = new GameState();

        assertTrue(state.declareWinner(1));
        assertFalse(state.declareWinner(2));

        assertEquals(1, state.getWinner());
        assertTrue(state.isGameOver());
    }

    @Test 
    void testConcurrentWinnerDeclarations() throws InterruptedException {
        GameState state = new GameState();
        AtomicInteger successfulDeclarations = new AtomicInteger(0);
        CountDownLatch start = new CountDownLatch(1);

        Thread player1 = new Thread(() -> {
            try {
                start.await();
                if (state.declareWinner(1)) {
                    successfulDeclarations.incrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread player2 = new Thread(() -> {
            try {
                start.await();
                if (state.declareWinner(2)) {
                    successfulDeclarations.incrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        player1.start();
        player2.start();

        start.countDown();

        player1.join();
        player2.join();

        assertEquals(1, successfulDeclarations.get());
        assertTrue(state.getWinner() == 1 || state.getWinner() == 2);
        assertTrue(state.isGameOver());
    }    
}
