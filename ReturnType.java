public class ReturnType {
    public enum type{NONE, BREAK, CONTINUE, RETURN}
    private type returnType;
    private String value;

    public ReturnType(type returnType){
        this.returnType = returnType;
    }
    public ReturnType(type returnType, String value){
        this(returnType);
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public type getReturnType() {
        return returnType;
    }
}
