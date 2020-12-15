package handlers;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import static handlers.ConnectState.*;
import static handlers.Config.*;

public class ClientHandler extends Handler {
    private final SelectionKey clientKey;
    private final SocketChannel channel;
    private final ResolveHandler resolveHandler;
    private final ByteBuffer buffer;
    private SelectionKey serverKey;
    private ConnectState state;
    private int portToConnect;
    private boolean hasNoData = false;

    public ClientHandler(SelectionKey clientKey, ResolveHandler resolveHandler) {
        this.resolveHandler = resolveHandler;
        this.clientKey = clientKey;
        state = WAIT_AUTH;
        this.channel = (SocketChannel) clientKey.channel();
        buffer = ByteBuffer.allocate(BUF_SIZE);
    }

    @Override
    public void handle(SelectionKey selectionKey) throws IOException {
        ClientHandler clientHandler = (ClientHandler) clientKey.attachment();
        ConnectState state = clientHandler.getState();
        if (state == ConnectState.WAIT_AUTH || state == ConnectState.WAIT_REQ) {
            channel.read(buffer);
        } else if (state == ConnectState.SEND_AUTH || state == ConnectState.SEND_RESP
                || state == ConnectState.SEND_ERR) {
            channel.write(buffer);
        } else if (state == ConnectState.FORWARDING && clientKey.isReadable()) {
            clientHandler.read();
        } else if (state == ConnectState.FORWARDING && clientKey.isWritable()) {
            clientHandler.write();
        }
        clientHandler.nextState();
    }

    public void nextState() throws IOException {
        if (state == WAIT_AUTH) {
            makeAuthResponse();
        } else if (state == WAIT_REQ) {
            handleRequest();
        } else if (state == SEND_AUTH && !buffer.hasRemaining()) {
            state = WAIT_REQ;
            buffer.clear();
        } else if (state == SEND_RESP && !buffer.hasRemaining()) {
            state = FORWARDING;
            clientKey.interestOps(SelectionKey.OP_READ);
        } else if (state == SEND_ERR && !buffer.hasRemaining()) {
            close();
            buffer.clear();
        }
    }

    public void write() throws IOException {
        ServerHandler serverHandler = (ServerHandler) serverKey.attachment();
        channel.write(serverHandler.getBuffer());
        if (!serverHandler.getBuffer().hasRemaining()) {
            clientKey.interestOps(clientKey.interestOps() & ~SelectionKey.OP_WRITE);
            serverKey.interestOps(serverKey.interestOps() | SelectionKey.OP_READ);
        }
    }

    public void read() throws IOException {
        buffer.clear();
        if (channel.read(buffer) == -1) {
            hasNoData = true;
            clientKey.interestOps(clientKey.interestOps() & ~SelectionKey.OP_READ);
            ServerHandler serverHandler = (ServerHandler) serverKey.attachment();
            serverHandler.shutDownOutput();
            if (serverHandler.isHasNoData()) {
                close();
            }
            return;
        }
        buffer.flip();
        clientKey.interestOps(clientKey.interestOps() & ~SelectionKey.OP_READ);
        serverKey.interestOps(serverKey.interestOps() | SelectionKey.OP_WRITE);
    }

    public void connectToHost(InetAddress address, int port) throws IOException {
        SocketChannel serverChannel = SocketChannel.open();
        serverChannel.configureBlocking(false);
        serverKey = serverChannel.register(clientKey.selector(), SelectionKey.OP_CONNECT);
        serverKey.attach(new ServerHandler(serverKey, clientKey));
        serverChannel.connect(new InetSocketAddress(address, port));
    }

    public void makeResponse(byte error) {
        buffer.clear();
        buffer.put(VERSION);
        buffer.put(error);
        buffer.put(RSV);
        buffer.put(IPV4);
        for (int i = 0; i < 6; i++) {
            buffer.put(RSV);
        }
        buffer.flip();
        state = (error == REQUEST_GRANTED) ? SEND_RESP : SEND_ERR;
        clientKey.interestOps(SelectionKey.OP_WRITE);
    }

    public void handleRequest() throws IOException {
        int bufferSize = buffer.position();
        if (bufferSize < 4) {
            return;
        }
        byte command = buffer.get(1);
        if (command != TCP_IP_CONNECT) {
            makeResponse(CMD_NOT_SUPPORTED);
            return;
        }
        byte addressType = buffer.get(3);
        if (addressType == IPV4) {
            if (bufferSize < 10) {
                return;
            }
            byte[] address = new byte[4];
            buffer.position(4);
            buffer.get(address);
            int port = buffer.getShort(8);
            InetAddress inetAddress = InetAddress.getByAddress(address);
            connectToHost(inetAddress, port);
            clientKey.interestOps(0);
            state = FORWARDING;
        } else if (addressType == DOMAIN_NAME) {
            int addressLength = buffer.get(4);
            if (bufferSize < 6 + addressLength) {
                return;
            }
            byte[] address = new byte[addressLength];
            buffer.position(5);
            buffer.get(address, 0, addressLength);
            String addressStr = new String(address);
            clientKey.interestOps(0);
            state = FORWARDING;
            resolveHandler.addRequest(addressStr, this);
            portToConnect = buffer.getShort(5 + addressLength);
        } else {
            makeResponse(ADDRESS_TYPE_NOT_SUPPORTED);
        }
    }

    public void makeAuthResponse() {
        int methodsNumber = buffer.get(1);
        int method = AUTH_NOT_FOUND;
        for (int i = 0; i < methodsNumber; i++) {
            byte currentMethod = buffer.get(i + 2);
            if (currentMethod == NO_AUTH) {
                method = currentMethod;
                break;
            }
        }
        buffer.clear();
        buffer.put(VERSION);

        if (method == AUTH_NOT_FOUND) {
            buffer.put((byte) (method >> 8));
            buffer.put((byte) method);
            state = SEND_ERR;
        } else {
            buffer.put((byte) method);
            state = SEND_AUTH;
        }

        buffer.flip();
        clientKey.interestOps(SelectionKey.OP_WRITE);
    }

    public ConnectState getState() {
        return state;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public boolean isHasNoData() {
        return hasNoData;
    }

    public void shutDownOutput() throws IOException {
        channel.shutdownOutput();
    }

    public int getPortToConnect() {
        return portToConnect;
    }

    @Override
    public void close() throws IOException {
        if (channel != null) {
            channel.close();
        }
        if (serverKey != null) {
            serverKey.channel().close();
        }
    }
}