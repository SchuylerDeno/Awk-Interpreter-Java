public class AssignmentNode extends StatementNode{
    private Node target;

    private Node expression;

    public AssignmentNode(Node target, Node expression){
        this.target = target;
        this.expression = expression;
    }

    public Node getExpression() {
        return expression;
    }

    public Node getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return target + " = " + expression;
    }
}
