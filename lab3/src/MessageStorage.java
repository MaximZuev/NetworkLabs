import Messages.TextMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageStorage {
    //private final BlockingQueue<Messages.Message> messagesToSend;
    //private final BlockingQueue<Messages.Message> messagesToConfirm;
    private final Map<String, TextMessage> map;

    public MessageStorage() {
        //messagesToSend = new LinkedBlockingDeque<>();
        //messagesToConfirm = new LinkedBlockingDeque<>();
        map = new ConcurrentHashMap<>();
    }

    public void put(TextMessage textMessage) {
        //messagesToSend.add(new Messages.Message(message, sender));
        map.put(textMessage.getGUID(), textMessage);
    }

    public TextMessage get(String GUID) {
        return map.get(GUID);
    }
}
