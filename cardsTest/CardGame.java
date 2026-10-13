import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class CardGame {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        int n = readNumberOfPlayers(scanner);
        List<Card> pack = readPack(scanner, n);

        GameState state = new GameState();

        // Create decks
        List<CardDeck> decks = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            decks.add(new CardDeck(i));
        }

        // Distribute hands
        List<List<Card>> hands = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            hands.add(new ArrayList<>());
        }

        int index = 0;
        for (int round = 0; round < 4; round++) {
            for (int p = 0; p < n; p++) {
                hands.get(p).add(pack.get(index++));
            }
        }

        // Fill decks with remaining cards round-robin
        int remaining = pack.size() - index;
        for (int i = 0; i < remaining; i++) {
            int deckIndex = i % n;
            decks.get(deckIndex).add(pack.get(index + i));
        }

        // Check for immediate winner
        int immediateWinner = findImmediateWinner(hands);
        if (immediateWinner != -1) {
            state.declareWinner(immediateWinner);
        }

        // Create and start player threads
        List<Player> players = new ArrayList<>();
        try {
            for (int i = 0; i < n; i++) {
                Player p = new Player(i + 1, n, hands.get(i), decks, state);
                players.add(p);
            }
            for (Player p : players) {
                p.start();
            }
            for (Player p : players) {
                p.join();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Write deck outputs
        for (CardDeck deck : decks) {
            writeDeckOutput(deck);
        }

        scanner.close();
    }

    private static int readNumberOfPlayers(Scanner scanner) {
        while (true) {
            System.out.print("Enter number of players: ");
            String line = scanner.nextLine().trim();
            try {
                int n = Integer.parseInt(line);
                if (n > 0) {
                    return n;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Invalid number of players. Please enter a positive integer.");
        }
    }

    private static List<Card> readPack(Scanner scanner, int n) {
        int expected = 8 * n;

        while (true) {
            System.out.print("Enter path to input pack file: ");
            String path = scanner.nextLine().trim();
            File file = new File(path);

            if (!file.exists() || !file.isFile() || !file.canRead()) {
                System.out.println("Invalid pack file. Please provide a valid pack file.");
                continue;
            }

            try {
                List<String> lines = new ArrayList<>();
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        lines.add(line);
                    }
                }

                if (lines.size() != expected) {
                    System.out.println("Invalid pack file. Expected " + expected + " rows, found " + lines.size() + ".");
                    continue;
                }

                List<Card> pack = new ArrayList<>();
                boolean valid = true;

                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) {
                        valid = false;
                        break;
                    }
                    try {
                        int value = Integer.parseInt(trimmed);
                        if (value < 0) {
                            valid = false;
                            break;
                        }
                        pack.add(new Card(value));
                    } catch (NumberFormatException e) {
                        valid = false;
                        break;
                    }
                }

                if (!valid) {
                    System.out.println("Invalid pack file. Each row must contain a single non-negative integer.");
                    continue;
                }

                return pack;
            } catch (IOException e) {
                System.out.println("Error reading pack file. Please provide a valid pack file.");
            }
        }
    }

    private static int findImmediateWinner(List<List<Card>> hands) {
        for (int i = 0; i < hands.size(); i++) {
            Map<Integer, Integer> freq = new HashMap<>();
            for (Card c : hands.get(i)) {
                freq.merge(c.getValue(), 1, Integer::sum);
            }
            for (int count : freq.values()) {
                if (count == 4) {
                    return i + 1;
                }
            }
        }
        return -1;
    }

    private static void writeDeckOutput(CardDeck deck) {
        try (PrintWriter out = new PrintWriter(new FileWriter("deck" + deck.getId() + "_output.txt"))) {
            List<Card> cards = deck.snapshot();
            StringBuilder sb = new StringBuilder();
            sb.append("deck").append(deck.getId()).append(" contents: ");
            for (int i = 0; i < cards.size(); i++) {
                if (i > 0) sb.append(' ');
                sb.append(cards.get(i).getValue());
            }
            out.println(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}