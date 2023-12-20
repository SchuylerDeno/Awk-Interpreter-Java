import java.util.HashMap;

public class InterpreterArrayDataType extends InterpreterDataType{
    private HashMap<String, InterpreterDataType> variableArray;

    public InterpreterArrayDataType(HashMap<String, InterpreterDataType> variableArray){
        this.variableArray = variableArray;
    }

    public HashMap<String, InterpreterDataType> getVariableArray() {
        return variableArray;
    }

    public void setVariableArray(HashMap<String, InterpreterDataType> variableArray) {
        this.variableArray = variableArray;
    }
}
