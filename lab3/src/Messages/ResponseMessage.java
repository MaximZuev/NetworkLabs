package Messages;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class ResponseMessage implements Message {
    private final String GUID;
    private final InetSocketAddress receiver;

    public ResponseMessage(String GUID, InetSocketAddress receiver) {
        this.GUID = GUID;
        this.receiver = receiver;
    }

    @Override
    public String getGUID() {
        return GUID;
    }

    @Override
    public InetSocketAddress getReceiver() {
        return receiver;
    }

    @Override
    public byte[] bytes() {
        String message = "RESPONSE_" + getGUID();
        return message.getBytes(StandardCharsets.UTF_8);
    }
}
