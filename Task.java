import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;

public class Task implements Runnable{
    private String firstName;
    private String secondName;
    private String firstPolicy;
    private String secondPolicy;
    private String directory;
    private PrintStream Output;
    private int depthBound;
    private int timeBound;
    private int fenLowerBound;
    private int fenUpperBound;
    private boolean redirect;
    private CountDownLatch latch;

    public Task(String firstName, String secondName, String firstPolicy, String secondPolicy, String directory, int depthBound, int timeBound, int fenLowerBound, int fenUpperBound, boolean redirect, CountDownLatch latch){
        this.directory = directory;
        this.firstName = firstName;
        this.secondName = secondName;
        this.firstPolicy = firstPolicy;
        this.secondPolicy = secondPolicy;
        this.depthBound = depthBound;
        this.timeBound = timeBound;
        this.fenLowerBound = fenLowerBound;
        this.fenUpperBound = fenUpperBound;
        this.redirect = redirect;
        this.latch = latch;
    }
    @Override
    public void run() {
        try {
            File dir = new File(directory);
            if (!dir.exists()) {
                if (dir.mkdirs()) {
                    System.out.println("Directory "+directory+" created successfully");
                } else {
                    throw new Exception("Failed to create directory "+directory);
                }
            }

            if(redirect){
                String out = directory+"/output.txt";
                File file = new File(out);
                if (!file.exists()) {
                    if (file.createNewFile()) {
                        System.out.println("Directory "+out+" created successfully");
                    } else {
                        throw new Exception("Failed to create directory "+out);
                    }
                }
                Output = new PrintStream(new FileOutputStream(out, true));
            }
            else{
                Output = System.out;
            }

            MatchManager manager = new MatchManager(directory, firstName, secondName, firstPolicy, secondPolicy, Output, depthBound, timeBound, fenLowerBound, fenUpperBound);
            manager.continueMatchUp();
            System.out.println(firstName + " " + secondName + " match finished successfully");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            latch.countDown();
        }
    }
}
