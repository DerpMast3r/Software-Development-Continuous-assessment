import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * End-to-end tests for CardGame. Feeds CardGame.main() a scripted stdin
 * (n and pack path) and verifies stdout + the produced output files.
 *
 * Tests:
 *   1. n=4, immediate win (player 1 dealt four 1s)
 *   2. n=4, deterministic mid-game win (player 1 has 3 ones, deck 1 top is 1)
 *   3. n=1, boundary: single player cycles into four 1s
 *
 * Run: java CardGameCheck
 */
public class CardGameCheck {

    public static void main(String[] args) throws Exception {
        testImmediateWin4Players();
        testDeterministicWin4Players();
        testSinglePlayer();
        System.out.println("CardGameCheck: all passed");
    }

    // ---------------------------------------------------------------- tests

    private static void testImmediateWin4Players() throws Exception {
        System.out.println("  [1] n=4, immediate win: player 1 dealt four 1s");
        deleteOutputs(4);
        // Deal order: p1<-pack[0], p2<-pack[1], p3<-pack[2], p4<-pack[3], then wrap.
        // p1 gets pack[0], pack[4], pack[8], pack[12] = 1,1,1,1 -> immediate win.
        int[] pack = {
            1,  2,  3,  4,
            1,  5,  6,  7,
            1,  8,  9, 10,
            1, 11, 12, 13,
            14, 15, 16, 17,
            18, 19, 20, 21,
            22, 23, 24, 25,
            26, 27, 28, 29
        };
        File packFile = writePack(pack);
        String stdout = runGame(4, packFile);

        check(stdout.contains("player 1 wins"),
              "stdout missing 'player 1 wins'; got: " + stdout);

        // Winner file: exactly 4 lines
        List<String> p1 = readLines("player1_output.txt");
        check(p1.size() == 4, "player1 file should have 4 lines, had " + p1.size());
        check(p1.get(0).equals("player 1 initial hand 1 1 1 1"),
              "p1[0] = " + p1.get(0));
        check(p1.get(1).equals("player 1 wins"), "p1[1] = " + p1.get(1));
        check(p1.get(2).equals("player 1 exits"), "p1[2] = " + p1.get(2));
        check(p1.get(3).equals("player 1 final hand: 1 1 1 1"),
              "p1[3] = " + p1.get(3));

        // Loser files: 4 lines each
        for (int i = 2; i <= 4; i++) {
            List<String> pi = readLines("player" + i + "_output.txt");
            check(pi.size() == 4,
                  "player" + i + " should have 4 lines, had " + pi.size());
            check(pi.get(1).equals("player 1 has informed player " + i
                                   + " that player 1 has won"),
                  "player" + i + "[1] = " + pi.get(1));
            check(pi.get(2).equals("player " + i + " exits"),
                  "player" + i + "[2] = " + pi.get(2));
            check(pi.get(3).startsWith("player " + i + " hand: "),
                  "player" + i + "[3] = " + pi.get(3));
            checkHandHasFourValues(pi.get(3), "player " + i + " hand");
        }

        for (int i = 1; i <= 4; i++) {
            check(new File("deck" + i + "_output.txt").exists(),
                  "deck" + i + "_output.txt missing");
        }

        packFile.delete();
        deleteOutputs(4);
    }

    private static void testDeterministicWin4Players() throws Exception {
        System.out.println("  [2] n=4, player 1 wins on first turn");
        deleteOutputs(4);
        // p1 = [1,1,1,2]; deck 1 top = 1 -> p1 draws 1, discards 2 (only
        // non-preferred), reaches four 1s. Only 2 is non-preferred, so
        // random discard collapses to a single choice -> deterministic.
        int[] pack = {
            1,  3,  4,  5,
            1,  6,  7,  8,
            1,  9, 10, 11,
            2, 12, 13, 14,
            1, 15, 16, 17,
            18, 19, 20, 21,
            22, 23, 24, 25,
            26, 27, 28, 29
        };
        File packFile = writePack(pack);
        String stdout = runGame(4, packFile);

        check(stdout.contains("player 1 wins"),
              "stdout missing 'player 1 wins'; got: " + stdout);

        List<String> p1 = readLines("player1_output.txt");
        check(p1.size() == 7, "player1 file should have 7 lines, had " + p1.size());
        check(p1.get(0).equals("player 1 initial hand 1 1 1 2"),
              "p1[0] = " + p1.get(0));
        check(p1.get(1).equals("player 1 draws a 1 from deck 1"),
              "p1[1] = " + p1.get(1));
        check(p1.get(2).equals("player 1 discards a 2 to deck 2"),
              "p1[2] = " + p1.get(2));
        check(p1.get(3).equals("player 1 current hand is 1 1 1 1"),
              "p1[3] = " + p1.get(3));
        check(p1.get(4).equals("player 1 wins"), "p1[4] = " + p1.get(4));
        check(p1.get(5).equals("player 1 exits"), "p1[5] = " + p1.get(5));
        check(p1.get(6).equals("player 1 final hand: 1 1 1 1"),
              "p1[6] = " + p1.get(6));

        for (int i = 2; i <= 4; i++) {
            List<String> pi = readLines("player" + i + "_output.txt");
            int sz = pi.size();
            check(sz >= 4, "player" + i + " should have >= 4 lines, had " + sz);
            check(pi.get(sz - 3).equals("player 1 has informed player " + i
                                        + " that player 1 has won"),
                  "player" + i + " penultimate-2 = " + pi.get(sz - 3));
            check(pi.get(sz - 2).equals("player " + i + " exits"),
                  "player" + i + " penultimate = " + pi.get(sz - 2));
            check(pi.get(sz - 1).startsWith("player " + i + " hand: "),
                  "player" + i + " last = " + pi.get(sz - 1));
            checkHandHasFourValues(pi.get(sz - 1), "player " + i + " final hand");
        }

        for (int i = 1; i <= 4; i++) {
            List<String> di = readLines("deck" + i + "_output.txt");
            check(di.size() == 1, "deck" + i + " file should have 1 line");
            check(di.get(0).startsWith("deck" + i + " contents: "),
                  "deck" + i + " = " + di.get(0));
        }

        packFile.delete();
        deleteOutputs(4);
    }

