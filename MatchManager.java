import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MatchManager {
    private String enginePath = "chess.exe";
    private String stockfishPath = "stockfish-windows-x86-64-avx2.exe";
    private List<String> fens;
    private String directoryPath;
    private String firstName;
    private String secondName;
    private String firstPolicy;
    private String secondPolicy;
    private PrintStream Output;
    private int timeBound;
    private int depthBound;
    private int fenLower;
    private int fenUpper;

    private GameManager manager;
    
    public MatchManager(String directoryPath, String firstName, String secondName, String firstPolicy, String secondPolicy, PrintStream Output, int depthBound, int timeBound, int fenLowerBound, int fenUpperBound) throws Exception {
        this.Output = Output;
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                Output.println("Directory "+directoryPath+" created successfully!");
            } else {
                throw new Exception("failed to create directory "+directoryPath);
            }
        }
        this.directoryPath = directoryPath;
        this.firstName = firstName;
        this.secondName = secondName;
        this.firstPolicy = firstPolicy;
        this.secondPolicy = secondPolicy;
        this.depthBound = depthBound;
        this.timeBound = timeBound;
        fens = Files.readAllLines(Paths.get("silver_op_suite.txt"));
        this.fenLower = fenLowerBound;
        this.fenUpper = fenUpperBound;

        manager = new GameManager(enginePath, directoryPath, Output);
    }
    public void continueMatchUp() throws Exception {
        File directory = new File(directoryPath+"/data");
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                Output.println("Directory "+directory.getPath()+" created successfully!");
            } else {
                throw new Exception("failed to create directory "+directory.getPath());
            }
        }

        String pattern = "match(\\d+)\\.txt";
        Pattern regex = Pattern.compile(pattern);

        File[] files = directory.listFiles();
        if (files == null) {
            throw new Exception("Error reading directory contents");
        }

        int maxNumber = 0;
        for (File file : files) {
            Matcher matcher = regex.matcher(file.getName());
            if (matcher.matches()) {
                int number = Integer.parseInt(matcher.group(1));
                if (number > maxNumber) {
                    maxNumber = number;
                }
            }
        }

        String lastFen = "";
        boolean afterFirst = true;
        String lastFormat = "";
        double lastFormatArg =0;

        if(maxNumber !=0) {
            List<String> lines = Files.readAllLines(Paths.get(directory.getPath()+"/match"+maxNumber+".txt"));
            while(lines.size()<2) {
                maxNumber--;
                lines = Files.readAllLines(Paths.get(directory.getPath()+"/match"+maxNumber+".txt"));
            }
            lastFen = lines.get(1);
            String[] words = lines.get(0).split(" ");
            if(words[0].equals(firstName)) afterFirst = true;
            else if(words[0].equals(secondName)) afterFirst = false;
            else throw new Exception("wrong file format");
            lastFormat = words[2];
            lastFormatArg = Double.parseDouble(words[3]);
        }

        boolean started = lastFen.isEmpty();
        double[] depths = {2,3,4,5,6,7};
        double[] times = {0.2, 0.4, 0.6, 0.8, 1, 1.5, 2, 3, 5, 10, 20};
        int matchNum = maxNumber+1;

        for(int fenNum =fenLower;fenNum<fenUpper;fenNum++) {
            String fen = fens.get(fenNum);
            for(int num=0; num<depths.length && num < depthBound; num++){
                double depth = depths[num];
                System.out.println("Game "+secondName+" vs "+firstName+" for fen #"+fenNum+", depth "+depth+" started");

                if(started){
                    manager.runMatch(fen, matchNum,  firstName, secondName, firstPolicy, secondPolicy,  "depth", depth);

                    StockfishManager stockfishManager = new StockfishManager(stockfishPath, directoryPath, Output);
                    stockfishManager.processGame("data/match"+matchNum+".txt", "analysis/match"+matchNum+".txt", firstName, secondName);
                    stockfishManager.close();
                    stockfishManager = null;

                    matchNum++;
                }

                if(fen.equals(lastFen) && afterFirst && lastFormat.equals("depth") && lastFormatArg == depth) started = true;

                if(started) {
                    manager.runMatch(fen, matchNum, secondName, firstName, secondPolicy, firstPolicy, "depth", depth);

                    StockfishManager stockfishManager = new StockfishManager(stockfishPath, directoryPath, Output);
                    stockfishManager.processGame("data/match"+matchNum+".txt", "analysis/match"+matchNum+".txt", firstName, secondName);
                    stockfishManager.close();
                    stockfishManager = null;

                    matchNum++;
                }

                if(fen.equals(lastFen) && !afterFirst && lastFormat.equals("depth") && lastFormatArg == depth) started = true;
            }

            for(int num=0; num<times.length && num < timeBound; num++){
                double time = times[num];
                System.out.println("Game "+secondName+" vs "+firstName+" for fen #"+fenNum+",time "+time+" started");
                if(started){
                    manager.runMatch(fen, matchNum,  firstName, secondName, firstPolicy, secondPolicy,  "time", time);

                    StockfishManager stockfishManager = new StockfishManager(stockfishPath, directoryPath, Output);
                    stockfishManager.processGame("data/match"+matchNum+".txt", "analysis/match"+matchNum+".txt", firstName, secondName);
                    stockfishManager.close();
                    stockfishManager = null;

                    matchNum++;
                }

                if(fen.equals(lastFen) && afterFirst && lastFormat.equals("time") && lastFormatArg == time) started = true;

                if(started) {
                    manager.runMatch(fen, matchNum, secondName, firstName, secondPolicy, firstPolicy,  "time", time);

                    StockfishManager stockfishManager = new StockfishManager(stockfishPath, directoryPath, Output);
                    stockfishManager.processGame("data/match"+matchNum+".txt", "analysis/match"+matchNum+".txt", firstName, secondName);
                    stockfishManager.close();
                    stockfishManager = null;

                    matchNum++;
                }

                if(fen.equals(lastFen) && !afterFirst && lastFormat.equals("time") && lastFormatArg == time) started = true;
            }
        }

        /*for(int num=0; num<depths.length && num < depthBound; num++){
            double depth = depths[num];
            System.out.println("Match "+secondName+" vs "+firstName+" for depth "+depth+" started");
            for(String fen : fens){
                if(started){
                    manager.runMatch(fen, matchNum,  firstName, secondName, firstPolicy, secondPolicy,  "depth", depth);

                    StockfishManager stockfishManager = new StockfishManager(stockfishPath, directoryPath, Output);
                    stockfishManager.processGame("data/match"+matchNum+".txt", "analysis/match"+matchNum+".txt", firstName, secondName);
                    stockfishManager.close();
                    stockfishManager = null;

                    matchNum++;
                }

                if(fen.equals(lastFen) && afterFirst && lastFormat.equals("depth") && lastFormatArg == depth) started = true;

                if(started) {
                    manager.runMatch(fen, matchNum, secondName, firstName, secondPolicy, firstPolicy, "depth", depth);

                    StockfishManager stockfishManager = new StockfishManager(stockfishPath, directoryPath, Output);
                    stockfishManager.processGame("data/match"+matchNum+".txt", "analysis/match"+matchNum+".txt", firstName, secondName);
                    stockfishManager.close();
                    stockfishManager = null;

                    matchNum++;
                }

                if(fen.equals(lastFen) && !afterFirst && lastFormat.equals("depth") && lastFormatArg == depth) started = true;
            }
        }
        for(int num=0; num<times.length && num < timeBound; num++){
            double time = times[num];
            System.out.println("Match "+secondName+" vs "+firstName+" for time "+time+" started");
            for(String fen : fens){
                if(started){
                    manager.runMatch(fen, matchNum,  firstName, secondName, firstPolicy, secondPolicy,  "time", time);

                    StockfishManager stockfishManager = new StockfishManager(stockfishPath, directoryPath, Output);
                    stockfishManager.processGame("data/match"+matchNum+".txt", "analysis/match"+matchNum+".txt", firstName, secondName);
                    stockfishManager.close();
                    stockfishManager = null;

                    matchNum++;
                }

                if(fen.equals(lastFen) && afterFirst && lastFormat.equals("time") && lastFormatArg == time) started = true;

                if(started) {
                    manager.runMatch(fen, matchNum, secondName, firstName, secondPolicy, firstPolicy,  "time", time);

                    StockfishManager stockfishManager = new StockfishManager(stockfishPath, directoryPath, Output);
                    stockfishManager.processGame("data/match"+matchNum+".txt", "analysis/match"+matchNum+".txt", firstName, secondName);
                    stockfishManager.close();
                    stockfishManager = null;

                    matchNum++;
                }

                if(fen.equals(lastFen) && !afterFirst && lastFormat.equals("time") && lastFormatArg == time) started = true;
            }
        }*/

        ResultParser parser = new ResultParser(directoryPath, firstName, secondName, Output);
        parser.parseByGames();
        parser.parseByMoves();
        parser.finish();
        parser = null;
        manager.close();
    }
}
