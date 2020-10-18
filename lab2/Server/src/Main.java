import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        try (Server server = new Server(port)){
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
