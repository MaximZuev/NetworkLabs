package network;

import me.ippolitov.fit.snakes.SnakesProto.*;
import me.ippolitov.fit.snakes.SnakesProto.GameMessage.*;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.*;

public class AnnouncementMsgReceiver extends Thread implements Closeable {
    private final MulticastSocket multicastSocket;
    private final Map<List<String>, Long> receivedMsg;
    private final List<List<String>> gamesInfo;
    private final Timer timer;
    private final long period;

    public AnnouncementMsgReceiver(List<List<String>> gamesInfo, MulticastSocket multicastSocket, long period) throws IOException {
        this.gamesInfo = gamesInfo;
        this.multicastSocket = multicastSocket;
        this.period = period;
        receivedMsg = new HashMap<>();
        timer = new Timer();
    }

    @Override
    public void run() {
        startDeleteOldMsg();

        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            while (!Thread.currentThread().isInterrupted()) {
                multicastSocket.receive(packet);
                GameMessage gameMessage = GameMessage.parseFrom(Arrays.copyOf(packet.getData(), packet.getLength()));
                if (gameMessage.hasAnnouncement()) {
                    AnnouncementMsg announcementMsg = gameMessage.getAnnouncement();
                    List<String> gameInfo = parseAnnouncementMsg(announcementMsg, packet.getAddress(), packet.getPort());

                    receivedMsg.put(gameInfo, System.currentTimeMillis());
                }
            }
        } catch (IOException ex) {
            //ex.printStackTrace();
        }
    }

    private void startDeleteOldMsg() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                gamesInfo.clear();
                Iterator<List<String>> iterator = receivedMsg.keySet().iterator();
                for (int i = 0; i < Math.min(5, receivedMsg.size()); ++i) {
                    gamesInfo.add(iterator.next());
                }
                receivedMsg.values().removeIf(time -> ((System.currentTimeMillis() - time) > period));
            }
        }, 0, period);
    }

    private List<String> parseAnnouncementMsg(AnnouncementMsg announcementMsg, InetAddress address, int port) {
        GamePlayers players = announcementMsg.getPlayers();
        GameConfig config = announcementMsg.getConfig();
        String size = config.getWidth() + "x" + config.getHeight();
        String food = config.getFoodStatic() + "+" + config.getFoodPerPlayer() + "x";
        String playersCount = Integer.toString(players.getPlayersCount());
        String canJoin = Boolean.toString(announcementMsg.getCanJoin());
        String host = null;
        for (int i = 0; i < players.getPlayersCount(); ++i) {
            GamePlayer player = players.getPlayers(i);
            if (player.getRole() == NodeRole.MASTER) {
                host = player.getName() + ":" + address + ":" + port;
                break;
            }
        }
        return Arrays.asList(host, size, food, playersCount, canJoin);
    }

    @Override
    public void close() {
        timer.cancel();
    }
}

