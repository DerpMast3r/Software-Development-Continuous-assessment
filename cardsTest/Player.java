    import java.util.ArrayList;
    import java.util.List;

    public class Player {

        private final int id;
        private final int preferredValue;
        private final List<Card> hand;

        public Player(int id, List<Card> initialHand) {
            if (id <= 0) {
                throw new IllegalArgumentException(
                    "The Player ID has to be positive"
                );
            }

            if (initialHand == null || initialHand.size() != 4) {
                throw new IllegalArgumentException(
                    "A player has to begin with four cards exactly"
                );
            }

            this.id = id;
            this.preferredValue = id;
            this.hand = new ArrayList<>(initialHand);
        }

        public int getId() {
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
                    "Card cannot be null"
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

    public synchronized Card discardNonPreferred() {
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);

            if (card.getValue() != preferredValue) {
                return hand.remove(i);
            }
        }

        throw new IllegalStateException(
            "No non-preferred card is available to discard"
        );
    }
}
