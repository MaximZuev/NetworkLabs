package Messages;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class DeputyMessage extends ConfirmMessage {
    private final String hostname;
    private final int port;

    public DeputyMessage(String hostname, int port, InetSocketAddress receiver) {
        super(receiver);
        this.hostname = hostname;
        this.port = port;
    }

    @Override
    public byte[] bytes() {
        String message = "DEPUTY_" + getGUID() + "_" + hostname + ":" + port;
        return message.getBytes(StandardCharsets.UTF_8);
    }
}
