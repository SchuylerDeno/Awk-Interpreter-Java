import java.util.HashMap;
import java.util.LinkedList;
import java.util.function.Function;

public class BuiltInFunctionNode extends FunctionNode{
    private boolean variadic;
    private int parameterCount;
    private LinkedList<String> parameterNames;
    private Function<HashMap<String, InterpreterDataType>,String> execute;
    public BuiltInFunctionNode(Function<HashMap<String, InterpreterDataType>,String> execute, boolean variadic, int parameterCount, LinkedList<String> parameterNames){
        this.execute = execute;
        this.variadic = variadic;
        this.parameterCount = parameterCount;
        this.parameterNames = parameterNames;
    }

    public int getParameterCount() {
        return parameterCount;
    }

    @Override
    public LinkedList<String> getParameterNames() {
        return parameterNames;
    }

    public boolean isVariadic() {
        return variadic;
    }

    public String execute(HashMap<String,InterpreterDataType> data) {
        return execute.apply(data);
    }
}
