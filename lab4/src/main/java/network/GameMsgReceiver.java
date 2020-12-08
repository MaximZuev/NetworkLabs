package network;

import me.ippolitov.fit.snakes.SnakesProto.*;
import me.ippolitov.fit.snakes.SnakesProto.GameMessage.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GameMsgReceiver extends Thread {
    private final DatagramSocket datagramSocket;
    private final Node node;
    private final GameMsgSender sender;
    private final List<GameMessage> noAckMessages;
    private final Map<Integer, Long> lastPingInTime;

    public GameMsgReceiver(DatagramSocket datagramSocket, Node node, GameMsgSender sender,
                           List<GameMessage> noAckMessages, Map<Integer, Long> lastPingInTime) {
        this.datagramSocket = datagramSocket;
        this.node = node;
        this.sender = sender;
        this.noAckMessages = noAckMessages;
        this.lastPingInTime = lastPingInTime;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            while (!Thread.currentThread().isInterrupted()) {
                datagramSocket.receive(packet);
                GameMessage gameMessage = GameMessage.parseFrom(Arrays.copyOf(packet.getData(), packet.getLength()));
                if (!gameMessage.hasJoin()) {
                    lastPingInTime.put(gameMessage.getSenderId(), System.currentTimeMillis());
                }
                if (gameMessage.hasJoin()) {
                    handleJoinMsg(gameMessage, packet.getAddress(), packet.getPort());
                }
                if (gameMessage.hasState()) {
                    handleStateMsg(gameMessage, packet.getAddress(), packet.getPort());
                }
                if (gameMessage.hasSteer()) {
                    handleSteerMsg(gameMessage, packet.getAddress(), packet.getPort());
                }
                if (gameMessage.hasAck()) {
                    handleAckMsg(gameMessage, packet.getAddress(), packet.getPort());
                }
                if (gameMessage.hasPing()) {
                    handlePingMsg(gameMessage, packet.getAddress(), packet.getPort());
                }
                if (gameMessage.hasRoleChange()) {
                    handleRoleChangeMsg(gameMessage, packet.getAddress(), packet.getPort());
                }
            }
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    private void handlePingMsg(GameMessage gameMessage, InetAddress address, int port) {
        sender.sendAckMsg(node.getId(), gameMessage.getSenderId(), gameMessage.getMsgSeq(), address, port);
    }

    private void handleAckMsg(GameMessage gameMessage, InetAddress address, int port) {
        noAckMessages.removeIf(n -> {
            if ((n.getMsgSeq() == gameMessage.getMsgSeq()) && (n.getReceiverId() == gameMessage.getSenderId())) {
                if (n.hasJoin()) {
                    NodeRole role = (n.getJoin().getOnlyView()) ? NodeRole.VIEWER : NodeRole.NORMAL;
                    node.startNotMaster(role, gameMessage.getReceiverId(), gameMessage.getSenderId(), address, port);
                }
                return true;
            } else {
                return false;
            }
        });
    }

    private void handleSteerMsg(GameMessage gameMessage, InetAddress address, int port) {
        node.changeDirection(gameMessage.getSenderId(), gameMessage.getSteer().getDirection());
        sender.sendAckMsg(node.getId(), gameMessage.getSenderId(), gameMessage.getMsgSeq(), address, port);
    }

    private void handleRoleChangeMsg(GameMessage gameMessage, InetAddress address, int port) {
        RoleChangeMsg roleChangeMsg = gameMessage.getRoleChange();
        NodeRole receiverRole = roleChangeMsg.getReceiverRole();
        NodeRole senderRole = roleChangeMsg.getSenderRole();
        if (receiverRole == NodeRole.DEPUTY) {
            node.setRole(NodeRole.DEPUTY);
        }
        if ((receiverRole == NodeRole.MASTER) && (senderRole == NodeRole.DEPUTY)) {
            node.setMaster(gameMessage.getSenderId(), port, address);
        }
        sender.sendAckMsg(node.getId(), gameMessage.getSenderId(), gameMessage.getMsgSeq(), address, port);
    }

    private void handleStateMsg(GameMessage gameMessage, InetAddress address, int port) {
        node.updateModel(gameMessage.getState().getState());
        sender.sendAckMsg(node.getId(), gameMessage.getSenderId(), gameMessage.getMsgSeq(), address, port);
    }

    private void handleJoinMsg(GameMessage gameMessage, InetAddress address, int port) {
        JoinMsg joinMsg = gameMessage.getJoin();
        int playerId;
        if (joinMsg.getOnlyView()) {
            playerId = node.addNewPlayer(NodeRole.VIEWER, address.toString(), port, joinMsg.getName());
            sender.sendAckMsg(node.getId(), playerId, gameMessage.getMsgSeq(), address, port);
            lastPingInTime.put(playerId, System.currentTimeMillis());
        } else {
            playerId = node.addNewPlayer(NodeRole.NORMAL, address.toString(), port, joinMsg.getName());
            if (playerId > 0) {
                sender.sendAckMsg(node.getId(), playerId, gameMessage.getMsgSeq(), address, port);
                lastPingInTime.put(playerId, System.currentTimeMillis());
            } else {
                sender.sendErrorMsg(node.getId(), playerId, "There is no free space.", address, port);
            }
        }
    }
}
