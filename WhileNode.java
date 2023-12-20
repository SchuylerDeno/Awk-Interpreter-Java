import java.util.LinkedList;
import java.util.Optional;

public class WhileNode extends StatementNode{
    private Node condition;

    private LinkedList<StatementNode> statements;

    public WhileNode(Node condition, LinkedList<StatementNode> statements){
        this.condition = condition;
        this.statements = statements;
    }

    public LinkedList<StatementNode> getStatements() {
        return statements;
    }

    public Node getCondition() {
        return condition;
    }

    @Override
    public String toString() {
        return "while(" + condition + "){" + statements + "}";
    }
}
