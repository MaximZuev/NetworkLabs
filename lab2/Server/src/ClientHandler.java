import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class ClientHandler implements Runnable {
    private final DataInputStream dis;
    private final DataOutputStream dos;
    private final Socket socket;

    private long fileLength;
    private String fileName;
    private File file;
    private int countPrint;

    private long totalRecvBytes;
    private long lastRcvBytes;

    private boolean isEnded;

    private final MessageDigest md5Digest;

    public ClientHandler(Socket socket) throws IOException, NoSuchAlgorithmException {
        this.socket = socket;
        dis = new DataInputStream(socket.getInputStream());
        dos = new DataOutputStream(socket.getOutputStream());
        md5Digest = MessageDigest.getInstance("MD5");
    }

    @Override
    public void run() {
        try {
            startTimer();
            recvFileLengthAndName();
            createFile();
            recvFile();
            recvChecksum();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                dis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                dos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void recvFileLengthAndName() throws IOException {
        fileLength = dis.readLong();

        int fileNameLength = dis.readInt();
        byte[] fileNameBytes = new byte[fileNameLength];
        dis.readFully(fileNameBytes, 0, fileNameLength);
        fileName = new String(fileNameBytes, StandardCharsets.UTF_8);
    }

    private void createFile() throws IOException {
        int i = 0;
        do {
            if (i == 0) {
                file = new File( "uploads/" + fileName);
            } else {
                file = new File("uploads/" + i + "_" + fileName);
            }
            ++i;
        } while (file.exists());

        if (!file.createNewFile()) {
            throw new IOException();
        }
    }

    private void recvFile() {
        try (FileOutputStream out = new FileOutputStream(file)) {
            int bytes, len;
            byte[] buffer = new byte[1024];
            if (fileLength > 1024) {
                len = 1024;
            } else {
                len = (int) fileLength;
            }
            while ((bytes = dis.read(buffer, 0, len)) > 0) {
                totalRecvBytes += bytes;
                lastRcvBytes += bytes;

                out.write(buffer, 0, bytes);
                out.flush();

                if ((fileLength - totalRecvBytes) < 1024) {
                    len = (int) (fileLength - totalRecvBytes);
                }

                md5Digest.update(buffer, 0, bytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void recvChecksum() throws IOException {
        byte[] clientChecksum = new byte[16];
        byte[] serverChecksum = md5Digest.digest();
        boolean flag = false;
        if (dis.read(clientChecksum, 0, 16) > 0) {
            flag = Arrays.equals(serverChecksum, clientChecksum);
        }
        dos.writeBoolean(flag);
        dos.flush();
        isEnded = true;
    }

    private void printInfo() {
        System.out.println("Client: " + socket.getInetAddress() + ":" + socket.getPort());
        System.out.println("instant speed " + (lastRcvBytes / 3) + " bytes/sec.");
        System.out.println("average speed " + (totalRecvBytes / (++countPrint * 3)) + " bytes/sec.\n");
        lastRcvBytes = 0;
    }

    private void startTimer() {
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                printInfo();
                if (isEnded) {
                    timer.cancel();
                }
            }
        };
        timer.schedule(timerTask, 3000, 3000);
    }
}
