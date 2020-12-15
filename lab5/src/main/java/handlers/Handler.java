package handlers;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SelectionKey;

public abstract class Handler implements Closeable {
    abstract public void handle(SelectionKey selectionKey) throws IOException;
}
