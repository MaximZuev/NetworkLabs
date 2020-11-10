import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        UDPMulticastClient client = null;
        try {
            client = new UDPMulticastClient(2222, "224.0.0.69");
            client.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (client != null) {
                client.stop();
            }
        }
    }
}
