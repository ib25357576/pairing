import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        String err = "error.txt";
        File file = new File(err);
        if (!file.exists()) {
            if (file.createNewFile()) {
                System.out.println("Directory "+err+" created successfully");
            } else {
                System.out.println("Failed to create directory "+err);
                return;
            }
        }
        System.setErr(new PrintStream(new FileOutputStream(err, true)));

        List<String> lines = Files.readAllLines(Paths.get("input.txt"));
        ExecutorService executorService = Executors.newFixedThreadPool(lines.size());
        CountDownLatch latch = new CountDownLatch(lines.size());
        for (String line : lines) {
            String[] words = line.split(" ");
            Task task = new Task(words[0], words[1], words[2], words[3], words[4], Integer.parseInt(words[5]), Integer.parseInt(words[6]), Integer.parseInt(words[7]), Integer.parseInt(words[8]), Boolean.parseBoolean(words[9]), latch);
            executorService.submit(task);
        }

        executorService.shutdown();
        latch.await();
        ChessStatsMerger merger = new ChessStatsMerger();
        merger.merge("vm");

        /*String firstName = "depth";
        String secondName = "mixed";
        String firstPolicy = "depth";
        String secondPolicy = "mixed";
        String directory = "vm";
        try {
            File dir = new File(directory);
            if (!dir.exists()) {
                if (dir.mkdirs()) {
                    System.out.println("Directory "+directory+" created successfully");
                } else {
                    throw new Exception("Failed to create directory "+directory);
                }
            }
            MatchManager manager = new MatchManager(directory, firstName, secondName, firstPolicy, secondPolicy);
            manager.continueMatchUp();
            System.out.println(firstName + " " + secondName + " match finished successfully");


            System.out.println("task finished successfully");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }*/

            /*Path dir = Paths.get("depth vs depth new\\data");

            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // Check if the file name matches the pattern "match*number*.txt"
                    if (file.getFileName().toString().matches("match.*\\d+.*\\.txt")) {
                        replaceDepthInFile(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });*/

            /*ResultParser parserGames = new ResultParser("statistics\\stats_games.txt", 38, firstPolicy+" vs "+secondPolicy+" new");
            parserGames.startGameParse(firstPolicy, secondPolicy);
            for(int num =1000;num<1045;num++){
                System.out.println(num);
                String file = "data\\match"+num+".txt";
                parserGames.parseDataByGames(file, firstPolicy, secondPolicy);
            }
            parserGames.finish();*/
            /*ResultParser parserMoves = new ResultParser("statistics\\stats_moves.txt", 0, firstPolicy+" vs "+secondPolicy);
            parserMoves.startMoveParse(firstPolicy, secondPolicy);
            for(int num =0;num<658;num++){
                System.out.println(num);
                String file = "analysis\\match"+num+".txt";
                parserMoves.parseDataByMoves(file, firstPolicy, secondPolicy);
            }
            parserMoves.finish();*/

            /*StockfishManager stockfishManager = new StockfishManager("C:\\Users\\kiril\\Downloads\\stockfish-windows-x86-64-avx2\\stockfish\\stockfish-windows-x86-64-avx2.exe", "C:\\Users\\kiril\\IdeaProjects\\untitled\\"+firstPolicy+" vs "+secondPolicy+" new");
            for(int i=455;i<495;i++){
                stockfishManager.processGame("data\\match"+i+".txt", "analysis\\match"+i+".txt", firstPolicy, secondPolicy);
            }
            stockfishManager.close();*/

            /*int matchNum =1000;
            String startFromFen = "r1b1kbnr/pp3ppp/1qn1p3/3pP3/2pP4/P1P2N2/1P3PPP/RNBQKB1R w KQkq - 0 7";

            double[] depths = {2,3,4,5,6,7,8,9,10,11,12};
            double[] times = {0.2, 0.4, 0.6, 0.8, 1.0, 1.5, 2, 2.5, 3, 5, 7.5, 10};
            boolean started = true;
            for(int i=2;i<3;i++){
                BufferedReader reader = new BufferedReader(new FileReader("silver_op_suite.txt"));
                String fen = reader.readLine();
                for(int j=0;j<25;j++){
                    if(started) {
                        GameManager manager1 = new GameManager("C:\\Users\\kiril\\source\\repos\\chess\\x64\\Release\\chess.exe", directory;
                        manager1.runMatch(fen, matchNum, "depth1", "depth2", firstPolicy, secondPolicy, "time", times[i]);
                        manager1.close();
                        manager1 = null;

                        matchNum++;
                    }

                    if(started) {
                        GameManager manager2 = new GameManager("C:\\Users\\kiril\\source\\repos\\chess\\x64\\Release\\chess.exe", directory);
                        manager2.runMatch(fen, matchNum, "depth2", "depth1", secondPolicy, firstPolicy, "time", times[i]);
                        manager2.close();
                        manager2 = null;

                        matchNum++;
                    }

                    //if(Objects.equals(fen, startFromFen)) started = true;

                    fen = reader.readLine();
                }
                reader.close();
                reader = null;

                BufferedReader reader2 = new BufferedReader(new FileReader("silver_op_suite.txt"));
                fen = reader2.readLine();
                for(int j=0;j<25;j++){
                    if(started){
                        GameManager manager3 = new GameManager("C:\\Users\\kiril\\source\\repos\\chess\\x64\\Release\\chess.exe", firstPolicy+" vs "+secondPolicy+" new\\data");
                        manager3.runMatch(fen, matchNum,  firstPolicy, secondPolicy,  "depth", depths[i]);
                        manager3.close();
                        manager3 = null;

                        matchNum++;
                    }

                    //if(Objects.equals(fen, startFromFen)) started = true;

                    if(started) {
                        GameManager manager4 = new GameManager("C:\\Users\\kiril\\source\\repos\\chess\\x64\\Release\\chess.exe", firstPolicy+" vs "+secondPolicy+" new\\data");
                        manager4.runMatch(fen, matchNum,  secondPolicy, firstPolicy,  "depth", depths[i]);
                        manager4.close();
                        manager4 = null;

                        matchNum++;
                    }
                    fen = reader2.readLine();
                }
                reader2.close();
                reader2 = null;
            }

            System.out.println("task successful");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/
    /*private static void replaceDepthInFile(Path file) throws IOException {
        // Read all lines from the file
        List<String> lines = Files.readAllLines(file);
        boolean modified = false;

        // Modify the lines that contain "depth "
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("depth ")) {
                lines.set(i, lines.get(i).replace("depth ", "depth"));
                modified = true;
            }
        }

        // If any modifications were made, write the modified lines back to the file
        if (modified) {
            Files.write(file, lines);
            System.out.println("Updated file: " + file.toString());
        }
    }*/
    }
}
