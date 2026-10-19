import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for CardDeck: FIFO semantics, snapshot isolation, empty-draw
 * behaviour, concurrent safety, and the awaitCard/signalChange
 * producer-consumer path.
 *
 * Run: java CardDeckCheck
 */
public class CardDeckCheck {

    public static void main(String[] args) throws Exception {
        testFifoOrder();
        testSnapshotIsCopy();
        testEmptyDrawThrows();
        testConcurrentAddThenDraw();
        testProducerConsumerViaAwait();
        System.out.println("CardDeckCheck: all passed");
    }

    // ---------------------------------------------------------------- tests

    private static void testFifoOrder() {
        System.out.println("  [1] FIFO order");
        CardDeck deck = new CardDeck(1);
        deck.add(new Card(10));
        deck.add(new Card(20));
        deck.add(new Card(30));

        check(deck.draw().getValue() == 10, "first draw should be 10");
        check(deck.draw().getValue() == 20, "second draw should be 20");
        check(deck.draw().getValue() == 30, "third draw should be 30");
        check(deck.isEmpty(), "deck should be empty after three draws");
    }

    private static void testSnapshotIsCopy() {
        System.out.println("  [2] snapshot is an independent copy");
        CardDeck deck = new CardDeck(1);
        deck.add(new Card(1));
        deck.add(new Card(2));
        deck.add(new Card(3));

        List<Card> snap = deck.snapshot();
        check(snap.size() == 3, "snapshot should have 3 cards");
        snap.clear(); // mutate the returned list
        check(snap.isEmpty(), "mutated snapshot should be empty");

        List<Card> snap2 = deck.snapshot();
        check(snap2.size() == 3,
              "deck must be unaffected by mutating snapshot, was " + snap2.size());
        check(deck.draw().getValue() == 1,
              "deck order preserved after snapshot mutation");
    }

    private static void testEmptyDrawThrows() {
        System.out.println("  [3] draw on empty deck throws IllegalStateException");
        CardDeck deck = new CardDeck(1);
        check(deck.isEmpty(), "new deck should be empty");
        try {
            deck.draw();
            throw new AssertionError("FAILED: draw() on empty did not throw");
        } catch (IllegalStateException expected) {
            // ok
        }
    }

    private static void testConcurrentAddThenDraw() throws Exception {
        System.out.println("  [4] 10 threads x 1000 adds, then 10 x 1000 draws");
        final int threads = 10;
        final int perThread = 1000;
        final CardDeck deck = new CardDeck(1);

        runParallel(threads, () -> {
            for (int i = 0; i < perThread; i++) deck.add(new Card(i % 10));
        });

        check(deck.snapshot().size() == threads * perThread,
              "expected " + (threads * perThread) + " cards, had "
                      + deck.snapshot().size());

        runParallel(threads, () -> {
            for (int i = 0; i < perThread; i++) {
                deck.draw(); // must not throw
            }
        });

        check(deck.isEmpty(),
              "deck should be empty after all draws, had " + deck.snapshot().size());
    }

    private static void testProducerConsumerViaAwait() throws Exception {
        System.out.println("  [5] producer wakes a consumer via signalChange");
        GameState state = new GameState();
        CardDeck deck = new CardDeck(1); // empty

        final AtomicInteger result = new AtomicInteger(-1);
        Thread consumer = new Thread(() -> {
            try {
                result.set(state.awaitCard(deck) ? 1 : 0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        consumer.start();
        Thread.sleep(300);
        check(consumer.isAlive(), "consumer should be blocked on empty deck");

        // Producer: add a card and signal
        deck.add(new Card(42));
        state.signalChange();

        consumer.join(2000);
        check(!consumer.isAlive(), "consumer should have woken");
        check(result.get() == 1, "awaitCard should return true");
        check(deck.draw().getValue() == 42, "consumer should see the added card");
    }

    // -------------------------------------------------------------- helpers

    private static void runParallel(int threads, Runnable body) throws Exception {
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            new Thread(() -> {
                ready.countDown();
                try { go.await(); } catch (InterruptedException e) { return; }
                body.run();
                done.countDown();
            }).start();
        }
        ready.await();
        go.countDown();
        check(done.await(10, TimeUnit.SECONDS),
              "parallel workers did not finish in 10s");
    }

    private static void check(boolean cond, String msg) {
        if (!cond) throw new AssertionError("FAILED: " + msg);
    }
}