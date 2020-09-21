import java.io.IOException;
import java.net.*;
import java.util.HashMap;

public class UDPMulticastClient {
    private final int TTL = 5000;
    private final int RECEIVE_TIMEOUT = 1000;

    private final MulticastSocket receiver;
    private final DatagramSocket publisher;

    private final InetAddress groupIP;
    private final int groupPort;

    private final HashMap<String, Long> clientsMap = new HashMap<>();

    public UDPMulticastClient(int groupPort, String groupIP) throws IOException {
        publisher = new DatagramSocket();
        receiver = new MulticastSocket(groupPort);
        this.groupIP = InetAddress.getByName(groupIP);
        this.groupPort = groupPort;

        receiver.joinGroup(this.groupIP);
        receiver.setSoTimeout(100);
    }


    public void start() throws IOException {
        while (true) {
            byte[] sendBuf = new byte[256];
            DatagramPacket packetToSend = new DatagramPacket(sendBuf, sendBuf.length, groupIP, groupPort);
            publisher.send(packetToSend);

            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < RECEIVE_TIMEOUT) {
                byte[] recvBuf = new byte[256];
                DatagramPacket receivedPacket = new DatagramPacket(recvBuf, recvBuf.length);
                try {
                    receiver.receive(receivedPacket);
                } catch (SocketTimeoutException e) {
                    continue;
                }
                updateMap(receivedPacket);
            }

            if (checkMap()) {
                printMap();
            }
        }
    }

    private void updateMap(DatagramPacket receivedPacket) {
        if (receivedPacket != null) {
            String key = receivedPacket.getSocketAddress().toString();
            if (clientsMap.containsKey(key)) {
                clientsMap.put(key, System.currentTimeMillis());
            } else {
                clientsMap.put(key, System.currentTimeMillis());
                printMap();
            }
        }
    }

    private boolean checkMap() {
        long time = System.currentTimeMillis();
        return clientsMap.entrySet().removeIf(entry -> (time - entry.getValue()) > TTL);
    }

    private void printMap() {
        System.out.println("____________________________");
        System.out.println("Number of copies: " + clientsMap.size());
        for (HashMap.Entry<String, Long> entry : clientsMap.entrySet()) {
            System.out.println(entry.getKey());
        }
        System.out.println("____________________________");
    }

    public void stop() {
        publisher.close();
        receiver.close();
    }
}
