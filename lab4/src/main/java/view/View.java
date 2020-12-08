package view;

import me.ippolitov.fit.snakes.SnakesProto.*;
import model.Field;
import network.Node;
import observer.Observer;

import javax.swing.*;
import java.awt.*;

public class View extends JFrame implements Observer {
    private final JPanel jPanel = new JPanel();
    private final Node node;


    public View(Node node) {
        super("Snake");
        this.node = node;

        setVisible(true);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(0,0,1000,500);
        add(jPanel);

        setGameFrame();
    }

    public void setGameFrame() {
        jPanel.removeAll();
        jPanel.setLayout(new BorderLayout());

        int cellSize = 10;

        JComponent gameField = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                GameConfig config = node.getConfig();
                Field field = node.getField();
                if (field != null)  {
                    for (int i = 0; i < config.getWidth(); ++i) {
                        for (int j = 0; j < config.getHeight(); ++j) {
                            g.setColor(Color.BLACK);
                            g.drawRect(i * cellSize, j * cellSize, cellSize, cellSize);
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
                            g.fillRect(i * cellSize + 1, j * cellSize + 1, cellSize - 1, cellSize - 1);
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
