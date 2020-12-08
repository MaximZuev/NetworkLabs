import controller.Controller;
import me.ippolitov.fit.snakes.SnakesProto;
import me.ippolitov.fit.snakes.SnakesProto.GameState.*;
import model.Model;
import network.Node;
import view.View;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Main {
    public static void main(String[] args) {
        Node node = new Node();
        //node.startMaster();
        View view = new View(node);
        Controller controller = new Controller(node);
        view.addKeyListener(controller);
        node.addObserver(view);

    }

}
