import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for GameState: atomic winner declaration and the awaitCard /
 * signalChange wakeup protocol.
 *
 * Run: java GameStateCheck
 */
public class GameStateCheck {

    public static void main(String[] args) throws Exception {
        testInitialState();
        testSingleDeclare();
        testConcurrentDeclare();
        testAwaitCardFastPaths();
        testAwaitCardBlocksThenUnblocks();
        testAwaitCardUnblocksOnGameOver();
        testMultipleWaitersAllUnblock();
        System.out.println("GameStateCheck: all passed");
    }

    // ---------------------------------------------------------------- tests

    private static void testInitialState() {
        System.out.println("  [1] initial state");
        GameState s = new GameState();
        check(!s.isGameOver(), "should not be game over initially");
        check(s.getWinner() == 0, "initial winner should be 0, was " + s.getWinner());
    }

    private static void testSingleDeclare() {
        System.out.println("  [2] single declareWinner");
        GameState s = new GameState();
        check(s.declareWinner(3), "first declareWinner should return true");
        check(s.isGameOver(), "game should be over");
        check(s.getWinner() == 3, "winner should be 3, was " + s.getWinner());
        check(!s.declareWinner(4), "second declareWinner should return false");
        check(s.getWinner() == 3, "winner must remain 3, was " + s.getWinner());
    }

    private static void testConcurrentDeclare() throws Exception {
        System.out.println("  [3] concurrent declareWinner (10 threads)");
        final int n = 10;
        final GameState s = new GameState();
        final CountDownLatch ready = new CountDownLatch(n);
        final CountDownLatch go = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(n);
        final AtomicInteger trueCount = new AtomicInteger(0);
        final AtomicInteger winningId = new AtomicInteger(0);

        for (int i = 1; i <= n; i++) {
            final int id = i;
            new Thread(() -> {
                ready.countDown();
                try { go.await(); } catch (InterruptedException e) { return; }
                if (s.declareWinner(id)) {
                    trueCount.incrementAndGet();
                    winningId.set(id);
                }
                done.countDown();
            }).start();
        }
        ready.await();
        go.countDown();
        check(done.await(5, TimeUnit.SECONDS), "threads did not finish in 5s");
        check(trueCount.get() == 1,
              "exactly one thread should win, was " + trueCount.get());
        check(winningId.get() == s.getWinner(),
              "winningId " + winningId.get() + " != getWinner() " + s.getWinner());
    }

    /**
     * Combined [4]/[5]/[6]: awaitCard's three fast paths.
     *  - game over + empty deck      -> false
     *  - game over + non-empty deck  -> false
     *  - live game + non-empty deck  -> true
     */
    private static void testAwaitCardFastPaths() throws Exception {
        System.out.println("  [4] awaitCard fast paths");

        // (a) game over, empty deck
        GameState s1 = new GameState();
        s1.declareWinner(1);
        check(!s1.awaitCard(new CardDeck(1)),
              "game over + empty deck should return false");

        // (b) game over, non-empty deck
        GameState s2 = new GameState();
        CardDeck nonEmpty1 = new CardDeck(1);
        nonEmpty1.add(new Card(7));
        s2.declareWinner(1);
        check(!s2.awaitCard(nonEmpty1),
              "game over + non-empty deck should return false");

        // (c) live game, non-empty deck
        GameState s3 = new GameState();
        CardDeck nonEmpty2 = new CardDeck(1);
        nonEmpty2.add(new Card(7));
        check(s3.awaitCard(nonEmpty2),
              "live game + non-empty deck should return true");
    }

    private static void testAwaitCardBlocksThenUnblocks() throws Exception {
        System.out.println("  [5] awaitCard blocks, then signalChange+card wakes it");
        GameState s = new GameState();
        CardDeck deck = new CardDeck(1);         // empty
        final AtomicInteger result = new AtomicInteger(-1);
        Thread t = new Thread(() -> {
            try { result.set(s.awaitCard(deck) ? 1 : 0); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        t.start();
        Thread.sleep(300);
        check(t.isAlive(), "thread should be blocked on empty deck");
        check(result.get() == -1, "thread should not have returned yet");

        deck.add(new Card(7));
        s.signalChange();
        t.join(2000);
        check(!t.isAlive(), "thread should have unblocked");
        check(result.get() == 1, "awaitCard should have returned true");
    }

    private static void testAwaitCardUnblocksOnGameOver() throws Exception {
        System.out.println("  [6] awaitCard unblocks with false when game ends");
        GameState s = new GameState();
        CardDeck deck = new CardDeck(1);         // empty
        final AtomicInteger result = new AtomicInteger(-1);
        Thread t = new Thread(() -> {
            try { result.set(s.awaitCard(deck) ? 1 : 0); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        t.start();
        Thread.sleep(300);
        check(t.isAlive(), "thread should be blocked");

        s.declareWinner(1);
        s.signalChange();
        t.join(2000);
        check(!t.isAlive(), "thread should have unblocked on game over");
        check(result.get() == 0, "awaitCard should have returned false");
    }

    private static void testMultipleWaitersAllUnblock() throws Exception {
        System.out.println("  [7] multiple waiters all unblock on one signal");
        final int numWaiters = 5;
        final GameState s = new GameState();
        final CardDeck deck = new CardDeck(1);   // empty
        final AtomicInteger unblocked = new AtomicInteger(0);
        final AtomicInteger trueCount = new AtomicInteger(0);

        for (int i = 0; i < numWaiters; i++) {
            new Thread(() -> {
                try {
                    if (s.awaitCard(deck)) trueCount.incrementAndGet();
                    unblocked.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
        Thread.sleep(300);
        check(unblocked.get() == 0, "no waiter should have returned yet");

        deck.add(new Card(7));
        s.signalChange();
        Thread.sleep(300);
        check(unblocked.get() == numWaiters,
              "all waiters should return, got " + unblocked.get());
        check(trueCount.get() == numWaiters,
              "all should return true, got " + trueCount.get());
    }

    // -------------------------------------------------------------- helpers

    private static void check(boolean cond, String msg) {
        if (!cond) throw new AssertionError("FAILED: " + msg);
    }
}