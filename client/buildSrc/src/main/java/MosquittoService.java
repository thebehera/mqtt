import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;


class MosquittoService implements Runnable {
    private final File configFile;
    private Process runningProcess;

    public MosquittoService(File configFile) {
        this.configFile = configFile;
    }

    @Override
    public void run() {
        ProcessBuilder pb = new ProcessBuilder()
                .command("mosquitto", "-c", configFile.getAbsolutePath(), "-v");
        System.out.println(pb.directory());
        System.out.println(pb.command());
        pb.redirectErrorStream(true);
        try {
            runningProcess = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(runningProcess.getInputStream()));
            String readline;
            int i = 0;
            while ((readline = reader.readLine()) != null) {
                System.out.println(++i + " " + readline);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}