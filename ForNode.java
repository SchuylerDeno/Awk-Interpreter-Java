import java.util.LinkedList;
import java.util.Optional;

public class ForNode extends StatementNode{
    private Optional<Node> left;
    private Optional<Node> middle;
    private Optional<Node> right;
    private LinkedList<StatementNode> statements = new LinkedList<>();

    public ForNode(Optional<Node> left,Optional<Node> middle,Optional<Node> right,LinkedList<StatementNode> statements){
        this.left = left;
        this.middle = middle;
        this.right = right;
        this.statements = statements;
    }

    public Optional<Node> getLeft() {
        return left;
    }

    public Optional<Node> getMiddle() {
        return middle;
    }

    public Optional<Node> getRight() {
        return right;
    }

    public LinkedList<StatementNode> getStatements() {
        return statements;
    }

    @Override
    public String toString() {
        return "for(" + left + ";" + middle + ";" + right + "){" + statements + "}";
    }
}
