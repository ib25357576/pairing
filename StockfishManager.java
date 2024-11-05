import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import static java.lang.Integer.min;
import static java.lang.Integer.parseInt;

public class StockfishManager {
    private Process stockfishProcess;
    private BufferedReader stockfishReader;
    private PrintWriter stockfishWriter;
    private final int checkmateScore = 1000;
    private String directory;
    private boolean isWindows;
    private PrintStream Output;

    public StockfishManager(String stockfishPath, String directoryPath, PrintStream Output) throws Exception {
        this.Output = Output;
        isWindows = System.getProperty("os.name").toLowerCase().contains("windows");

        File directory = new File(directoryPath+"/analysis");
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                Output.println("Directory "+directory.getPath()+" created successfully!");
            } else {
                throw new Exception("failed to create directory "+directory.getPath());
            }
        }
        this.directory = directoryPath;
        ProcessBuilder stockfishBuilder = isWindows? new ProcessBuilder(stockfishPath) : new ProcessBuilder("wine", stockfishPath);
        stockfishProcess = stockfishBuilder.start();
        stockfishReader = new BufferedReader(new InputStreamReader(stockfishProcess.getInputStream()));
        stockfishWriter = new PrintWriter(new OutputStreamWriter(stockfishProcess.getOutputStream()), true);
        stockfishWriter.println("isready");
        String line;
        if((line = stockfishReader.readLine()) == null){
            close();
            throw new Exception("stockfish process is not responding");
        }
        if(!line.equals("readyok")){
            line = stockfishReader.readLine();
            if(!line.equals("readyok")){
                close();
                throw new Exception("stockfish wrong validation : \n"+line);
            }
        }
        Output.println("stockfish init successfully");
    }

    private String readResponse() throws IOException {
        StringBuilder string = new StringBuilder();
        while(string.indexOf("bestmove") == -1){
            string.append(stockfishReader.readLine());
            string.append("\n");
        }
        //Output.println(string);
        return string.toString();
    }

    public void close() throws IOException {
        stockfishWriter.close();
        stockfishReader.close();
        stockfishProcess.destroy();
    }

    private int numFromString(String str, int startIndex) {
        int space = str.indexOf(' ', startIndex);
        int endl = str.indexOf('\n', startIndex);
        int endIndex;
        if(space == -1 && endl == -1) endIndex = str.length() - 1;
        else if(space == -1) endIndex = endl;
        else if(endl == -1) endIndex = space;
        else endIndex = min(endl, space);
        String numberStr = str.substring(startIndex, endIndex);
        return Integer.parseInt(numberStr.trim());
    }

    private long cpLossOnMove(String setup, String move) {
        stockfishWriter.println(setup);
        stockfishWriter.println("go depth 12");
        String response;
        try{
            response = readResponse();
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        int startEval =0;
        int index = response.lastIndexOf("score ");
        String type = response.substring(index + 6, index + 8);
        if(type.equals("cp")){
            startEval = numFromString(response, index+9);
        }
        else if(type.equals("ma")){
            int result = numFromString(response, index+11);
            if(result>0) startEval = checkmateScore - numFromString(response, index+11);
            else startEval = -result - checkmateScore;
        }
        else {
            Output.println("failed to get eval");
            return -1;
        }
        stockfishWriter.println("go depth 12 searchmoves "+move);
        try{
            response = readResponse();
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        int endEval =0;
        index = response.lastIndexOf("score ");
        type = response.substring(index + 6, index + 8);
        if(type.equals("cp")){
            endEval = numFromString(response, index+9);
        }
        else if(type.equals("ma")){
            int result = numFromString(response, index+11);
            if(result>0) endEval = checkmateScore - numFromString(response, index+11);
            else endEval = -result - checkmateScore;
        }
        else {
            Output.println("failed to get eval");
            return -1;
        }
        //Output.println(move + " : start "+startEval+" end "+endEval);
        if(endEval > startEval) return 0;
        int absoluteLoss = (startEval - endEval);
        if(endEval < 0 && startEval > 0) return absoluteLoss;
        double coeff = (endEval>0) ? endEval : -startEval;
        coeff = (checkmateScore-coeff)/checkmateScore;
        return Math.round(absoluteLoss*coeff);
    }

    private int getPosEval(String setup){
        Output.println(setup);
        stockfishWriter.println(setup);
        stockfishWriter.println("go");
        String response;
        try{
            response = readResponse();
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
        int index = response.lastIndexOf("score ");
        String type = response.substring(index + 6, index + 8);
        if(type.equals("cp")){
            return numFromString(response, index+9);
        }
        else if(type.equals("ma")){
            int result = numFromString(response, index+11);
            if(result>0) return checkmateScore - numFromString(response, index+11);
            else return -result - checkmateScore;
        }
        else {
            Output.println("failed to get eval");
        }
        return 0;
    }

    private String getMoveFromLine(String line){
        int index =0;
        while(line.charAt(index)!=' '){
            index++;
        }
        return line.substring(0, index);
    }

    private int depthFromLine(String line){
        String[] words = line.split(" ");
        return parseInt(words[2]);
    }

    public void processGame(String filepath, String outpath, String firstName, String secondName) throws IOException{
        Output.println("<------------------------------------------------>\nfile "+filepath+" analysis started\n");
        filepath = directory + "/" + filepath;
        outpath = directory + "/" + outpath;
        List<String> lines = Files.readAllLines(Paths.get(filepath));
        BufferedWriter writer = new BufferedWriter(new FileWriter(outpath));
        writer.write(lines.get(0));
        writer.newLine();
        writer.write(lines.get(1));
        writer.newLine();
        String fen = lines.get(1);
        StringBuilder setCommand = new StringBuilder("position fen " + fen + " moves");
        int lineNum = 2;
        boolean isFirstTurn = (fen.contains(" b ")) ^ lines.get(0).startsWith(firstName);
        long firstCpLoss =0;
        long secondCpLoss =0;
        long currentLoss =0;
        int firstMoves =0;
        int secondMoves =0;
        boolean firstTTOn =true;
        boolean secondTTOn =true;
        while(!lines.get(lineNum).contains("gameover")){
            writer.write(lines.get(lineNum));
            if(lines.get(lineNum).contains("TT")){
                writer.newLine();
                if(firstTTOn && Objects.equals(lines.get(lineNum), "TT off "+firstName)){
                    firstTTOn = false;
                    lineNum++;
                    continue;
                }
                else if(!firstTTOn && Objects.equals(lines.get(lineNum), "TT on "+firstName)){
                    firstTTOn = true;
                    lineNum++;
                    continue;
                }
                else if(secondTTOn && Objects.equals(lines.get(lineNum), "TT off "+secondName)){
                    secondTTOn = false;
                    lineNum++;
                    continue;
                }
                else if(!secondTTOn && Objects.equals(lines.get(lineNum), "TT on "+secondName)){
                    secondTTOn = true;
                    lineNum++;
                    continue;
                }
                else{
                    Output.println("suspicious tt mode behavior in file "+filepath+"\n");
                    writer.write("\nsuspicious tt behavior\ntest closed\n");
                    writer.close();
                    return;
                }
            }
            currentLoss = cpLossOnMove(setCommand.toString(), getMoveFromLine(lines.get(lineNum)));
            writer.write(' ');
            writer.write(String.valueOf(currentLoss));
            writer.newLine();
            if (isFirstTurn && firstTTOn) {
                firstCpLoss += currentLoss;
                firstMoves++;
            } else if (!isFirstTurn && secondTTOn) {
                secondCpLoss += currentLoss;
                secondMoves++;
            }
            setCommand.append(" ");
            setCommand.append(getMoveFromLine(lines.get(lineNum)));
            isFirstTurn = !isFirstTurn;
            lineNum++;
        }
        writer.write("movecount : "+(firstMoves+secondMoves)+"\n");
        Output.println(firstCpLoss);
        Output.println(secondCpLoss);
        Output.println(firstMoves);
        Output.println(secondMoves);
        writer.write(firstName+" cp loss ");
        writer.write(Double.toString((double) firstCpLoss /firstMoves));
        writer.newLine();
        writer.write(secondName+" cp loss ");
        writer.write(Double.toString((double) secondCpLoss /secondMoves));
        for(int i=lineNum;i<lines.size();i++){
            writer.newLine();
            writer.write(lines.get(i));
        }
        writer.close();
    }
}
