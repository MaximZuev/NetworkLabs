import Messages.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MessageReceiver implements Runnable {
    private final int losses;

    private final DatagramSocket datagramSocket;

    private InetSocketAddress deputy;

    private final List<InetSocketAddress> neighbors;
    private final Map<InetSocketAddress, InetSocketAddress> deputies;

    private final List<Message> messagesToSend;
    private final Map<String, ConfirmMessage> messagesToConfirm;

    private final ArrayList<String> receivedMessage;
    private final DatagramPacket datagramPacket = new DatagramPacket(new byte[2048], 2048);

    public MessageReceiver(int losses, DatagramSocket socket, List<Message> messagesToSend,
                           Map<String, ConfirmMessage> messagesToConfirm, List<InetSocketAddress> neighbors,
                           Map<InetSocketAddress, InetSocketAddress> deputies, InetSocketAddress deputy) {
        this.losses = losses;
        this.datagramSocket = socket;
        this.messagesToSend = messagesToSend;
        this.messagesToConfirm = messagesToConfirm;
        this.neighbors = neighbors;
        this.deputies = deputies;
        this.deputy = deputy;
        receivedMessage = new ArrayList<>();
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                datagramSocket.receive(datagramPacket);
                if (!isLost()) {
                    String string = new String(datagramPacket.getData(), datagramPacket.getOffset(),
                            datagramPacket.getLength(), StandardCharsets.UTF_8);
                    String[] strings = string.split("_", 3);
                    String type = strings[0];
                    String GUID = strings[1];
                    InetSocketAddress sender = new InetSocketAddress(datagramPacket.getAddress(), datagramPacket.getPort());
                    switch (type) {
                        case "TEXT":
                            String text = strings[2];
                            if (!receivedMessage.contains(GUID)) {
                                System.out.println(text);
                                addNeighbor(sender);
                                addTextMessage(text, sender);
                                addReceivedMessage(GUID);
                            }
                            addResponseMessage(GUID, sender);
                            break;
                        case "RESPONSE":
                            messagesToConfirm.remove(GUID);
                            break;
                        case "DEPUTY":
                            if (!receivedMessage.contains(GUID)) {
                                String[] data = strings[2].split(":");
                                String hostName = data[0];
                                int port = Integer.parseInt(data[1]);
                                InetSocketAddress senderDeputy = new InetSocketAddress(hostName, port);
                                deputies.put(sender, senderDeputy);
                                addReceivedMessage(GUID);
                                System.out.println("deputy" +sender+ "-" +hostName + port);
                            }
                            addResponseMessage(GUID, sender);
                            break;
                    }
                }
            }
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    private boolean isLost() {
        return ((int) (Math.random() * 100) < losses);
    }

    private void addNeighbor(InetSocketAddress newNeighbor) {
        if (!neighbors.contains(newNeighbor)) {
            neighbors.add(newNeighbor);
            if (deputy != null ) {
                DeputyMessage deputyMessage = new DeputyMessage(deputy.getHostString(), deputy.getPort(), newNeighbor);
                messagesToSend.add(deputyMessage);
            } else {
                deputy = newNeighbor;
                for (InetSocketAddress neighbor : neighbors) {
                    if (neighbor != newNeighbor) {
                        DeputyMessage deputyMessage = new DeputyMessage(deputy.getHostString(), deputy.getPort(), neighbor);
                        messagesToSend.add(deputyMessage);
                    }

                }
            }
        }
    }

    private void addTextMessage(String text, InetSocketAddress sender) {
        for (InetSocketAddress neighbor: neighbors) {
            if (!sender.toString().equals(neighbor.toString())) {
                TextMessage textMessage = new TextMessage(text, neighbor);
                messagesToSend.add(textMessage);
            }
        }
    }

    private void addResponseMessage(String GUID, InetSocketAddress sender) {
        ResponseMessage ResponseMessage = new ResponseMessage(GUID, sender);
        messagesToSend.add(ResponseMessage);
    }

    private void addReceivedMessage(String GUID) {
        receivedMessage.add(GUID);
        if (receivedMessage.size() > 100) {
            receivedMessage.remove(0);
        }
    }

}
