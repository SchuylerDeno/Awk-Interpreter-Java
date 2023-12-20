import java.util.Optional;

public class OperationNode implements Node {

    public enum Operations{EQ, NE, LT, LE, GT, GE, AND, OR, NOT, MATCH, NOTMATCH, DOLLAR,
        PREINC,POSTINC,PREDEC, POSTDEC,UNARYPOS, UNARYNEG, IN,
        EXPONENT, ADD, SUBTRACT,MULTIPLY, DIVIDE,MODULO, CONCATENATION
    }

    private Node left;

    private Operations operation;

    private Optional<Node> right = Optional.empty();

    public OperationNode(Node left, Operations operation){
        this.left = left;
        this.operation = operation;
    }
    public OperationNode(Node left, Operations operation, Optional<Node> right) {
        this.left = left;
        this.right = right;
        this.operation = operation;
    }

    public Optional<Node> getRight() {
        return right;
    }

    public Node getLeft() {
        return left;
    }

    public Operations getOperation() {
        return operation;
    }

    @Override
    public String toString(){
        if(right.isEmpty()) {
            return left.toString() + " " + operation;
        }else
            return "(" + left.toString() + " " + operation + " " + right.get() + ")";
    }
}
