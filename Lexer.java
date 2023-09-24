import java.util.HashMap;
import java.util.LinkedList;

public class Lexer {
    private int lineNumber = 1;
    private int position = 0;
    private StringHandler awkFile;
    private LinkedList<Token> tokens = new LinkedList<Token>();
    private HashMap<String,Token.tokenType> keyWords = new HashMap<String,Token.tokenType>();
    private HashMap<String,Token.tokenType> doubleOperators = new HashMap<String,Token.tokenType>();
    private HashMap<String,Token.tokenType> singleOperators = new HashMap<String,Token.tokenType>();
    public Lexer(){}
    public Lexer(String file){}
    public LinkedList<Token> Lex(String file) throws Exception {
        awkFile = new StringHandler(file);
        //adds values to hashmap
        fillHashmaps();
        //While loop that the lexer's operations happen inside of.
        while(awkFile.IsDone()==false){
            char next = awkFile.Peek(0);
            //this if else if statement is the core logic of the program and determines how to handle incoming characters.
            if(next == ' '){
                awkFile.Swallow(1);
                position++;
            } else if (next == '\n') {
                    tokens.add(new Token(Token.tokenType.SEPARATOR, lineNumber, position));
                    awkFile.Swallow(1);
                    lineNumber++;
                    position = 0;
            }else if (next == '\r') {
                    awkFile.Swallow(1);
                    position++;
            }else if(next=='#') {
                while(next != '\n' && !awkFile.IsDone()){
                    next= awkFile.Peek(0);
                    awkFile.Swallow(1);
                }
                if(next == '\n' && !awkFile.IsDone()){
                    lineNumber++;
                    position = 0;
                }
            }else if(next=='"'){
                awkFile.Swallow(1);
                position++;
                HandleStringLiteral();
            }else if(Character.isLetter(next)){
                ProcessWord();
            }else if(Character.isDigit(next) || next=='.'){
                ProcessNumber();
            }else if(next=='`') {
                awkFile.Swallow(1);
                position++;
                HandlePattern();
            }else{
                if(ProcessSymbol()){
                    //this will throw an error that will be caught in main and be printed out indication the origin of the error
                    throw new IllegalArgumentException("Unrecognized Character "+ awkFile.Peek(0) +" at linenumber "+ lineNumber + "/position " + position);
                }
            }
        }
        return tokens;
    }
    public void ProcessWord(){
        fillHashmaps();
        String current = "";
        int startPosition = position;
        //this while loop splices together the recognized incoming characters to form a string.
        while(awkFile.Remainder().length()>=1){
            //this if statement stops the loop if any characters other than letters and numbers are encountered.
            if(Character.isLetterOrDigit(awkFile.Peek(0)) || awkFile.Peek(0)=='_') {
                char c = awkFile.GetChar();
                position++;
                current += c;
            }else{
                break;
            }
        }
        if(keyWords.containsKey(current)){
            Token word = new Token(keyWords.get(current), lineNumber, startPosition);
            tokens.add(word);
        }else {
            Token word = new Token(Token.tokenType.WORD, lineNumber, startPosition);
            word.setValue(current);
            tokens.add(word);
        }
    }
    public void ProcessNumber(){
        boolean foundPoint = false;
        String current = "";
        int startPosition = position;
        //this while loop splices together the recognized incoming characters to form a string.
        while(!awkFile.IsDone()){
            //this if statement stops the loop if any characters other than numbers are encountered.
            if(Character.isDigit(awkFile.Peek(0)) || awkFile.Peek(0)=='.') {
                char c = awkFile.GetChar();
                position++;
                current += c;
                //this if statement will throw an error if there is an occurrence of more than one decimal point in a number.
                if (c == '.' && foundPoint == true) {
                    throw new NumberFormatException("The number at linenumber " + lineNumber + "/position " + startPosition + " contains more than one decimal.");
                } else if (c == '.') {
                    foundPoint = true;
                }
            }else{
                break;
            }
        }
        Token digit = new Token(Token.tokenType.NUMBER, lineNumber, startPosition);
        digit.setValue(current);
        tokens.add(digit);
    }
    public boolean ProcessSymbol(){
        int startPosition = position;
        if(!awkFile.IsDone()){
             String dSymbol = "";
             //This prevents PeekString from going OOB
             if(awkFile.Remainder().length()>1) {
                 dSymbol = awkFile.PeekString(2);
             }
             String sSymbol = awkFile.PeekString(1);
            //This if else if block first checks for \n and then for double character operators
            //followed by single character operators.
             if(sSymbol.compareTo("\\")==0){
                 if(!awkFile.IsDone()) {
                     if (awkFile.Peek(1) == 'n') {
                         awkFile.Swallow(2);
                         Token separator = new Token(Token.tokenType.SEPARATOR, lineNumber, startPosition);
                         tokens.add(separator);
                         return false;
                     }
                     if (awkFile.Peek(1) == 'r') {
                         awkFile.Swallow(2);
                         return false;
                     }
                 }
             }else if(doubleOperators.containsKey(dSymbol)){
                awkFile.Swallow(2);
                position+=2;
                Token doubleSymbol = new Token(doubleOperators.get(dSymbol), lineNumber, startPosition);
                tokens.add(doubleSymbol);
                return false;
            }else if(singleOperators.containsKey(sSymbol)) {
                awkFile.Swallow(1);
                position++;
                Token singleSymbol = new Token(singleOperators.get(sSymbol), lineNumber, startPosition);
                tokens.add(singleSymbol);
                return false;
            }
        }
     return true;
    }
    public void HandleStringLiteral() throws Exception {
        int startPosition = position-1;
        String current = "";
        if(!awkFile.IsDone()) {
            //this while loop assembles the String Literal.
            while(!awkFile.IsDone()) {
                //This if statement checks for escaped " and treats them accordingly.
                if(awkFile.Peek(0) == '\\'){
                    if(!awkFile.IsDone()) {
                        if(awkFile.Peek(1)=='"') {
                            awkFile.Swallow(2);
                            position+=2;
                            current += '"';
                        }
                    }
                }
                if(awkFile.Peek(0) != '"') {
                    char c = awkFile.GetChar();
                    position++;
                    current += c;
                }else{
                    break;
                }
            }
            //this if else if block will throw an exception if another " is not encountered by the end of the file.
            //however if a " is encountered the token will be made and added to the linked list.
            if(awkFile.IsDone()){
                throw new Exception("Syntax error, no closing quote at line number " + lineNumber + "/position " + startPosition);
            } else if (awkFile.Peek(0) == '"') {
                awkFile.Swallow(1);
                Token stringLiteral = new Token(Token.tokenType.STRINGLITERAL, lineNumber, startPosition);
                stringLiteral.setValue(current);
                tokens.add(stringLiteral);
            }
        }
    }
    public void HandlePattern() throws Exception{
        int startPosition = position-1;
        String current = "";
        if(!awkFile.IsDone()) {
            //this while loop assembles the regular expression.
            while(!awkFile.IsDone()) {
                if(awkFile.Peek(0) != '`') {
                    char c = awkFile.GetChar();
                    position++;
                    current += c;
                }else{
                    break;
                }
            }
            //this if else if block will throw an exception if another ` is not encountered by the end of the file.
            //however if a ` is encountered the token will be made and added to the linked list.
            if(awkFile.IsDone()){
                throw new Exception("Syntax error, no closing ` at line number " + lineNumber + "/position " + startPosition);
            } else if (awkFile.Peek(0) == '`') {
                awkFile.Swallow(1);
                Token regularExpression = new Token(Token.tokenType.REGULAREXPRESSION, lineNumber, startPosition);
                regularExpression.setValue(current);
                tokens.add(regularExpression);
            }
        }
    }
    private void fillHashmaps(){
        //fills the keyword hashmap
        keyWords.put("while", Token.tokenType.WHILE );
        keyWords.put("if",Token.tokenType.IF);
        keyWords.put("do", Token.tokenType.DO );
        keyWords.put("for", Token.tokenType.FOR );
        keyWords.put("break", Token.tokenType.BREAK );
        keyWords.put("continue", Token.tokenType.CONTINUE );
        keyWords.put("else", Token.tokenType.ELSE );
        keyWords.put("return", Token.tokenType.RETURN );
        keyWords.put("BEGIN", Token.tokenType.BEGIN );
        keyWords.put("END", Token.tokenType.END);
        keyWords.put("print", Token.tokenType.PRINT);
        keyWords.put("printf", Token.tokenType.PRINTF );
        keyWords.put("next", Token.tokenType.NEXT );
        keyWords.put("in", Token.tokenType.IN );
        keyWords.put("delete", Token.tokenType.DELETE );
        keyWords.put("getline", Token.tokenType.GETLINE );
        keyWords.put("nextfile", Token.tokenType.NEXTFILE );
        keyWords.put("function", Token.tokenType.FUNCTION );
        keyWords.put("exit", Token.tokenType.EXIT );

        //fills the doubleOperators hashmap
        doubleOperators.put(">=", Token.tokenType.GREATERorEQUALS);
        doubleOperators.put("++", Token.tokenType.INCREMENTUP);
        doubleOperators.put("--", Token.tokenType.INCREMENTDOWN);
        doubleOperators.put("<=", Token.tokenType.LESSorEQUALS);
        doubleOperators.put("==", Token.tokenType.COMPARETO);
        doubleOperators.put("^=", Token.tokenType.EXPONENTIALEQUAL);
        doubleOperators.put("!=", Token.tokenType.DOESNOTEQUAL);
        doubleOperators.put("%=", Token.tokenType.MODULOEQUAL);
        doubleOperators.put("*=", Token.tokenType.TIMESEQUAL);
        doubleOperators.put("/=", Token.tokenType.DIVIDEEQUAL);
        doubleOperators.put("+=", Token.tokenType.PLUSEQUAL);
        doubleOperators.put("-=", Token.tokenType.MINUSEQUAL);
        doubleOperators.put("!~", Token.tokenType.DOESNOTMATCH);
        doubleOperators.put("&&", Token.tokenType.AND);
        doubleOperators.put(">>", Token.tokenType.APPEND);
        doubleOperators.put("||", Token.tokenType.OR);

        //fills the singleOperators hashmap
        singleOperators.put("}",Token.tokenType.RIGHTBRACE);
        singleOperators.put("{",Token.tokenType.LEFTBRACE);
        singleOperators.put("]",Token.tokenType.RIGHTBRACKET);
        singleOperators.put("[",Token.tokenType.LEFTBRACKET);
        singleOperators.put(")",Token.tokenType.RIGHTPARENTHESIS);
        singleOperators.put("(",Token.tokenType.LEFTPARENTHESIS);
        singleOperators.put("$",Token.tokenType.DOLLARSIGN);
        singleOperators.put("~",Token.tokenType.TILDA);
        singleOperators.put("=",Token.tokenType.EQUALS);
        singleOperators.put("<",Token.tokenType.LESSTHAN);
        singleOperators.put(">",Token.tokenType.GREATERTHAN);
        singleOperators.put("!",Token.tokenType.NOT);
        singleOperators.put("+",Token.tokenType.PLUS);
        singleOperators.put("^",Token.tokenType.EXPONENTIAL);
        singleOperators.put("-",Token.tokenType.MINUS);
        singleOperators.put("?",Token.tokenType.QUESTIONMARK);
        singleOperators.put(":",Token.tokenType.COLON);
        singleOperators.put("*",Token.tokenType.TIMES);
        singleOperators.put("/",Token.tokenType.DIVIDE);
        singleOperators.put("%",Token.tokenType.MODULO);
        singleOperators.put(";",Token.tokenType.SEPARATOR);
        singleOperators.put("\n",Token.tokenType.SEPARATOR);
        singleOperators.put("|",Token.tokenType.PIPE);
        singleOperators.put(",",Token.tokenType.COMMA);
    }
}
