package assignments;

import exe.ex3.game.Game;
import exe.ex3.game.GhostCL;
import exe.ex3.game.PacManAlgo;
import exe.ex3.game.PacmanGame;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.Scanner;

public class myMain {
    PacmanGame game = new Game();
    int[][] board = game.getGame(0);
    Map m = new Map(board);
    public static void saveMap(Map2D map, String mapFileName) {
        if (map == null || mapFileName == null) return;

        try (FileWriter writer = new FileWriter(new File(mapFileName))) {
            for (int row = 0; row < map.getHeight(); row++) {
                for (int col = 0; col < map.getWidth(); col++) {
                    writer.write(String.valueOf(map.getPixel(col, row)));
                    if (col < map.getWidth() - 1) writer.write(" ");
                }
                writer.write("\n");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed saving map to: " + mapFileName, e);
        }
    }
    private static int getHeightFromFile(File f) throws FileNotFoundException {
        int lines = 0;
        try (Scanner sc = new Scanner(f)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (!line.isEmpty()) lines++;
            }
        }
        return lines;
    }

    /**
     * Counts integers in the first non-empty line to determine map width.
     * Assumes all lines have a consistent width.
     */
    private static int getWidthFromFile(File f) throws FileNotFoundException {
        try (Scanner sc = new Scanner(f)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (line.isEmpty()) continue;

                Scanner rowScanner = new Scanner(line);
                int count = 0;
                while (rowScanner.hasNextInt()) {
                    rowScanner.nextInt();
                    count++;
                }
                rowScanner.close();
                return count;
            }
        }
        return 0;
    }
    private static int[] getArrFromFileLine(String line, int width) {
        int[] ans = new int[width];
        if (line == null) return ans;

        Scanner sc = new Scanner(line);
        int i = 0;

        while (sc.hasNext() && i < width) {
            if (sc.hasNextInt()) {
                ans[i] = sc.nextInt();
                i++;
            } else {
                sc.next();
            }
        }
        sc.close();
        return ans;
    }
    public static void play(){

    }

}
