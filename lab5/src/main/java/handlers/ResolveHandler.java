package handlers;

import javafx.util.Pair;
import org.xbill.DNS.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.*;

import static handlers.Config.*;

public class ResolveHandler extends Handler {
    private final DatagramChannel channel;
    private final InetSocketAddress dnsAddress;
    private final SelectionKey resolverKey;
    private final Deque<Pair<String, ClientHandler>> resolves;
    private final Map<Integer, Pair<String, ClientHandler>> sentResolves;
    private final ByteBuffer buffer;
    private int index = 0;

    public ResolveHandler(SelectionKey resolverKey) {
        channel = (DatagramChannel) resolverKey.channel();
        dnsAddress = ResolverConfig.getCurrentConfig().servers().get(0);
        this.resolverKey = resolverKey;
        this.resolverKey.interestOps(SelectionKey.OP_READ);
        resolves = new ArrayDeque<>();
        sentResolves = new HashMap<>();
        buffer = ByteBuffer.allocate(BUF_SIZE);
    }

    @Override
    public void handle(SelectionKey selectionKey) throws IOException {
        ResolveHandler resolveHandler = (ResolveHandler) resolverKey.attachment();
        if (resolverKey.isReadable()) {
            resolveHandler.recvRequest();
        } else if (resolverKey.isWritable()) {
            resolveHandler.sendRequest();
        }
    }

    public void addRequest(String address, ClientHandler clientHandler) {
        resolves.add(new Pair<>(address, clientHandler));
        resolverKey.interestOps(resolverKey.interestOps() | SelectionKey.OP_WRITE);
    }

    public void sendRequest() throws IOException {
        if (resolves.isEmpty()) {
            resolverKey.interestOps(resolverKey.interestOps() & ~SelectionKey.OP_WRITE);
            return;
        }
        Pair<String, ClientHandler> resolve = resolves.pop();
        sentResolves.put(++index, resolve);
        Message message = new Message();
        Header header = message.getHeader();
        header.setOpcode(Opcode.QUERY);
        header.setID(index);
        header.setFlag(Flags.RD);
        message.addRecord(Record.newRecord(new Name(resolve.getKey() + "."), Type.A, DClass.IN), Section.QUESTION);
        ByteBuffer newBuffer = ByteBuffer.wrap(message.toWire());
        channel.send(newBuffer, dnsAddress);
    }

    public void recvRequest() throws IOException {
        buffer.clear();
        channel.receive(buffer);
        buffer.flip();
        Message message = new Message(buffer.array());
        int requestId = message.getHeader().getID();
        if ((!sentResolves.containsKey(requestId)) && (message.getRcode() != Rcode.NOERROR)) {
            return;
        }
        List<Record> records = message.getSection(Section.ANSWER);
        ARecord aRecord = null;
        for (Record record : records) {
            if (record.getType() == Type.A) {
                aRecord = (ARecord) record;
                break;
            }
        }
        Pair<String, ClientHandler> resolve = sentResolves.get(requestId);
        ClientHandler clientHandler = resolve.getValue();
        if (aRecord != null) {
            InetAddress address = aRecord.getAddress();
            clientHandler.connectToHost(address, clientHandler.getPortToConnect());
        } else {
            clientHandler.makeResponse(HOST_UNREACHABLE);
        }
        sentResolves.remove(requestId);
    }
    
    @Override
    public void close() throws IOException {
        if (channel != null) {
            channel.close();
        }
    }
}
