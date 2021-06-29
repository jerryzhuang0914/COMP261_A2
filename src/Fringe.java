public class Fringe implements Comparable <Fringe>{

    public Node currentNode;
    public Node previousNode;
    public double g_Value;
    public double f_Value;

    public Fringe(Node currentNode, Node previousNode, double g_Value, double f_Value) {
        this.currentNode = currentNode;
        this.previousNode = previousNode;
        this.g_Value = g_Value;
        this.f_Value = f_Value;
    }

    public Node getCurrentNode() {
        return currentNode;
    }

    public Node getPreviousNode() {
        return previousNode;
    }

    public double getG_Value() {
        return g_Value;
    }

    public double getF_Value() {
        return f_Value;
    }

    // Compare the f value among all the elements in the fringe.
    @Override
    public int compareTo(Fringe fringe) {
        if (this.f_Value < fringe.f_Value) {
            return -1;
        } else if (this.f_Value > fringe.f_Value) {
            return 1;
        } else {
            return 0;
        }
    }
}
