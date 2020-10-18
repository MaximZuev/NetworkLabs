import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        File file = new File(args[0]);
        String host = args[1];
        int port = Integer.parseInt(args[2]);

        try (Client client = new Client(file, host, port)) {
            client.start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
