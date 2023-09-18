public class Token {
    //i'm sorry
    public enum tokenType {
        WORD, NUMBER, SEPARATOR, STRINGLITERAL, REGULAREXPRESSION, WHILE, IF, DO, FOR, BREAK, CONTINUE, ELSE, RETURN, BEGIN, END, PRINT, PRINTF, NEXT, IN,
        DELETE, GETLINE, EXIT, NEXTFILE, FUNCTION, GREATERorEQUALS, INCREMENTUP, INCREMENTDOWN, LESSorEQUALS, COMPARETO,
        DOESNOTEQUAL, EXPONENTIALEQUAL, MODULOEQUAL, TIMESEQUAL, DIVIDEEQUAL, PLUSEQUAL, MINUSEQUAL, DOESNOTMATCH,
        AND, APPEND, OR, RIGHTPARENTHESIS, LEFTPARENTHESIS, RIGHTBRACKET, LEFTBRACKET, RIGHTBRACE, LEFTBRACE, DOLLARSIGN,
        TILDA, EQUALS, LESSTHAN, GREATERTHAN, NOT, PLUS, EXPONENTIAL, MINUS, QUESTIONMARK, COLON, TIMES, DIVIDE, MODULO, COMMA, PIPE
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
    public String toString(){
        //if statement to avoid unnecessary characters
        if(value != null) {
            return type + "(" + value + ") ";
        }else{
            return type+" ";
        }
    }
}
