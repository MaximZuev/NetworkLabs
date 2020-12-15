import handlers.AcceptHandler;
import handlers.Handler;
import handlers.ResolveHandler;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;

public class ProxyServer implements Runnable, Closeable {
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private DatagramChannel datagramChannel;
    private ResolveHandler resolveAttachment;

    public ProxyServer(int port) throws IOException {
        selector = Selector.open();

        datagramChannel = DatagramChannel.open();
        datagramChannel.configureBlocking(false);
        SelectionKey selectionKey = datagramChannel.register(selector, 0);
        resolveAttachment = new ResolveHandler(selectionKey);
        selectionKey.attach(resolveAttachment);

        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(port));
        serverSocketChannel.register(selector, serverSocketChannel.validOps(), new AcceptHandler(resolveAttachment));
    }



    @Override
    public void run() {
        try {
            while (true) {
                selector.select();
                Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey selectionKey = selectedKeys.next();
                    selectedKeys.remove();
                    if (selectionKey.isValid()) {
                        Handler handler = (Handler) selectionKey.attachment();
                        try {
                            handler.handle(selectionKey);
                        } catch (IOException e) {
                            e.printStackTrace();
                            ((Handler) selectionKey.attachment()).close();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        if (serverSocketChannel != null) {
            serverSocketChannel.close();
        }
        if (datagramChannel != null) {
            datagramChannel.close();
        }
    }
}
