import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ChessStatsMerger {

    public void merge(String chessTestDir) {
        Map<String, StringBuilder> combinedGames = new HashMap<>();
        Map<String, StringBuilder> combinedMoves = new HashMap<>();

        try {
            // Traverse the chessTest directory
            Files.walk(Paths.get(chessTestDir))
                    .filter(Files::isDirectory)
                    .forEach(dir -> {
                        String dirName = dir.getFileName().toString();

                        // Find the last underscore and split accordingly
                        int lastUnderscoreIndex = dirName.lastIndexOf('_');
                        if (lastUnderscoreIndex != -1) {
                            String testName = dirName.substring(0, lastUnderscoreIndex);
                            Path statsFolder = dir.resolve("statistics");

                            // Initialize StringBuilder for this test if it doesn't exist
                            combinedGames.putIfAbsent(testName, new StringBuilder());
                            combinedMoves.putIfAbsent(testName, new StringBuilder());

                            // Read and combine stats_games and stats_moves
                            try {
                                combineFiles(statsFolder.resolve("stats_games.txt"), combinedGames.get(testName));
                                combineFiles(statsFolder.resolve("stats_moves.txt"), combinedMoves.get(testName));
                            } catch (IOException e) {
                                System.err.println("Error reading files in " + statsFolder + ": " + e.getMessage());
                            }
                        }
                    });

            // Write combined stats to output files
            for (String testName : combinedGames.keySet()) {
                writeToFile(testName + "_total_stats_games.txt", combinedGames.get(testName).toString());
                writeToFile(testName + "_total_stats_moves.txt", combinedMoves.get(testName).toString());
            }

            System.out.println("Combining completed successfully.");
        } catch (IOException e) {
            System.err.println("Error traversing directory: " + e.getMessage());
        }
    }

    private void combineFiles(Path filePath, StringBuilder combinedContent) throws IOException {
        if (Files.exists(filePath)) {
            try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    combinedContent.append(line).append(System.lineSeparator());
                }
            }
        }
    }

    private void writeToFile(String fileName, String content) {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileName))) {
            writer.write(content);
        } catch (IOException e) {
            System.err.println("Error writing to file " + fileName + ": " + e.getMessage());
        }
    }
}
