package view;

import me.ippolitov.fit.snakes.SnakesProto.*;
import model.Field;
import model.Model;
import network.Node;
import observer.Observer;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class View extends JFrame implements Observer {
    private final JPanel jPanel = new JPanel();
    private final static int CELL_SIZE = 10;
    private final Node node;

    public View(Node node) {
        super("Press 1-5 to connect to the game. Press 0 to start the game.");
        this.node = node;

        setVisible(true);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(0,0,1000,500);
        add(jPanel);
        setGameFrame();
    }

    public void setGameFrame() {
        jPanel.setLayout(new BorderLayout());

        JComponent gameField = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                List<List<String>> gamesInfo = node.getGamesInfo();
                for (int i = 0; i < Math.min(5, gamesInfo.size()); ++i) {
                    g.drawString((i + 1) + ": "
                            + gamesInfo.get(i).get(0) + " "
                            + gamesInfo.get(i).get(1) + " "
                            + gamesInfo.get(i).get(2) + " "
                            + gamesInfo.get(i).get(3) + " "
                            + gamesInfo.get(i).get(4),
                            0, 10 * (i + 1));
                }

                Model model = node.getModel();
                if (model != null) {
                    GameConfig config = model.getConfig();
                    Field field = model.getField();
                    for (int i = 0; i < config.getWidth(); ++i) {
                        for (int j = 0; j < config.getHeight(); ++j) {
                            g.setColor(Color.BLACK);
                            g.drawRect(300 + i * CELL_SIZE, j * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                            switch (field.getCell(i, j).getState()) {
                                case NOTHING:
                                    g.setColor(Color.WHITE);
                                    break;
                                case BODY:
                                    g.setColor(Color.GRAY);
                                    break;
                                case HEAD:
                                    g.setColor(Color.DARK_GRAY);
                                    break;
                                case FOOD:
                                    g.setColor(Color.RED);
                                    break;
                            }
                            g.fillRect(300 + i * CELL_SIZE + 1, j * CELL_SIZE + 1, CELL_SIZE - 1, CELL_SIZE - 1);

                        }
                    }

                }
            }
        };
        jPanel.add(gameField);
        gameField.revalidate();
    }

    @Override
    public void update() {
        jPanel.repaint();
    }
}
