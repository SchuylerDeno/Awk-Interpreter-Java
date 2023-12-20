public class ReturnNode extends StatementNode{
    private Node value;

    public ReturnNode(Node value){
        this.value = value;
    }

    public Node getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "return " + value;
    }
}
