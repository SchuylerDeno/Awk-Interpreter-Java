public class TernaryNode implements Node{
    private Node condition;

    private Node consequent;

    private Node alternate;

    TernaryNode(Node condition, Node consequent, Node alternate){
        this.consequent=consequent;
        this.condition = condition;
        this.alternate = alternate;
    }

    public Node getCondition() {
        return condition;
    }

    public Node getAlternate() {
        return alternate;
    }

    public Node getConsequent() {
        return consequent;
    }

    @Override
    public String toString() {
        return condition + " ? " + consequent + " : " + alternate;
    }
}
