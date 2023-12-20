import java.util.LinkedList;

public class DoWhileNode extends StatementNode{
    private Node condition;

    private LinkedList<StatementNode> statements;

    public DoWhileNode(Node condition, LinkedList<StatementNode> statements){
        this.condition = condition;
        this.statements = statements;
    }

    public Node getCondition() {
        return condition;
    }

    public LinkedList<StatementNode> getStatements() {
        return statements;
    }

    @Override
    public String toString() {
        return "do{" + statements + "}while" + condition;
    }
}
