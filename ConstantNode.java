public class ConstantNode extends StatementNode{
    private String value;

    public ConstantNode(String value){
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString(){
        return "" + value;
    }
}
