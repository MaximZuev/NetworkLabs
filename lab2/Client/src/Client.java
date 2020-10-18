import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Client implements Closeable {
    private final File file;
    private final Socket socket;
    private final DataInputStream in;
    private final DataInputStream dis;
    private final DataOutputStream dos;

    public Client(File file, String host, int port) throws IOException {
        this.file = file;
        socket = new Socket(InetAddress.getByName(host), port);
        dos = new DataOutputStream(socket.getOutputStream());
        dis = new DataInputStream(socket.getInputStream());
        in = new DataInputStream(new FileInputStream(this.file));
    }

    public void start() throws IOException {
        sendFileLengthAndName();
        sendFile();
        check();
    }

    private void sendFileLengthAndName() throws IOException {
        dos.writeLong(file.length());

        byte[] fileName = file.getName().getBytes(StandardCharsets.UTF_8);
        dos.writeInt(fileName.length);
        dos.write(fileName);
        dos.flush();
    }

    private void sendFile() throws IOException {
        byte[] buffer = new byte[1024];
        int bytes;
        while ((bytes = in.read(buffer, 0 , 1024)) > 0) {
            dos.write(buffer, 0, bytes);
            dos.flush();
        }
    }

    private void check() throws IOException {
        boolean flag = dis.readBoolean();
        if (flag) {
            System.out.println("File sent successfully.");
        } else {
            System.out.println("File was not sent successfully");
        }
    }


    @Override
    public void close() throws IOException {
        in.close();
        dos.close();
        dis.close();
        socket.close();
    }
}
