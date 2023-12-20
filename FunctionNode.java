import java.util.Collection;
import java.util.LinkedList;

public class FunctionNode implements Node {
    private String name;
    private LinkedList<String> parameterNames;
    private LinkedList<StatementNode> statements = new LinkedList<>();
    private int parameterCount;

    public FunctionNode(){}
    public FunctionNode(String name, LinkedList parameterNames){
        this.name = name;
        this.parameterNames = parameterNames;
        parameterCount = parameterNames.size();
    }
    public void addStatements(Collection statements){
        this.statements.addAll(statements);
    }
    public String getName() {
        return name;
    }

    public int getParameterCount() {
        return parameterCount;
    }

    public LinkedList<StatementNode> getStatements() {
        return statements;
    }

    public LinkedList<String> getParameterNames() {
        return parameterNames;
    }

    @Override
    public String toString() {
        String retString = name + "(" + parameterNames + ")" + "{";
        for(StatementNode o: statements){
                       retString += o.toString() + " ";
                  }
        retString += "}";
        return retString;
    }
}
