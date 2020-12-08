import java.io.IOException;
import java.net.*;

public class Main {

    public static void main(String[] args) {
        if ((args.length == 3) || (args.length == 5)) {
            try {
                InetSocketAddress neighbor = null;
                int port = Integer.parseInt(args[1]);
                int losses = Integer.parseInt(args[2]);
                if ((losses < 0) || (losses >= 100)) {
                    throw new IllegalArgumentException();
                }
                if (args.length == 5) {
                    neighbor = new InetSocketAddress(InetAddress.getByName(args[3]), Integer.parseInt(args[4]));
                }
                try (ChatNode chatNode = new ChatNode(args[0], port, losses, neighbor)) {
                    chatNode.start();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            } catch (UnknownHostException | IllegalArgumentException e) {
                e.printStackTrace();
                System.exit(1);
            }
        } else {
            System.err.println("Invalid number of parameters.");
            System.exit(1);
        }
    }
}