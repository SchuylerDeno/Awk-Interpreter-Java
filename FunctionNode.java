import java.util.Collection;
import java.util.LinkedList;

public class FunctionNode implements Node {
    private String name;
    private LinkedList<String> parameterNames;
    private LinkedList<StatementNode> statements = new LinkedList<>();

    public FunctionNode(String name, LinkedList parameterNames){

    }
    public void addStatements(Collection statements){
        this.statements.addAll(statements);
    }
    public String getName() {
        return name;
    }

    public LinkedList<StatementNode> getStatements() {
        return statements;
    }

    public LinkedList<String> getParameterNames() {
        return parameterNames;
    }

    @Override
    public String toString() {
        String retString = name + "(" + parameterNames + ")" + "{\n";
        for(StatementNode o: statements){
                       retString += o.toString() + "\n";
                  }
        retString += "}";
        return retString;
    }
}
