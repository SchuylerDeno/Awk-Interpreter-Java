public class Token {
    //i'm sorry
    public enum tokenType {
        WORD, NUMBER, SEPARATOR, STRINGLITERAL, PATTERN, WHILE, IF, DO, FOR, BREAK, CONTINUE, ELSE, RETURN, BEGIN, END, PRINT, PRINTF, NEXT, IN,
        DELETE, GETLINE, EXIT, NEXTFILE, FUNCTION, GE, INCREMENT, DECREMENT, LE, COMPARETO,
        NOTEQUAL, EXPONENTIALEQUAL, MODULOEQUAL, TIMESEQUAL, DIVIDEEQUAL, PLUSEQUAL, MINUSEQUAL, NOTMATCH,
        AND, APPEND, OR, RIGHTPARENTHESIS, LEFTPARENTHESIS, RIGHTBRACKET, LEFTBRACKET, RIGHTBRACE, LEFTBRACE, DOLLAR,
        MATCH, EQUALS, LT, GT, NOT, PLUS, EXPONENTIAL, SUBTRACT, QUESTIONMARK, COLON, MULTIPLY, DIVIDE, MODULO, COMMA, PIPE
    }
    //>=  ++  --  <=  ==  !=  ^=  %=  *=  /=  +=  -=  !~   &&   >>   || { } [ ] ( ) $ ~ = < > !  + ^ - ?  : * / % ; \n | ,
    private String value = null;
    private tokenType type;
    private int lineNumber;
    private int position;
    public Token(tokenType type, int lineNumber, int position){
        this.type = type;
        this.lineNumber = lineNumber;
        this.position = position;
    }
    public Token(String value,tokenType type, int lineNumber, int position){
        this(type,lineNumber,position);
        this.value = value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public tokenType getType(){
        return type;
    }

    public String getValue(){
        return value;
    }

    public int getLineNumber(){
        return lineNumber;
    }

    public int getPosition(){
        return position;
    }

    public String toString(){
        //if statement to avoid unnecessary characters
        if(value != null) {
            return type + "(" + value + ") ";
        }else{
            return type+" ";
        }
    }
}
