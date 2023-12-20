import java.util.LinkedList;
import java.util.Optional;

public class IfNode extends StatementNode{

    private Optional<Node> condition;
    private LinkedList<StatementNode> statements;
    private Optional<IfNode> next = Optional.empty();

    public IfNode(Optional<Node> condition, LinkedList<StatementNode> statements, Optional<IfNode> next){
        this.condition = condition;
        this.statements = statements;
        this.next = next;
    }

    public Optional<Node> getCondition() {
        return condition;
    }

    public IfNode getNext() {
        if(next.isPresent()) {
            return next.get();
        }else{
            return null;
        }
    }

    public LinkedList<StatementNode> getStatements() {
        return statements;
    }

    @Override
    public String toString() {
        if(next.isPresent()) {
            return "if(" + condition.get() + "){" + statements + "} else " + next.get();
        }else{
            if(condition.isPresent()) {
                return "if(" + condition.get() + "){" + statements + "}";
            }else{
                return "{" + statements + "}";
            }
        }
    }
}
