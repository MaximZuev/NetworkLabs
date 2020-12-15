package handlers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;


import static handlers.ConnectState.*;
import static handlers.Config.*;

public class ServerHandler extends Handler {
    private final SelectionKey serverKey;
    private final SelectionKey clientKey;
    private final SocketChannel channel;
    private ConnectState state;
    private boolean hasNoData = false;
    private final ByteBuffer buffer = ByteBuffer.allocate(BUF_SIZE);

    public ServerHandler(SelectionKey serverKey, SelectionKey clientKey) {
        this.clientKey = clientKey;
        this.serverKey = serverKey;
        channel = (SocketChannel) serverKey.channel();
        state = CONNECTION;
    }

    @Override
    public void handle(SelectionKey selectionKey) throws IOException {
        ServerHandler serverHandler = (ServerHandler) serverKey.attachment();
        if (serverHandler.getState() == ConnectState.FORWARDING) {
            if (serverKey.isReadable()) {
                serverHandler.read();
            } else if (serverKey.isWritable()) {
                serverHandler.write();
            }
        }
        serverHandler.nextState();
    }

    public void write() throws IOException {
        ClientHandler clientHandler = (ClientHandler) clientKey.attachment();
        channel.write(clientHandler.getBuffer());
        if (!clientHandler.getBuffer().hasRemaining()) {
            serverKey.interestOps(serverKey.interestOps() & ~SelectionKey.OP_WRITE);
            clientKey.interestOps(clientKey.interestOps() | SelectionKey.OP_READ);
        }
    }

    public void read() throws IOException {
        buffer.clear();
        if (channel.read(buffer) == -1) {
            hasNoData = true;
            serverKey.interestOps(serverKey.interestOps() & ~SelectionKey.OP_READ);
            ClientHandler clientHandler = (ClientHandler) clientKey.attachment();
            clientHandler.shutDownOutput();
            if (clientHandler.isHasNoData()) {
                close();
            }
            return;
        }
        buffer.flip();
        serverKey.interestOps(serverKey.interestOps() & ~SelectionKey.OP_READ);
        clientKey.interestOps(clientKey.interestOps() | SelectionKey.OP_WRITE);
    }

    public void nextState() throws IOException {
        if (state == CONNECTION && serverKey.isConnectable()) {
            if (!channel.finishConnect()) {
                throw new IOException();
            }
            state = FORWARDING;
            ((ClientHandler) clientKey.attachment()).makeResponse(REQUEST_GRANTED);
            serverKey.interestOps(SelectionKey.OP_READ);
            buffer.clear();
            buffer.flip();
        }
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public ConnectState getState() {
        return state;
    }

    public boolean isHasNoData() {
        return hasNoData;
    }

    public void shutDownOutput() throws IOException {
        channel.shutdownOutput();
    }

    @Override
    public void close() throws IOException {
        if (channel!= null) {
            channel.close();
        }
        if (clientKey != null) {
            clientKey.channel().close();
        }
    }
}
