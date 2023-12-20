import java.util.LinkedList;

public class FunctionCallNode extends StatementNode{
    private LinkedList<Node> parameters;
    private String functionName;
    private int parameterCount;

    public FunctionCallNode(String functionName){
        this.functionName = functionName;
        parameterCount = 0;
    }

    public FunctionCallNode(String functionName, LinkedList<Node> arguments){
        this(functionName);
        this.parameters = arguments;
        parameterCount = arguments.size();
    }

    public int getParameterCount() {
        return parameterCount;
    }

    public LinkedList<Node> getParameters() {
        return parameters;
    }

    public String getFunctionName() {
        return functionName;
    }

    @Override
    public String toString() {
        return functionName + "(" + parameters + ")";
    }
}
