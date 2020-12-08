package Messages;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class TextMessage extends ConfirmMessage {
    private final String text;

    public TextMessage(String text, InetSocketAddress receiver) {
        super(receiver);
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public byte[] bytes() {
        String message = "TEXT_" + getGUID() + "_" + text;
        return message.getBytes(StandardCharsets.UTF_8);
    }
}