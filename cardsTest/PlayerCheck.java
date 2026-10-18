import java.util.Arrays;
import java.util.List;

public class PlayerCheck {

    public static void main(String[] args) {

        List<Card> startingHand = Arrays.asList(
            new Card(1),
            new Card(2),
            new Card(3),
            new Card(4)
        );

        Player player1 = new Player(1, startingHand);

        System.out.println("Player ID: " + player1.getPlayerId());
        System.out.println(
            "The initial hand: " + player1.getHandSnapshot()
        );

        // Simulation for drawing a card
        player1.receiveCard(new Card(1));

        System.out.println(
            "After the draw: " + player1.getHandSnapshot()
        );

        System.out.println(
            "Has won: " + player1.hasWinningHand()
        );

        // discards the least-common card
        Card discarded = player1.discardLeastCommon();

        System.out.println("Discarded: " + discarded);
        System.out.println(
            "Hand after discard: " + player1.getHandSnapshot()
        );

        // To test winning hand
        Player player2 = new Player(
            2,
            Arrays.asList(
                new Card(2),
                new Card(2),
                new Card(2),
                new Card(2)
            )
        );

        System.out.println(
            "Player 2 is the winner: " + player2.hasWinningHand()
        );
    }
}