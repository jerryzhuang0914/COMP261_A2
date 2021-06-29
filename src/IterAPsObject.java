import java.util.List;

public class IterAPsObject {

    private Node node;
    private int depth;
    private Node root;

    public IterAPsObject(Node node, int depth, Node root) {
        this.node = node;
        this.depth = depth;
        this.root = root;
    }

    public Node getNode() {
        return node;
    }
    public int getDepth() {
        return depth;
    }
    public Node getParent() {
        return root;
    }
}
