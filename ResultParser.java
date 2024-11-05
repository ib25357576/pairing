import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResultParser {
    private BufferedWriter writer;
    private String directory;
    String firstName;
    String secondName;
    private PrintStream Output;

    public ResultParser(String directoryPath, String firstName, String secondName, PrintStream Output) throws Exception {
        this.Output = Output;
        File directory = new File(directoryPath+"/statistics");
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                Output.println("Directory "+directory.getPath()+" created successfully!");
            } else {
                throw new Exception("failed to create directory "+directory.getPath());
            }
        }
        this.directory = directoryPath;
        this.firstName = firstName;
        this.secondName = secondName;
        //writer.write("test num, test type, test bound, move num, turn, move, time, depth, nodes, hit, cp loss\n");
        //writer.write("test num, test type, type bound, result, depth time/move, age time/move, depth depth/move, age depth/move, depth nodes, depth hit, age nodes, age hit, depth cp loss, age cp loss, movecount\n");
    }
    public void finish() throws IOException {
        writer.close();
    }
    private double doubleAfterTarget(String target, String file){
        int index = file.indexOf(target);
        if(index==-1) return -1;
        return doubleFromString(file, index+target.length());
    }
    private double doubleFromString(String str, int startIndex) {
        int endIndex = str.indexOf(' ', startIndex+1);
        if (endIndex == -1) {
            endIndex = str.length();
        }
        String numberStr = str.substring(startIndex, endIndex);
        return Double.parseDouble(numberStr.trim());
    }
    private String wholeFile(String file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder result = new StringBuilder();
        String current = reader.readLine();
        while(current!=null){
            result.append(current).append(" \n");
            current = reader.readLine();
        }
        return String.valueOf(result);
    }
    private void addToFile(String str) throws IOException {
        writer.write(str);
        writer.write(", ");
    }
    private void addToFile(int num) throws IOException {
        writer.write(String.valueOf(num));
        writer.write(", ");
    }
    private void addToFile(double db) throws IOException {
        writer.write(String.valueOf(db));
        writer.write(", ");
    }
    private String lineAfter(String string, int index, int lineNum){
        while(lineNum > 0){
            index = string.indexOf('\n', index)+1;
            lineNum--;
        }
        int end = string.indexOf('\n', index);
        return string.substring(index, end);
    }
    public void parseByGames() throws Exception {
        writer = new BufferedWriter(new FileWriter(directory+"/statistics/stats_games.txt"));
        writer.write("test num, test type, type bound, result, depth time/move, age time/move, depth depth/move, age depth/move, depth nodes, depth hit, age nodes, age hit, depth cp loss, age cp loss, movecount\n");

        File dir = new File(directory+"/analysis");
        String pattern = "match(\\d+)\\.txt";
        Pattern regex = Pattern.compile(pattern);
        File[] files = dir.listFiles();
        if (files == null) {
            throw new Exception("Error reading directory contents");
        }
        for (File file : files) {
            Matcher matcher = regex.matcher(file.getName());
            if (matcher.matches()) {
                int number = Integer.parseInt(matcher.group(1));
                parseGame(number, "analysis/match"+number+".txt");
            }
        }
        writer.close();
    }
    public void parseByMoves() throws Exception {
        writer = new BufferedWriter(new FileWriter(directory+"/statistics/stats_moves.txt"));
        writer.write("test num, test type, test bound, move num, turn, move, time, depth, nodes, hit, cp loss\n");

        File dir = new File(directory+"/analysis");
        String pattern = "match(\\d+).txt";
        Pattern regex = Pattern.compile(pattern);
        File[] files = dir.listFiles();
        if (files == null) {
            throw new Exception("Error reading directory contents");
        }
        for (File file : files) {
            Matcher matcher = regex.matcher(file.getName());
            if (matcher.matches()) {
                int number = Integer.parseInt(matcher.group(1));
                parseGameByMoves(number, "analysis/match"+number+".txt");
            }
        }
        writer.close();
    }
    private void parseGameByMoves(int testNum, String file) throws IOException {
        file = directory + "/"+file;
        List<String> lines = Files.readAllLines(Paths.get(file));
        boolean isFirstTurn = lines.get(0).startsWith(firstName) ^ lines.get(1).contains(" b ");
        String testType = lines.get(0).split(" ")[2];
        String testBound = lines.get(0).split(" ")[3];
        int moveNum=0;
        boolean firstTTOn =true;
        boolean secondTTOn =true;
        for(int i=2;i<lines.size() && !lines.get(i).startsWith("movecount");i++){
            if(lines.get(i).contains("TT")){
                if(firstTTOn && Objects.equals(lines.get(i), "TT off "+firstName)){
                    firstTTOn = false;
                    continue;
                }
                else if(!firstTTOn && Objects.equals(lines.get(i), "TT on "+firstName)){
                    firstTTOn = true;
                    continue;
                }
                else if(secondTTOn && Objects.equals(lines.get(i), "TT off "+secondName)){
                    secondTTOn = false;
                    continue;
                }
                else if(!secondTTOn && Objects.equals(lines.get(i), "TT on "+secondName)){
                    secondTTOn = true;
                    continue;
                }
                else{
                    Output.println("suspicious tt mode behavior in file "+file+"\n");
                    writer.write("\nsuspicious tt behavior\ntest closed\n");
                    writer.close();
                    return;
                }
            }
            if (isFirstTurn && firstTTOn) {
                moveNum++;
                String[] words = lines.get(i).split(" ");
                addToFile(testNum);
                addToFile(testType);
                addToFile(testBound);
                addToFile(moveNum);
                addToFile(firstName);
                for (String word : words) {
                    addToFile(word);
                }
                writer.newLine();
            } else if (!isFirstTurn && secondTTOn) {
                moveNum++;
                String[] words = lines.get(i).split(" ");
                addToFile(testNum);
                addToFile(testType);
                addToFile(testBound);
                addToFile(moveNum);
                addToFile(secondName);
                for (String word : words) {
                    addToFile(word);
                }
                writer.newLine();
            }
            isFirstTurn = !isFirstTurn;
        }
    }
    private void parseGame(int testNum, String filePath) throws IOException {
        filePath = directory + "/"+filePath;
        addToFile(testNum);
        String file = wholeFile(filePath);
        boolean firstIsWhite = file.startsWith(firstName);

        List<String> lines = Files.readAllLines(Paths.get(filePath));
        String testType = lines.get(0).split(" ")[2];
        String testBound = lines.get(0).split(" ")[3];
        addToFile(testType); addToFile(testBound);

        if(file.contains("gameover draw")) addToFile("draw");
        else if(file.contains("gameover "+firstName)) addToFile(firstName+" won");
        else if(file.contains("gameover "+secondName)) addToFile(secondName+" won");
        else if(file.contains("gameover unexpected engine response")){
            if(file.contains("gameover white won")){
                if(firstIsWhite) addToFile(firstName+" won");
                else addToFile(secondName+" won");
            }
            else if(file.contains("gameover black won")){
                if(!firstIsWhite) addToFile(secondName+" won");
                else addToFile(firstName+" won");
            }
            else{
                addToFile(0);
                Output.println("no gameover in file "+ file+"\n");
            }
        }
        else{
            addToFile(0);
            Output.println("no gameover in file "+ file+"\n");
        }

        //time/move
        addToFile(doubleAfterTarget(firstName+" time per move ", file));
        addToFile(doubleAfterTarget(secondName+" time per move ", file));

        //depth/move
        addToFile(doubleAfterTarget(firstName+" depth per move ", file));
        addToFile(doubleAfterTarget(secondName+" depth per move ", file));

        //hit rate
        addToFile(doubleAfterTarget(firstName+" nodes ", file));
        addToFile(doubleAfterTarget(firstName+" hit ", file));
        addToFile(doubleAfterTarget(secondName+" nodes ", file));
        addToFile(doubleAfterTarget(secondName+" hit ", file));

        //cp loss
        addToFile(doubleAfterTarget(firstName+" cp loss ", file));
        addToFile(doubleAfterTarget(secondName+" cp loss ", file));

        // movecount
        addToFile(doubleAfterTarget("movecount : ", file));

        writer.newLine();
    }
}
