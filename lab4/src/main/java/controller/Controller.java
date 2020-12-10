package controller;

import me.ippolitov.fit.snakes.SnakesProto.*;
import network.Node;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class Controller extends KeyAdapter {
    private final Node node;

    public Controller(Node node) {
        this.node = node;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_RIGHT:
                node.changeDirection(Direction.RIGHT);
                break;
            case KeyEvent.VK_LEFT:
                node.changeDirection(Direction.LEFT);
                break;
            case KeyEvent.VK_UP:
                node.changeDirection(Direction.UP);
                break;
            case KeyEvent.VK_DOWN:
                node.changeDirection(Direction.DOWN);
                break;
            case KeyEvent.VK_0:
                node.startMaster();
                break;
            case KeyEvent.VK_1:
                node.joinToGame(1, false);
                break;
            case KeyEvent.VK_2:
                node.joinToGame(2, false);
                break;
            case KeyEvent.VK_3:
                node.joinToGame(3, false);
                break;
            case KeyEvent.VK_4:
                node.joinToGame(4, false);
                break;
            case KeyEvent.VK_5:
                node.joinToGame(5, false);
                break;
            default:
                break;
        }
    }
}
