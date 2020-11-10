package Messages;

import java.net.InetSocketAddress;

public abstract class ConfirmMessage implements Message {
    private final static int MAX_SEND_COUNT = 50;
    private final static long MAX_TIME_RESEND = 100;
    private int sendCount;
    private long timeStartSend;
    private final String GUID;
    private final InetSocketAddress receiver;

    public ConfirmMessage(InetSocketAddress receiver) {
        this.receiver = receiver;
        GUID = java.util.UUID.randomUUID().toString();
    }

    public void send() {
        timeStartSend = System.currentTimeMillis();
        ++sendCount;
    }

    public int isTimeToResend() {
        if ((System.currentTimeMillis() - timeStartSend) > MAX_TIME_RESEND) {
            if (sendCount < MAX_SEND_COUNT) {
                send();
                return 1;
            } else {
                return -1;
            }
        } else {
            return 0;
        }
    }

    @Override
    public String getGUID() {
        return GUID;
    }

    @Override
    public InetSocketAddress getReceiver() {
        return receiver;
    }
}
