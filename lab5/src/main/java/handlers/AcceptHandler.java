package handlers;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class AcceptHandler extends Handler {
    private ResolveHandler resolveHandler;

    public AcceptHandler(ResolveHandler resolveHandler) {
        this.resolveHandler = resolveHandler;
    }

    @Override
    public void handle(SelectionKey selectionKey) throws IOException {
        SocketChannel clientSocketChannel = ((ServerSocketChannel) selectionKey.channel()).accept();
        clientSocketChannel.configureBlocking(false);
        SelectionKey clientSelectionKey = clientSocketChannel.register(selectionKey.selector(), SelectionKey.OP_READ);
        clientSelectionKey.attach(new ClientHandler(clientSelectionKey, resolveHandler));
    }

    @Override
    public void close() throws IOException {

    }
}
