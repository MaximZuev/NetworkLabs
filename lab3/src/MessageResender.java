import Messages.ConfirmMessage;
import Messages.DeputyMessage;
import Messages.Message;
import Messages.TextMessage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

public class MessageResender implements Runnable {
    private final DatagramSocket datagramSocket;

    private final List<Message> messagesToSend;
    private final Map<String, ConfirmMessage> messagesToConfirm;

    private InetSocketAddress deputy;

    private final List<InetSocketAddress> neighbors;
    private final Map<InetSocketAddress, InetSocketAddress> deputies;


    public MessageResender(DatagramSocket datagramSocket, List<Message> messagesToSend,
                           Map<String, ConfirmMessage> messagesToConfirm, List<InetSocketAddress> neighbors,
                           Map<InetSocketAddress, InetSocketAddress> deputies, InetSocketAddress deputy) {
        this.datagramSocket = datagramSocket;
        this.messagesToSend = messagesToSend;
        this.messagesToConfirm = messagesToConfirm;
        this.neighbors = neighbors;
        this.deputies = deputies;
        this.deputy = deputy;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                for (String GUID : messagesToConfirm.keySet()) {
                    ConfirmMessage confirmMessage = messagesToConfirm.getOrDefault(GUID, null);
                    if (confirmMessage != null) {
                        switch (confirmMessage.isTimeToResend()) {
                            case 1:
                                byte[] bytes = confirmMessage.bytes();
                                datagramSocket.send(new DatagramPacket(bytes, bytes.length, confirmMessage.getReceiver()));
                                break;
                            case -1:
                                InetSocketAddress receiver = confirmMessage.getReceiver();
                                messagesToConfirm.values().removeIf(message -> (message.getReceiver() == receiver));
                                neighbors.remove(receiver);
                                InetSocketAddress deputyReceiver = deputies.remove(receiver);
                                if (deputyReceiver != null) {
                                    neighbors.add(deputyReceiver);
                                }
                                redirectTextMessage(receiver, deputyReceiver);
                                if (deputy == receiver) {
                                    updateDeputy();
                                }
                                break;
                            case 0:
                                break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void redirectTextMessage(InetSocketAddress receiver, InetSocketAddress deputyReceiver) {
        for (Message message : messagesToSend) {
            if ((message instanceof TextMessage) && (message.getReceiver() == receiver)) {
                TextMessage textMessage = new TextMessage(((TextMessage) message).getText(), deputyReceiver);
                messagesToSend.add(textMessage);
            }
        }
    }

    private void updateDeputy() {
        deputy = null;
        if (!neighbors.isEmpty()) {
            deputy = neighbors.get(0);
            for (InetSocketAddress neighbor : neighbors) {
                if (neighbor != deputy) {
                    DeputyMessage deputyMessage = new DeputyMessage(deputy.getHostString(),
                            deputy.getPort(), neighbor);
                    messagesToSend.add(deputyMessage);
                }
            }
        }
    }
}