import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GameManager {
    private Process engine1;
    private BufferedReader engine1Reader;
    private PrintWriter engine1Writer;
    private Process engine2;
    private BufferedReader engine2Reader;
    private PrintWriter engine2Writer;
    private List<String> keyHolder = new ArrayList<>(); // for repetition break
    private String directoryPath; //is actually folder\data
    private boolean isWindows; // false for linux
    private PrintStream Output;

    public GameManager(String enginePath, String directoryPath, PrintStream Output) throws Exception {
        this.Output = Output;
        String os = System.getProperty("os.name");
        Output.println(os);
        isWindows = os.toLowerCase().contains("windows");

        File directory = new File(directoryPath+"/data");
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                Output.println("Directory "+directory.getPath()+" created successfully!");
            } else {
                throw new Exception("failed to create directory "+directory.getPath());
            }
        }
        this.directoryPath = directory.getPath();

        List<String> command = new ArrayList<>();
        if(isWindows){
            command.add("cmd");
            command.add("/c");
            command.add("start");
            command.add("/HIGH");
            command.add("/B");
            command.add(enginePath);
        }else{
            command.add("wine");
            command.add(enginePath);
        }

        ProcessBuilder engine1Builder = new ProcessBuilder(command);
        engine1 = engine1Builder.start();
        engine1Reader = new BufferedReader(new InputStreamReader(engine1.getInputStream()));
        engine1Writer = new PrintWriter(new OutputStreamWriter(engine1.getOutputStream()), true);

        ProcessBuilder engine2Builder = new ProcessBuilder(command);
        engine2 = engine2Builder.start();
        engine2Reader = new BufferedReader(new InputStreamReader(engine2.getInputStream()));
        engine2Writer = new PrintWriter(new OutputStreamWriter(engine2.getOutputStream()), true);

        engine1Writer.println("isready");
        String line;
        if((line = engine1Reader.readLine()) == null){
            close();
            throw new Exception("engine 1 process is not responding");
        }
        if(!line.equals("readyok")){
            close();
            throw new Exception("engine 1 wrong validation :\n"+line);
        }
        else Output.println("engine 1 init successful");

        engine2Writer.println("isready");
        if((line = engine2Reader.readLine()) == null){
            close();
            throw new Exception("engine 2 process is not responding");
        }
        if(!line.equals("readyok")){
            close();
            throw new Exception("engine 2 wrong validation :\n"+line);
        }
        else Output.println("engine 2 init successful");
    }

    public void close() throws IOException {
        engine1Writer.println("quit");
        engine1Writer.close();
        engine1Reader.close();
        engine1.destroy();
        engine2Writer.println("quit");
        engine2Writer.close();
        engine2Reader.close();
        engine2.destroy();
    }

    private boolean is50Move() throws IOException {
        engine1Writer.println("50move");
        String response = engine1Reader.readLine();
        //Output.println("is 50 : ");
        //Output.println(response);
        return (Integer.parseInt(response) >= 100);
    }

    private boolean isRepetition() throws IOException {
        engine1Writer.println("key");
        String response = engine1Reader.readLine();
        keyHolder.add(response);
        int count = 0;
        for(int i = keyHolder.size()-1;i>=0 && count < 4;i-=2){
            if(Objects.equals(keyHolder.get(i), response)) count++;
        }
        //Output.println("is rep : ");
        //Output.println(response);
        return (count>=4);
    }
    private int isMate() throws IOException {
        engine1Writer.println("moves count");
        String response = engine1Reader.readLine();
        //Output.println("is mate : ");
        //Output.println(response);
        if(Integer.parseInt(response) != 0) return 0;
        engine1Writer.println("checkers");
        response = engine1Reader.readLine();
        if(Objects.equals(response, "0")) return 1; // stalemate
        return 2; // checkmate
    }

    private String readSearchResponse(BufferedReader reader) throws IOException {
        StringBuilder response = new StringBuilder();
        String line;
        while(!response.toString().contains("bestmove") && !response.toString().contains("gameover")){
            line = reader.readLine();
            if(line == null) {
                reader.close();
                close();
                throw new IOException("process is null");
            }
            response.append(line);
            response.append(" \n");
        }
        return response.toString();
    }

    private int numFromString(String str, int startIndex) {
        int endIndex = str.indexOf(' ', startIndex);
        if (endIndex == -1) {
            endIndex = str.length();
        }
        String numberStr = str.substring(startIndex, endIndex);
        return Integer.parseInt(numberStr.trim());
    }
    private double doubleFromString(String str, int startIndex) {
        int endIndex = str.indexOf(' ', startIndex);
        if (endIndex == -1) {
            endIndex = str.length();
        }
        String numberStr = str.substring(startIndex, endIndex);
        return Double.parseDouble(numberStr.trim());
    }

    public void runMatch(String fen, int matchNum, String whiteName, String blackName, String whitePolicy, String blackPolicy, String type, double typeParameter) throws IOException {
        String goCommand = "go "+type+" "+typeParameter;
        Output.println("<----------------------------------------------------------------------->\ngame "+matchNum+" started\n");
        BufferedWriter fileWriter = new BufferedWriter(new FileWriter(directoryPath +"/match"+matchNum+".txt"));
        fileWriter.write(whiteName + " "+ blackName + " " + type +" "+ typeParameter + "\n");
        engine1Writer.println("reset");
        engine2Writer.println("reset");
        engine1Writer.println("tt policy "+ whitePolicy);
        engine2Writer.println("tt policy "+ blackPolicy);
        engine1Writer.println("position fen "+fen);
        engine2Writer.println("position fen "+fen);
        if(!engine1Reader.readLine().equals("fen ok") || !engine2Reader.readLine().equals("fen ok")){
            Output.println("fen wrong\n");
            return;
        }

        long[] nodes = {0,0};
        long[] hit = {0,0};
        double[] time = {0,0};
        int[] depth = {0,0};
        int[] movecount = {0,0};

        long currentNodes =0;
        long currentHit =0;
        double currentTime =0;
        int currentDepth =0;

        boolean[] TTOn = {true,true};

        PrintWriter activeWriter;
        BufferedReader activeReader;
        boolean isWhiteTurn = (fen.contains(" w "));
        fileWriter.write(fen + "\n");
        while(true){
            if(isRepetition()) {
                Output.println("gameover draw by repetition");
                fileWriter.write("gameover draw by repetition\n");
                break;
            }
            if(is50Move()) {
                Output.println("gameover draw by 50-move rule");
                fileWriter.write("gameover draw by 50-move rule\n");
                break;
            }
            int isMate = isMate();
            if(isMate == 1){
                Output.println("gameover draw by stalemate");
                fileWriter.write("gameover draw by stalemate\n");
                break;
            }
            else if(isMate == 2){
                String won = isWhiteTurn? blackName : whiteName;
                Output.println("gameover "+won+" won by checkmate");
                fileWriter.write("gameover "+won+" won by checkmate\n");
                break;
            }
            if(isWhiteTurn){
                activeWriter = engine1Writer;
                activeReader = engine1Reader;
            }
            else{
                activeWriter = engine2Writer;
                activeReader = engine2Reader;
            }
            activeWriter.println(goCommand);
            String response;
            try{
                response = readSearchResponse(activeReader);
            } catch (IOException e){
                fileWriter.close();
                throw e;
            }
            //Output.println(response);
            if(response.contains("gameover")) {
                Output.println("gameover unexpected engine response");
                fileWriter.write("gameover unexpected engine response\n");
                fileWriter.write(response);
                break;
            }
            int index = response.indexOf("nodes : ");
            currentNodes = numFromString(response, index+8);
            index = response.indexOf("ttHit : ");
            currentHit = numFromString(response, index+8);
            index = response.indexOf("time : ");
            currentTime = doubleFromString(response, index+7);
            index = response.indexOf("depth : ");
            currentDepth = numFromString(response, index+8);
            if(isWhiteTurn && TTOn[0]){
                nodes[0] += currentNodes;
                hit[0] += currentHit;
                time[0] += currentTime;
                depth[0] += currentDepth;
                movecount[0]++;
            }
            else if(!isWhiteTurn && TTOn[1]){
                nodes[1] += currentNodes;
                hit[1] += currentHit;
                time[1] += currentTime;
                depth[1] += currentDepth;
                movecount[1]++;
            }
            index = response.indexOf("bestmove ");
            String move = response.substring(index+9).trim();
            fileWriter.write(move +" "+ currentTime +" "+ currentDepth +" "+ currentNodes +" "+ currentHit +"\n");
            Output.println("move "+move);
            engine1Writer.println("move "+move);
            response = engine1Reader.readLine();
            if(!response.equals("move ok")){
                //Output.println("white "+response);
                if(response.equals("Switched to non-TT mode!")) {
                    TTOn[0] = false;
                    fileWriter.write("TT off "+whiteName+"\n");
                }
                else if(response.equals("Switched to TT mode!")) {
                    TTOn[0] = true;
                    fileWriter.write("TT on "+whiteName+"\n");
                }
                else{
                    Output.println("wrong move command\n");
                    return;
                }
                response = engine1Reader.readLine();
                if(!response.equals("move ok")){
                    Output.println("wrong move command\n");
                    return;
                }
            }
            engine2Writer.println("move "+move);
            response = engine2Reader.readLine();
            if(!response.equals("move ok")){
                //Output.println("black "+response);
                if(response.equals("Switched to non-TT mode!")) {
                    TTOn[1] = false;
                    fileWriter.write("TT off "+blackName+"\n");
                }
                else if(response.equals("Switched to TT mode!")) {
                    TTOn[1] = true;
                    fileWriter.write("TT on "+blackName+"\n");
                }
                else{
                    Output.println("wrong move command\n");
                    return;
                }
                response = engine2Reader.readLine();
                if(!response.equals("move ok")){
                    Output.println("wrong move command\n");
                    return;
                }
            }
            isWhiteTurn = !isWhiteTurn;
        }
        fileWriter.write(whiteName +" movecount "+ String.valueOf(movecount[0])+ "\n");
        fileWriter.write(whiteName +" nodes "+ String.valueOf(nodes[0])+ "\n");
        fileWriter.write(whiteName +" hit "+ String.valueOf(hit[0])+ "\n");
        fileWriter.write(whiteName +" time per move "+ String.valueOf(time[0]/movecount[0])+ "\n");
        fileWriter.write(whiteName +" depth per move "+ String.valueOf((double) depth[0]/movecount[0])+ "\n");
        engine1Writer.println("tt stats");
        fileWriter.write(whiteName + " "+engine1Reader.readLine()+"\n");

        fileWriter.write(blackName +" movecount "+ String.valueOf(movecount[1])+ "\n");
        fileWriter.write(blackName +" nodes "+ String.valueOf(nodes[1])+ "\n");
        fileWriter.write(blackName +" hit "+ String.valueOf(hit[1])+ "\n");
        fileWriter.write(blackName +" time per move "+ String.valueOf(time[1]/movecount[1])+ "\n");
        fileWriter.write(blackName +" depth per move "+ String.valueOf((double) depth[1]/movecount[1])+ "\n");
        engine2Writer.println("tt stats");
        fileWriter.write(blackName + " "+engine2Reader.readLine()+"\n");
        fileWriter.close();
    }

}
