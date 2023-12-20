import java.util.LinkedList;
import java.util.Optional;

public class ForEachNode extends StatementNode{
    private Node forEach;
    private LinkedList<StatementNode> statements = new LinkedList<>();

    public ForEachNode(Node forEach, LinkedList<StatementNode> statements){
        this.forEach = forEach;
        this.statements = statements;
    }

    public LinkedList<StatementNode> getStatements() {
        return statements;
    }

    public Node getForEach() {
        return forEach;
    }

    @Override
    public String toString() {
        return "for(" + forEach + "){" + statements +"}";
    }
}
