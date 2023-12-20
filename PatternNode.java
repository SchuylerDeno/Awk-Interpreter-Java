public class PatternNode extends StatementNode{
    private String value;

    public PatternNode(String value){
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString(){
        return "Pattern " + value;
    }
}
