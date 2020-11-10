import Messages.ConfirmMessage;
import Messages.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.List;
import java.util.Map;

public class MessageSender implements Runnable {
    private final DatagramSocket datagramSocket;

    private final List<Message> messagesToSend;
    private final Map<String, ConfirmMessage> messagesToConfirm;

    public MessageSender(DatagramSocket datagramSocket, List<Message> messagesToSend,
                         Map<String, ConfirmMessage> messagesToConfirm) {
        this.datagramSocket = datagramSocket;
        this.messagesToSend = messagesToSend;
        this.messagesToConfirm = messagesToConfirm;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                if (messagesToSend.size() > 0) {
                    Message message = messagesToSend.remove(0);
                    byte[] bytes = message.bytes();
                    datagramSocket.send(new DatagramPacket(bytes, bytes.length, message.getReceiver()));

                    if (message instanceof ConfirmMessage) {
                        ((ConfirmMessage) message).send();
                        messagesToConfirm.put(message.getGUID(), (ConfirmMessage) message);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
