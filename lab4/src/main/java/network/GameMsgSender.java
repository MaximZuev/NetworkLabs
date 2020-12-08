package network;

import me.ippolitov.fit.snakes.SnakesProto.*;
import me.ippolitov.fit.snakes.SnakesProto.GameMessage.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

public class GameMsgSender {
    private int msgSeq;
    private final DatagramSocket datagramSocket;
    private final Map<Integer, Long> lastPingOutTime;
    private final List<GameMessage> noAckMessages;
    private final InetAddress multicastIp;
    private final int multicastPort;

    public GameMsgSender(DatagramSocket datagramSocket, List<GameMessage> noAckMessages,
                         Map<Integer, Long> lastPingOutTime, String multicastIp, int multicastPort) throws UnknownHostException {
        this.datagramSocket = datagramSocket;
        this.noAckMessages = noAckMessages;
        this.lastPingOutTime = lastPingOutTime;
        this.multicastIp = InetAddress.getByName(multicastIp);
        this.multicastPort = multicastPort;
    }

    public void sendGameMsg(GameMessage gameMsg, InetAddress address, int port, boolean isResend) {
        byte[] bytesToSend = gameMsg.toByteArray();
        DatagramPacket packet = new DatagramPacket(bytesToSend, bytesToSend.length, address, port);
        if (!gameMsg.hasAnnouncement()) {
            lastPingOutTime.put(gameMsg.getReceiverId(), System.currentTimeMillis());
            if (!isResend  && !gameMsg.hasAck()) {
                noAckMessages.add(gameMsg);
            }
        }
        try {
            datagramSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendJoinMsg(int receiverId, String name, InetAddress address, int port, boolean isOnlyView) {
        JoinMsg joinMsg = JoinMsg.newBuilder()
                .setName(name)
                .setOnlyView(isOnlyView)
                .build();
        GameMessage gameMessage = GameMessage.newBuilder()
                .setJoin(joinMsg)
                .setReceiverId(receiverId)
                .setMsgSeq(++msgSeq)
                .build();
        sendGameMsg(gameMessage, address, port, false);
    }

    public void sendStateMsg(int senderId, int receiverId, GameState state, InetAddress address, int port) {
        StateMsg stateMsg = StateMsg.newBuilder()
                .setState(state)
                .build();
        GameMessage gameMessage = GameMessage.newBuilder()
                .setState(stateMsg)
                .setSenderId(senderId)
                .setReceiverId(receiverId)
                .setMsgSeq(++msgSeq)
                .build();
        sendGameMsg(gameMessage, address, port, false);
    }

    public void sendAnnouncementMsg(GameState state) {
        GamePlayers players = state.getPlayers();
        GameConfig config = state.getConfig();
        AnnouncementMsg announcementMsg = AnnouncementMsg.newBuilder()
                .setConfig(config)
                .setPlayers(players)
                .build();
        GameMessage gameMsg = GameMessage.newBuilder()
                .setMsgSeq(++msgSeq)
                .setAnnouncement(announcementMsg)
                .build();
        sendGameMsg(gameMsg, multicastIp, multicastPort, false);
    }

    public void sendSteerMsg(int senderId, int receiverId, Direction direction, InetAddress masterIp, int masterPort) {
        SteerMsg steerMsg = SteerMsg.newBuilder()
                .setDirection(direction)
                .build();
        GameMessage gameMessage = GameMessage.newBuilder()
                .setSteer(steerMsg)
                .setSenderId(senderId)
                .setReceiverId(receiverId)
                .setMsgSeq(++msgSeq)
                .build();
        sendGameMsg(gameMessage, masterIp, masterPort, false);
    }

    public void sendAckMsg(int senderId, int receiverId, long msg_seq, InetAddress address, int port) {
        AckMsg ackMsg = AckMsg.newBuilder().build();
        GameMessage gameMessage = GameMessage.newBuilder()
                .setAck(ackMsg)
                .setMsgSeq(msg_seq)
                .setSenderId(senderId)
                .setReceiverId(receiverId)
                .build();
        sendGameMsg(gameMessage, address, port, false);
    }

    public void sendErrorMsg(int senderId, int receiverId, String errorMessage, InetAddress address, int port) {
        ErrorMsg errorMsg = ErrorMsg.newBuilder()
                .setErrorMessage(errorMessage)
                .build();
        GameMessage gameMessage = GameMessage.newBuilder()
                .setError(errorMsg)
                .setSenderId(senderId)
                .setReceiverId(receiverId)
                .setMsgSeq(++msgSeq)
                .build();
        sendGameMsg(gameMessage, address, port, false);
    }

    public void sendRoleChangeMsg(int senderId, int receiverId, NodeRole senderRole, NodeRole receiverRole,
                                               InetAddress address, int port) {
        RoleChangeMsg roleChangeMsg = RoleChangeMsg.newBuilder()
                .setReceiverRole(receiverRole)
                .setSenderRole(senderRole)
                .build();
        GameMessage gameMessage = GameMessage.newBuilder()
                .setRoleChange(roleChangeMsg)
                .setMsgSeq(++msgSeq)
                .setReceiverId(receiverId)
                .setSenderId(senderId)
                .build();
        sendGameMsg(gameMessage, address, port, true);
    }

    public void sendPingMsg(int senderId, int receiverId, InetAddress address, int port) {
        PingMsg pingMsg = PingMsg.newBuilder().build();
        GameMessage gameMessage = GameMessage.newBuilder()
                .setPing(pingMsg)
                .setSenderId(senderId)
                .setReceiverId(receiverId)
                .setMsgSeq(++msgSeq)
                .build();
        sendGameMsg(gameMessage, address, port, false);
    }
}
