import controller.Controller;
import network.Node;
import view.View;

public class Main {
    public static void main(String[] args) {
        Node node = new Node();
        View view = new View(node);
        Controller controller = new Controller(node);
        view.addKeyListener(controller);
        node.addObserver(view);
    }
}
