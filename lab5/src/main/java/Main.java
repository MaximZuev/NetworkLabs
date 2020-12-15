import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            if (args.length != 1) {
                throw new IllegalArgumentException();
            }
            int port = Integer.parseInt(args[0]);
            try (ProxyServer proxyServer = new ProxyServer(port)) {
                proxyServer.run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Wrong arguments");
        }
    }
}
