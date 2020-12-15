package network;

import me.ippolitov.fit.snakes.SnakesProto.*;
import me.ippolitov.fit.snakes.SnakesProto.GameState.Coord;
import me.ippolitov.fit.snakes.SnakesProto.GameState.Snake;
import model.Model;
import observer.Observable;
import observer.Observer;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Node implements Closeable, Observable {
    private static final String MULTICAST_IP = "239.192.0.4";
    private static final int MULTICAST_PORT = 9192;
    private static final long ANNOUNCEMENT_MSG_PERIOD_MS = 1000;

    private MulticastSocket multicastSocket;
    private DatagramSocket datagramSocket;
    private Model model;
    private GameState state;
    private GameConfig config = GameConfig.newBuilder().build();;

    private NodeRole role;
    private int id;
    private int masterId;
    private InetAddress masterIp;
    private int masterPort;
    private int playersId;


    private  Timer modelTimer;
    private Timer announcementMsgTimer;
    private Timer pingMsgTimer;
    private Timer TTLTimer;

    private final List<Observer> observers = new ArrayList<>();


    private AnnouncementMsgReceiver announceMsgReceiver;
    private GameMsgReceiver gameMsgReceiver;
    private  GameMsgSender gameMsgSender;
    private final List<GameMessage> noAckMessages = new CopyOnWriteArrayList<>();
    private final Map<Integer, Long> lastPingOutTime = new ConcurrentHashMap<>();
    private final Map<Integer, Long> lastPingInTime = new ConcurrentHashMap<>();

    private final List<List<String>> gamesInfo = new CopyOnWriteArrayList<>();

    public Node() {
        try {
            datagramSocket = new DatagramSocket();
            multicastSocket = new MulticastSocket(MULTICAST_PORT);
            multicastSocket.joinGroup(InetAddress.getByName(MULTICAST_IP));
            gameMsgSender = new GameMsgSender(datagramSocket, noAckMessages, lastPingOutTime, MULTICAST_IP, MULTICAST_PORT);
            gameMsgReceiver = new GameMsgReceiver(datagramSocket, this, gameMsgSender, noAckMessages, lastPingInTime);
            announceMsgReceiver = new AnnouncementMsgReceiver(gamesInfo, multicastSocket, ANNOUNCEMENT_MSG_PERIOD_MS);
            gameMsgReceiver.start();
            announceMsgReceiver.start();
        } catch (IOException e) {
            e.printStackTrace();
            close();
        }
    }

    public void createModel() {
        config = GameConfig.newBuilder().build();
        id = ++playersId;
        role = NodeRole.MASTER;
        GamePlayer player = createGamePlayer(id, role, "", datagramSocket.getLocalPort(), "host");
        Snake snake = createSnake(id, createCoord(2, 2));
        GamePlayers players = GamePlayers.newBuilder()
                .addPlayers(player)
                .build();
        state = GameState.newBuilder()
                .setStateOrder(0)
                .setConfig(config)
                .setPlayers(players)
                .addSnakes(snake)
                .build();
        model = new Model(state);
    }

    public void startMaster() {
        stopPlay();
        playersId = 0;
        createModel();

        modelTimer = new Timer();
        modelTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                model.step();
                notifyObservers();
                state = model.getState();
                for (GamePlayer player: state.getPlayers().getPlayersList()) {
                    if (player.getRole() != NodeRole.MASTER) {
                        try {
                            InetAddress address = InetAddress.getByName(player.getIpAddress().substring(1));
                            gameMsgSender.sendStateMsg(id, player.getId(), state, address, player.getPort());
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
                notifyObservers();
            }
        }, 0, config.getStateDelayMs());

        startSendAnnouncementMsg();
        startCheckPlayerTTL();
        startSendPingMsg();
    }

    private void startSendAnnouncementMsg() {
        announcementMsgTimer = new Timer();
        announcementMsgTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                gameMsgSender.sendAnnouncementMsg(state);
            }
        }, 0, 1000);
    }

    private void startSendPingMsg() {
        pingMsgTimer = new Timer();
        pingMsgTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                lastPingOutTime.keySet().removeIf(n -> {
                    if ((n != id) && (System.currentTimeMillis() - lastPingOutTime.get(n) > config.getPingDelayMs())) {
                        GamePlayer player = model.getPlayer(n);
                        if (player != null) {
                            try {
                                gameMsgSender.sendPingMsg(id, n, InetAddress.getByName(player.getIpAddress().substring(1)),
                                        player.getPort());
                                return false;
                            } catch (IOException ex) {
                                ex.printStackTrace();
                                return true;
                            }
                        } else {
                            return true;
                        }
                    } else {
                        return false;
                    }
                });
            }
        }, 0, config.getPingDelayMs());
    }

    private void startCheckPlayerTTL() {
        TTLTimer = new Timer();
        TTLTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                lastPingInTime.keySet().removeIf(n -> {
                    if ((n != id) && (System.currentTimeMillis() - lastPingInTime.get(n) > config.getNodeTimeoutMs())) {
                        GamePlayer player = model.getPlayer(n);
                        switch (role) {
                            case NORMAL:
                                if (player.getRole() == NodeRole.MASTER) {
                                    changeMaster();
                                }
                                break;
                            case MASTER:
                                model.removePlayer(n);
                                if (player.getRole() == NodeRole.DEPUTY) {
                                    findNewDeputy();
                                }
                                break;
                            case DEPUTY:
                                startMaster();
                                for (GamePlayer gamePlayer : state.getPlayers().getPlayersList()) {
                                    if (gamePlayer.getRole() != NodeRole.MASTER) {
                                        try {
                                            gameMsgSender.sendRoleChangeMsg(id, player.getId(), role, NodeRole.MASTER,
                                                    InetAddress.getByName(player.getIpAddress()), player.getPort());
                                            break;
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                                break;
                            default:
                                break;
                        }
                        return true;
                    }
                    return false;
                });
            }
        }, 0, config.getPingDelayMs());
    }

    public void findNewDeputy() {
        for (GamePlayer player : state.getPlayers().getPlayersList()) {
            if (player.getRole() == NodeRole.NORMAL) {
                try {
                    gameMsgSender.sendRoleChangeMsg(id, player.getId(), role, NodeRole.DEPUTY,
                            InetAddress.getByName(player.getIpAddress()), player.getPort());
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void changeMaster() {
        for (GamePlayer player : state.getPlayers().getPlayersList()) {
            if (player.getRole() == NodeRole.DEPUTY) {
                try {
                    masterId = player.getId();
                    masterPort = player.getPort();
                    masterIp = InetAddress.getByName(player.getIpAddress());
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setMaster(int masterId, int masterPort, InetAddress masterIp) {
        this.masterId = masterId;
        this.masterPort = masterPort;
        this.masterIp = masterIp;
    }

    public void stopPlay() {
        if (role == NodeRole.MASTER) {
            modelTimer.cancel();
            announcementMsgTimer.cancel();
        }
        if (TTLTimer != null) {
            TTLTimer.cancel();
        }
        if (pingMsgTimer != null) {
            pingMsgTimer.cancel();
        }
        model = null;
        state = null;
    }

    public void joinToGame(int gameNumber, boolean isOnlyView) {
        if (gameNumber <= gamesInfo.size()) {
            try {
                System.out.println(gamesInfo.get(gameNumber - 1).get(0).split(":")[1]);
                InetAddress address = InetAddress.getByName(gamesInfo.get(gameNumber - 1).get(0).split(":")[1].substring(1));
                int port = Integer.parseInt(gamesInfo.get(gameNumber - 1).get(0).split(":")[2]);
                gameMsgSender.sendJoinMsg(1, "", address, port, isOnlyView);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void startNotMaster(NodeRole role, int id, int masterId, InetAddress masterIp, int masterPort) {
        stopPlay();
        this.role = role;
        this.id = id;
        this.masterId = masterId;
        this.masterIp = masterIp;
        this.masterPort = masterPort;
    }

    public void updateModel(GameState state) {
        if (role != NodeRole.MASTER) {
            if (this.state != null) {
                if (this.state.getStateOrder() < state.getStateOrder()) {
                    this.state = state;
                    model.update(state);
                }
            } else {
                this.state = state;
                model = new Model(state);
                startCheckPlayerTTL();
                startSendPingMsg();
            }
            notifyObservers();
        }
    }

    public synchronized int addNewPlayer(NodeRole role, String ip, int port, String name) {
        if (role == NodeRole.NORMAL) {
            Coord headCoord = model.getField().getNewHeadCoord();
            if (headCoord != null) {
                ++playersId;
                Snake snake = createSnake(playersId, headCoord);
                GamePlayer player = createGamePlayer(playersId, NodeRole.NORMAL, ip, port, name);
                model.addPlayer(player);
                model.addSnake(snake);
                return playersId;
            } else {
                return -1;
            }
        } else {
            ++playersId;
            GamePlayer player = createGamePlayer(playersId, NodeRole.VIEWER, ip, port, name);
            model.addPlayer(player);
            return playersId;
        }
    }

    public void changeDirection(Direction direction) {
        if (role == NodeRole.MASTER) {
            model.addDirectionChange(1, direction);
        } else {
            gameMsgSender.sendSteerMsg(id, masterId, direction, masterIp, masterPort);
        }
    }

    public void changeDirection(int id, Direction direction) {
        model.addDirectionChange(id, direction);
    }

    private Coord createCoord(int x, int y) {
        return Coord.newBuilder()
                .setX(x)
                .setY(y)
                .build();
    }

    public Snake createSnake(int id, Coord headCoord) {
        Coord tailCoord = createCoord(-1, 0);

        return Snake.newBuilder()
                .setPlayerId(id)
                .addPoints(headCoord)
                .addPoints(tailCoord)
                .setHeadDirection(Direction.RIGHT)
                .setState(Snake.SnakeState.ALIVE)
                .build();
    }

    public GamePlayer createGamePlayer(int id, NodeRole role, String ip, int port, String name) {
        return GamePlayer.newBuilder()
                .setId(id)
                .setRole(role)
                .setIpAddress(ip)
                .setPort(port)
                .setName(name)
                .setScore(0)
                .build();
    }

    public Model getModel() {
        return model;
    }

    public int getId() {
        return id;
    }

    public NodeRole getRole() {
        return role;
    }

    public void setRole(NodeRole role) {
        this.role = role;
    }

    public List<List<String>> getGamesInfo() {
        return gamesInfo;
    }

    @Override
    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        for (Observer obs : observers) obs.update();
    }

    @Override
    public void close() {
        stopPlay();
        gameMsgReceiver.interrupt();
        announceMsgReceiver.close();
        announceMsgReceiver.interrupt();
        datagramSocket.close();
        multicastSocket.close();
        try {
            gameMsgReceiver.join();
            announceMsgReceiver.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
