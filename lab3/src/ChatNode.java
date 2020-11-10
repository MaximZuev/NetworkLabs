import Messages.ConfirmMessage;
import Messages.Message;
import Messages.TextMessage;

import java.io.Closeable;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;

public class ChatNode implements Closeable {
    private final String name;

    private final DatagramSocket datagramSocket;

    private final List<InetSocketAddress> neighbors;

    private final Thread messageSender;
    private final Thread messageReceiver;
    private final Thread messageResender;

    private final List<Message> messagesToSend;

    public ChatNode(String name, int myPort, int losses, InetSocketAddress neighbor) throws SocketException {
        this.name = name;

        datagramSocket = new DatagramSocket(myPort);
        neighbors = new CopyOnWriteArrayList<>();
        messagesToSend = new CopyOnWriteArrayList<>();

        Map<String, ConfirmMessage> messagesToConfirm = new ConcurrentHashMap<>();
        Map<InetSocketAddress, InetSocketAddress> deputies = new ConcurrentHashMap<>();

        if (neighbor != null) {
            neighbors.add(neighbor);
        }

        messageSender = new Thread(new MessageSender(datagramSocket, messagesToSend, messagesToConfirm));
        messageResender = new Thread(new MessageResender(datagramSocket, messagesToSend, messagesToConfirm,
                neighbors, deputies, neighbor));
        messageReceiver = new Thread(new MessageReceiver(losses, datagramSocket, messagesToSend, messagesToConfirm,
                neighbors, deputies, neighbor));
    }

    public void start() {
        messageSender.start();
        messageResender.start();
        messageReceiver.start();
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        String string;
        while (!(string = scanner.nextLine()).equals("")) {
            for (InetSocketAddress neighbor : neighbors) {
                TextMessage textMessage = new TextMessage("[" + name + "]: " + string, neighbor);
                messagesToSend.add(textMessage);
            }
        }
        scanner.close();
    }

    @Override
    public void close() {
        messageReceiver.interrupt();
        messageSender.interrupt();
        messageResender.interrupt();
        datagramSocket.close();
        try {
            messageReceiver.join();
            messageSender.join();
            messageResender.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
