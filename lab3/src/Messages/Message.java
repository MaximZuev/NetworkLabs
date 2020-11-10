package Messages;

import java.net.InetSocketAddress;

public interface Message {
    String getGUID();
    InetSocketAddress getReceiver();
    byte[] bytes();
}