    private static void testSinglePlayer() throws Exception {
        System.out.println("  [3] n=1, player 1 cycles into four 1s");
        deleteOutputs(1);
        // p1 hand = [1,2,3,4]; deck 1 = [1,1,1,1].
        // Each turn draws 1, discards a random non-preferred card
        // (anything in {2,3,4}); after 3 turns the hand is four 1s.
        // Final deck CONTENTS are {1,2,3,4} but ORDER is non-deterministic
        // because the discard target among {2,3,4} is chosen randomly.
        int[] pack = {1, 2, 3, 4, 1, 1, 1, 1};
        File packFile = writePack(pack);
        String stdout = runGame(1, packFile);

        check(stdout.contains("player 1 wins"),
              "stdout missing 'player 1 wins'; got: " + stdout);

        List<String> p1 = readLines("player1_output.txt");
        check(p1.size() == 13,
              "player1 file should have 13 lines, had " + p1.size());
        check(p1.get(0).equals("player 1 initial hand 1 2 3 4"),
              "p1[0] = " + p1.get(0));
        check(p1.get(p1.size() - 3).equals("player 1 wins"),
              "p1 third-from-last = " + p1.get(p1.size() - 3));
        check(p1.get(p1.size() - 2).equals("player 1 exits"),
              "p1 second-from-last = " + p1.get(p1.size() - 2));
        check(p1.get(p1.size() - 1).equals("player 1 final hand: 1 1 1 1"),
              "p1 last = " + p1.get(p1.size() - 1));

        List<String> d1 = readLines("deck1_output.txt");
        check(d1.size() == 1, "deck1 file should have 1 line");
        check(d1.get(0).startsWith("deck1 contents: "), "deck1 = " + d1.get(0));
        String body = d1.get(0).substring("deck1 contents: ".length()).trim();
        String[] parts = body.split("\\s+");
        check(parts.length == 4, "deck1 should have 4 cards, had " + parts.length);
        Set<Integer> contents = new TreeSet<>();
        for (String p : parts) contents.add(Integer.parseInt(p));
        Set<Integer> expected = new TreeSet<>(Arrays.asList(1, 2, 3, 4));
        check(contents.equals(expected),
              "deck1 should contain {1,2,3,4}, was " + contents);

        packFile.delete();
        deleteOutputs(1);
    }

    // -------------------------------------------------------------- helpers

    private static String runGame(int n, File packFile) throws Exception {
        PrintStream originalOut = System.out;
        InputStream originalIn = System.in;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(captured, true));
            String input = n + "\n" + packFile.getAbsolutePath() + "\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            CardGame.main(new String[]{});
        } finally {
            System.setOut(originalOut);
            System.setIn(originalIn);
        }
        return captured.toString();
    }

    private static File writePack(int[] values) throws IOException {
        File f = File.createTempFile("pack", ".txt");
        f.deleteOnExit();
        try (PrintWriter w = new PrintWriter(new FileWriter(f))) {
            for (int v : values) w.println(v);
        }
        return f;
    }

    private static List<String> readLines(String path) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = r.readLine()) != null) lines.add(line);
        }
        return lines;
    }

    private static void deleteOutputs(int n) {
        for (int i = 1; i <= n; i++) {
            new File("player" + i + "_output.txt").delete();
            new File("deck" + i + "_output.txt").delete();
        }
    }

    private static void checkHandHasFourValues(String line, String label) {
        int idx = line.lastIndexOf(": ");
        check(idx > 0, label + " has no ': ' separator: " + line);
        String values = line.substring(idx + 2).trim();
        String[] parts = values.split("\\s+");
        check(parts.length == 4,
              label + " should have 4 values, had " + parts.length + ": " + line);
    }

    private static void check(boolean cond, String msg) {
        if (!cond) throw new AssertionError("FAILED: " + msg);
    }
}