import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server implements Closeable {
    private final ServerSocket serverSocket;
    private final ArrayList<Socket> clientsSockets;
    private final ArrayList<Thread> clientsThreads;

    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        clientsSockets = new ArrayList<>();
        clientsThreads = new ArrayList<>();
    }

    public void start() throws IOException {
        while (!serverSocket.isClosed()) {
            Socket clientSocket = serverSocket.accept();
            Thread clientThread = new Thread(new ClientHandler(clientSocket));

            clientsSockets.add(clientSocket);
            clientsThreads.add(clientThread);
            
            clientThread.start();
        }
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
        for (Thread clientThread: clientsThreads) {
            clientThread.interrupt();
        }
        for (Socket clientSocket: clientsSockets) {
            clientSocket.close();
        }
    }
}
