import java.util.LinkedList;
import java.util.Optional;

public class Parser {
    private TokenHandler tokens;
    public Parser(LinkedList<Token> tokens){
        this.tokens = new TokenHandler(tokens);
    }
    public ProgramNode Parse() throws Exception {
        ProgramNode program = new ProgramNode();
        //this while loop will run calling ParseFunction and ParseAction until there are no more tokens or an exception is thrown.
        while(tokens.MoreTokens()){
            if(ParseFunction(program) || ParseAction(program)){
            }else{
                //if neither return true that means there is an error in the syntax so an error is thrown.
                throw new Exception("Error in parse, unrecognized syntax at " + tokens.Peek(0));
            }
        }
        return program;
    }

    private boolean ParseFunction(ProgramNode node) throws Exception {
        if(tokens.MoreTokens()){
            AcceptSeparators();
            String name = "";
            LinkedList<String> parameters = new LinkedList<>();
            //this if statement determines if there is a function for the parser to parse.
            if(tokens.MatchAndRemove(Token.tokenType.FUNCTION).equals(Optional.empty())){
                return false;
            }
            if(tokens.Peek(0).get().getType().equals(Token.tokenType.WORD)) {
                name = tokens.MatchAndRemove(Token.tokenType.WORD).get().getValue();
            }else
                throw new Exception("Function has no name");
            if(tokens.MatchAndRemove(Token.tokenType.LEFTPARENTHESIS).equals(Optional.empty())){
                throw new Exception("Function "+ name + " is missing \"(\"");
            }
            //this while loop adds all the parameters to a list, if there are any.
            while(tokens.MatchAndRemove(Token.tokenType.RIGHTPARENTHESIS).equals(Optional.empty())){
                if(tokens.MoreTokens()){
                    AcceptSeparators();
                    Optional<Token> parameter = tokens.MatchAndRemove(Token.tokenType.WORD);
                    if(!parameter.equals(Optional.empty())){
                        parameters.add(parameter.get().getValue());
                        AcceptSeparators();
                        if(!tokens.MatchAndRemove(Token.tokenType.COMMA).equals(Optional.empty())){
                            AcceptSeparators();
                        }else{
                            if(!tokens.MatchAndRemove(Token.tokenType.RIGHTPARENTHESIS).equals(Optional.empty())){
                                break;
                            }else{
                                throw new Exception("Syntax Error at Function "+ name);
                            }
                        }
                    }
                }else{
                    throw new Exception("Function "+ name + " is missing \")\"");
                }
            }
            //now we create a new FunctionNode and add it to the ProgramNode.
            FunctionNode newFunction = new FunctionNode(name, parameters);
            newFunction.addStatements(parseBlock().getStatements());
            node.addFunctionNode(newFunction);
            return true;
        }else{
            return false;
        }
    }

    private boolean ParseAction(ProgramNode node) throws Exception {
        if(tokens.MoreTokens()) {
            AcceptSeparators();
            //this if else if block determines if it is a BEGIN, END, or regular block and adds it to the proper list in the ProgramNode.
            if (!tokens.MatchAndRemove(Token.tokenType.BEGIN).equals(Optional.empty())) {
                AcceptSeparators();
                node.addBeginBlock(parseBlock());
                return true;
            } else if (!tokens.MatchAndRemove(Token.tokenType.END).equals(Optional.empty())) {
                AcceptSeparators();
                node.addEndBlock(parseBlock());
                return true;
            } else {
                Optional<Node> condition = ParseOperation();
                BlockNode block = parseBlock();
                block.setCondition(condition);
                node.addBlock(block);
                return true;
            }
        }else{
            return false;
        }
    }

    public BlockNode parseBlock() throws Exception {
        BlockNode newBlockNode;
        LinkedList<StatementNode> statments = new LinkedList<>();
        if(tokens.MatchAndRemove(Token.tokenType.LEFTBRACE).isPresent()){
            //do while loop handles statements inside the BlockNode.
            do{
                AcceptSeparators();
                Optional<StatementNode> newStatement = parseStatement();
                if(newStatement.isEmpty()){
                    break; //No more statements so we exit the loop
                }else{
                    statments.add(newStatement.get());
                }
            }while(true);
            if(tokens.MatchAndRemove(Token.tokenType.RIGHTBRACE).isEmpty()){
                throw new Exception("block missing right brace");
            }
            newBlockNode = new BlockNode(statments);
        }else{
            throw new Exception("block missing left brace");
        }
        return newBlockNode;
    }

    public Optional<StatementNode> parseStatement() throws Exception {
        AcceptSeparators();
        //this brick of if else if statements determines what kind of statement is next and handles them accordingly
        if(tokens.MatchAndRemove(Token.tokenType.CONTINUE).isPresent()){
            AcceptSeparators();
            return Optional.of(new ContinueNode());
        }else if(tokens.MatchAndRemove(Token.tokenType.BREAK).isPresent()){
            AcceptSeparators();
            return Optional.of(new BreakNode());
        }else if(tokens.MatchAndRemove(Token.tokenType.IF).isPresent()){
            return Optional.of((StatementNode)parseIf().get());
        }else if(tokens.MatchAndRemove(Token.tokenType.FOR).isPresent()){
            return parseFor();
        }else if(tokens.MatchAndRemove(Token.tokenType.DELETE).isPresent()){
            return parseDelete();
        }else if(tokens.MatchAndRemove(Token.tokenType.WHILE).isPresent()){
            return parseWhile();
        }else if(tokens.MatchAndRemove(Token.tokenType.DO).isPresent()){
            return parseDoWhile();
        }else if(tokens.MatchAndRemove(Token.tokenType.RETURN).isPresent()){
            return parseReturn();
        }else{
            //handles AssignmentNodes, PostDec, PreDec, PostInc, PreInc, and FunctionCallNode.
            Optional<Node> value = ParseOperation();
            if(value.isPresent()){
                if(value.get() instanceof AssignmentNode) {
                    return Optional.of((AssignmentNode) value.get());
                }else if(value.get() instanceof OperationNode){
                    if(((OperationNode) value.get()).getOperation().equals(OperationNode.Operations.POSTINC)){
                        return Optional.of(new AssignmentNode(((OperationNode) value.get()).getLeft(), value.get()));
                    }else if(((OperationNode) value.get()).getOperation().equals(OperationNode.Operations.PREINC)){
                        return Optional.of(new AssignmentNode(((OperationNode) value.get()).getLeft(), value.get()));
                    }else if(((OperationNode) value.get()).getOperation().equals(OperationNode.Operations.POSTDEC)){
                        return Optional.of(new AssignmentNode(((OperationNode) value.get()).getLeft(), value.get()));
                    }else if(((OperationNode) value.get()).getOperation().equals(OperationNode.Operations.PREDEC)){
                        return Optional.of(new AssignmentNode(((OperationNode) value.get()).getLeft(), value.get()));
                    }else{
                        throw new Exception("non valid statement");
                    }
                }else if(value.get() instanceof FunctionCallNode){
                    return Optional.of((FunctionCallNode)value.get());
                }
            }
            return Optional.empty();
        }
    }

    private Optional<StatementNode> parseReturn() throws Exception {
        Optional<Node> value = ParseOperation();
        //handles return statements
        if(value.isPresent()){
            AcceptSeparators();
            return Optional.of(new ReturnNode(value.get()));
        }else{
            throw new Exception("return has no value to return");
        }
    }

    private Optional<StatementNode> parseDoWhile() throws Exception {
        if(tokens.MatchAndRemove(Token.tokenType.LEFTBRACE).isPresent()) {
            //handles do while loops
            LinkedList<StatementNode> statements = new LinkedList<>();
            do {
                AcceptSeparators();
                Optional<StatementNode> newStatement = parseStatement();
                if (newStatement.isEmpty()) {
                    break;
                } else {
                    statements.add(newStatement.get());
                }
            } while (true);
            if(tokens.MatchAndRemove(Token.tokenType.RIGHTBRACE).isPresent()){
                if(tokens.MatchAndRemove(Token.tokenType.WHILE).isPresent()) {
                    Optional<Node> condition = ParseOperation();
                    if (condition.isPresent()) {
                        AcceptSeparators();
                        return Optional.of(new DoWhileNode(condition.get(), statements));
                    } else {
                        throw new Exception("Do While loop has no condition");
                    }
                }else {
                    throw new Exception("Do While loop has no while");
                }
            }else{
                throw new Exception("Do While loop has no right brace");
            }
        }else {
            throw new Exception("Do While loop has no left brace");
        }
    }

    private Optional<StatementNode> parseWhile() throws Exception {
        Optional<Node> condition = ParseOperation();
        //handles while loops
        if(condition.isPresent()){
            if(tokens.MatchAndRemove(Token.tokenType.LEFTBRACE).isPresent()) {
                LinkedList<StatementNode> statements = new LinkedList<>();
                do {
                    AcceptSeparators();
                    Optional<StatementNode> newStatement = parseStatement();
                    if (newStatement.isEmpty()) {
                        break;
                    } else {
                        statements.add(newStatement.get());
                    }
                } while (true);
                if(tokens.MatchAndRemove(Token.tokenType.RIGHTBRACE).isPresent()){
                    AcceptSeparators();
                    return Optional.of(new WhileNode(condition.get(), statements));
                }else{
                    throw new Exception("While loop has no right brace");
                }
            }else{
                throw new Exception("While loop has no left brace");
            }
        }else {
            throw new Exception("While loop has no condition");
        }
    }

    private Optional<StatementNode> parseDelete() throws Exception {
        Optional<Token> arrayName = tokens.MatchAndRemove(Token.tokenType.WORD);
        //handles delete statements
        if(arrayName.isPresent()){
            VariableReferenceNode array = new VariableReferenceNode(arrayName.get().getValue());
            if(tokens.MatchAndRemove(Token.tokenType.LEFTBRACKET).isPresent()){
                LinkedList<ConstantNode> indexs = new LinkedList<>();
                do{
                    Optional<Node> index = ParseOperation();
                    if(index.isPresent()){
                        if(index.get() instanceof ConstantNode){ //indexs must be a number, which are stored in ConstantNodes.
                            indexs.add((ConstantNode) index.get());
                        }else{
                            throw new Exception("improper index at line " + arrayName.get().getLineNumber() + " position " + arrayName.get().getPosition());
                        }
                    }else{
                        throw new Exception("no index(es) or no index after comma at line " + arrayName.get().getLineNumber() + " position " + arrayName.get().getPosition());
                    }
                }while(tokens.MatchAndRemove(Token.tokenType.COMMA).isPresent());
                if(tokens.MatchAndRemove(Token.tokenType.RIGHTBRACKET).isPresent()){
                    return Optional.of(new DeleteNode(array, indexs));
                }else{
                    throw new Exception("no closing bracket at line " + arrayName.get().getLineNumber() + " position " + arrayName.get().getPosition());
                }

            }else{
                return Optional.of(new DeleteNode(array));
            }

        }else{
            throw new Exception("no array given to delete");
        }
    }

    private Optional<StatementNode> parseFor() throws Exception {
        if(tokens.MatchAndRemove(Token.tokenType.LEFTPARENTHESIS).isPresent()){
            int i = 0;
            boolean inPresent = false;
            //we crawl throw the for loops parameters and look for an IN token,
            //if there's an IN token it's safe to assume we're dealing with a forEach loop.
            while(!tokens.Peek(i).get().getType().equals(Token.tokenType.RIGHTPARENTHESIS)){
                if(tokens.Peek(i).isEmpty()){
                    throw new Exception("for loop is missing right parenthesis");
                }
                if(tokens.Peek(i).get().getType().equals(Token.tokenType.IN)){
                    inPresent = true;
                }
                i++;
            }
            //if the IN token was present we treat it like a forEach loop, else we treat it as a normal for loop.
            if(inPresent){
                Optional<Node> forEach = ParseOperation();
                if(forEach.isPresent()){
                    if(tokens.MatchAndRemove(Token.tokenType.RIGHTPARENTHESIS).isPresent()){
                        if(tokens.MatchAndRemove(Token.tokenType.LEFTBRACE).isPresent()) {
                            LinkedList<StatementNode> statements = new LinkedList<>();
                            do {
                                AcceptSeparators();
                                Optional<StatementNode> newStatement = parseStatement();
                                if (newStatement.isEmpty()) {
                                    break;
                                } else {
                                    statements.add(newStatement.get());
                                }
                            } while (true);
                            if(tokens.MatchAndRemove(Token.tokenType.RIGHTBRACE).isPresent()){
                                return Optional.of(new ForEachNode(forEach.get(), statements));
                            }else{
                                throw new Exception("for loop missing right brace");
                            }
                        }else{
                            throw new Exception("for loop missing left brace");
                        }
                    }else{
                        throw new Exception("for loop missing right parenthesis");
                    }
                }else{
                    throw new Exception();
                }
            }else{
                Optional<Node> left = ParseOperation();
                if(tokens.MatchAndRemove(Token.tokenType.SEPARATOR).isEmpty()){
                    throw new Exception("for loop missing ;");
                }
                AcceptSeparators();
                Optional<Node> middle = ParseOperation();
                if(tokens.MatchAndRemove(Token.tokenType.SEPARATOR).isEmpty()){
                    throw new Exception("for loop missing ;");
                }
                AcceptSeparators();
                Optional<Node> right = ParseOperation();
                if(tokens.MatchAndRemove(Token.tokenType.RIGHTPARENTHESIS).isPresent()){
                    if(tokens.MatchAndRemove(Token.tokenType.LEFTBRACE).isPresent()){
                        LinkedList<StatementNode> statements = new LinkedList<>();
                        do {
                            AcceptSeparators();
                            Optional<StatementNode> newStatement = parseStatement();
                            if (newStatement.isEmpty()) {
                                break;
                            } else {
                                statements.add(newStatement.get());
                            }
                        } while (true);
                        if(tokens.MatchAndRemove(Token.tokenType.RIGHTBRACE).isPresent()){
                            return Optional.of(new ForNode(left, middle, right, statements));
                        }else{
                            throw new Exception("for loop missing right brace");
                        }
                    }else{
                        throw new Exception("for loop missing left brace");
                    }
                }else{
                    throw new Exception("for loop missing right parenthesis");
                }
            }
        }else{
            throw new Exception("for loop missing left parenthesis");
        }
    }

    private Optional<IfNode> parseIf() throws Exception {
        Optional<Node> condition = ParseOperation();
        if(condition.isPresent()) {
            if (tokens.MatchAndRemove(Token.tokenType.LEFTBRACE).isPresent()) {
                LinkedList<StatementNode> statements = new LinkedList<>();
                do {
                    AcceptSeparators();
                    Optional<StatementNode> newStatement = parseStatement();
                    if (newStatement.isEmpty()) {
                        break;
                    } else {
                        statements.add(newStatement.get());
                    }
                } while (true);
                if (tokens.MatchAndRemove(Token.tokenType.RIGHTBRACE).isPresent()) {
                    AcceptSeparators();
                    //if there's an else we make a recursive call to parseIf, if not than we've reached the end of the if else if statement.
                    if (tokens.MatchAndRemove(Token.tokenType.ELSE).isPresent()) {
                        tokens.MatchAndRemove(Token.tokenType.IF);
                        return Optional.of(new IfNode(condition, statements, parseIf()));
                    } else {
                        return Optional.of(new IfNode(condition, statements, Optional.empty()));
                    }
                } else {
                    throw new Exception("if statement has no right brace");
                }
            } else {
                throw new Exception("if statement has no left brace");
            }
        }else{
            //handles else{}
            if (tokens.MatchAndRemove(Token.tokenType.LEFTBRACE).isPresent()) {
                LinkedList<StatementNode> statements = new LinkedList<>();
                do {
                    AcceptSeparators();
                    Optional<StatementNode> newStatement = parseStatement();
                    if (newStatement.isEmpty()) {
                        break;
                    } else {
                        statements.add(newStatement.get());
                    }
                } while (true);
                if (tokens.MatchAndRemove(Token.tokenType.RIGHTBRACE).isPresent()) {
                    AcceptSeparators();
                    return Optional.of(new IfNode(Optional.empty(), statements, Optional.empty()));
                }else{
                    throw new Exception("else statement has no right brace");
                }
            }else{
                throw new Exception("else statement has no left brace");
            }
        }
    }

    private boolean AcceptSeparators(){
        //This method is used remove any amount of separators in a row.
        if(tokens.MoreTokens()) {
            boolean found = false;
            while (!tokens.MatchAndRemove(Token.tokenType.SEPARATOR).equals(Optional.empty())) {
                found = true;
            }
            return found;
        }else {
            return false;
        }
    }

    public Optional<Node> ParseOperation() throws Exception {
        return parseAssignment();
    }


    private Optional<Node> parseAssignment() throws Exception {
        Optional<Node> left = parseTernary();
        if(left.isPresent()) {
            //handles assignment operators
            if (left.get() instanceof VariableReferenceNode){//Nothing else makes sense besides a variableReferenceNode as 21+=2 or (a OR b = 2) doesn't make sense.
                if(tokens.MatchAndRemove(Token.tokenType.EQUALS).isPresent()){
                    Optional<Node> right =  parseTernary();
                    if(right.isPresent()){
                        return Optional.of(new AssignmentNode(left.get(), right.get()));
                    }else{
                        if (tokens.MoreTokens()) {
                            Optional<Token> error = tokens.Peek(0);
                            throw new Exception("No right expression before line " + error.get().getLineNumber() + " position " + error.get().getPosition());
                        } else
                            throw new Exception("No right expression last line");
                    }
                }else if(tokens.MatchAndRemove(Token.tokenType.MINUSEQUAL).isPresent()){
                    Optional<Node> right =  parseTernary();
                    if(right.isPresent()){
                        return Optional.of(new AssignmentNode(left.get(), new OperationNode(left.get(), OperationNode.Operations.SUBTRACT, right)));
                    }else{
                        if (tokens.MoreTokens()) {
                            Optional<Token> error = tokens.Peek(0);
                            throw new Exception("No right expression before line " + error.get().getLineNumber() + " position " + error.get().getPosition());
                        } else
                            throw new Exception("No right expression last line");
                    }
                }else if(tokens.MatchAndRemove(Token.tokenType.PLUSEQUAL).isPresent()){
                    Optional<Node> right =  parseTernary();
                    if(right.isPresent()){
                        return Optional.of(new AssignmentNode(left.get(), new OperationNode(left.get(), OperationNode.Operations.ADD, right)));
                    }else{
                        if (tokens.MoreTokens()) {
                            Optional<Token> error = tokens.Peek(0);
                            throw new Exception("No right expression before line " + error.get().getLineNumber() + " position " + error.get().getPosition());
                        } else
                            throw new Exception("No right expression last line");
                    }
                }else if(tokens.MatchAndRemove(Token.tokenType.DIVIDEEQUAL).isPresent()){
                    Optional<Node> right =  parseTernary();
                    if(right.isPresent()){
                        return Optional.of(new AssignmentNode(left.get(), new OperationNode(left.get(), OperationNode.Operations.DIVIDE, right)));
                    }else{
                        if (tokens.MoreTokens()) {
                            Optional<Token> error = tokens.Peek(0);
                            throw new Exception("No right expression before line " + error.get().getLineNumber() + " position " + error.get().getPosition());
                        } else
                            throw new Exception("No right expression last line");
                    }
                }else if(tokens.MatchAndRemove(Token.tokenType.TIMESEQUAL).isPresent()){
                    Optional<Node> right =  parseTernary();
                    if(right.isPresent()){
                        return Optional.of(new AssignmentNode(left.get(), new OperationNode(left.get(), OperationNode.Operations.MULTIPLY, right)));
                    }else{
                        if (tokens.MoreTokens()) {
                            Optional<Token> error = tokens.Peek(0);
                            throw new Exception("No right expression before line " + error.get().getLineNumber() + " position " + error.get().getPosition());
                        } else
                            throw new Exception("No right expression last line");
                    }
                }else if(tokens.MatchAndRemove(Token.tokenType.MODULOEQUAL).isPresent()){
                    Optional<Node> right =  parseTernary();
                    if(right.isPresent()){
                        return Optional.of(new AssignmentNode(left.get(), new OperationNode(left.get(), OperationNode.Operations.MODULO, right)));
                    }else{
                        if (tokens.MoreTokens()) {
                            Optional<Token> error = tokens.Peek(0);
                            throw new Exception("No right expression before line " + error.get().getLineNumber() + " position " + error.get().getPosition());
                        } else
                            throw new Exception("No right expression last line");
                    }
                }else if(tokens.MatchAndRemove(Token.tokenType.EXPONENTIALEQUAL).isPresent()){
                    Optional<Node> right =  parseTernary();
                    if(right.isPresent()){
                        return Optional.of(new AssignmentNode(left.get(), new OperationNode(left.get(), OperationNode.Operations.EXPONENT, right)));
                    }else{
                        if (tokens.MoreTokens()) {
                            Optional<Token> error = tokens.Peek(0);
                            throw new Exception("No right expression before line " + error.get().getLineNumber() + " position " + error.get().getPosition());
                        } else
                            throw new Exception("No right expression last line");
                    }
                }
            }else if(left.get() instanceof OperationNode){
                if(((OperationNode) left.get()).getOperation() == OperationNode.Operations.DOLLAR){
                    if(tokens.MatchAndRemove(Token.tokenType.EQUALS).isPresent()){
                        Optional<Node> right =  parseTernary();
                        if(right.isPresent()){
                            return Optional.of(new AssignmentNode(left.get(), right.get()));
                        }else{
                            if (tokens.MoreTokens()) {
                                Optional<Token> error = tokens.Peek(0);
                                throw new Exception("No right expression before line " + error.get().getLineNumber() + " position " + error.get().getPosition());
                            } else
                                throw new Exception("No right expression last line");
                        }
                    }else if(tokens.MatchAndRemove(Token.tokenType.MINUSEQUAL).isPresent()){
                        Optional<Node> right =  parseTernary();
                        if(right.isPresent()){
                            return Optional.of(new AssignmentNode(left.get(), new OperationNode(left.get(), OperationNode.Operations.SUBTRACT, right)));
                        }else{
                            if (tokens.MoreTokens()) {
                                Optional<Token> error = tokens.Peek(0);
                                throw new Exception("No right expression before line " + error.get().getLineNumber() + " position " + error.get().getPosition());
                            } else
                                throw new Exception("No right expression last line");
                        }
                    }else if(tokens.MatchAndRemove(Token.tokenType.PLUSEQUAL).isPresent()){
                        Optional<Node> right =  parseTernary();
                        if(right.isPresent()){
                            return Optional.of(new AssignmentNode(left.get(), new OperationNode(left.get(), OperationNode.Operations.ADD, right)));
                        }else{
                            if (tokens.MoreTokens()) {
                                Optional<Token> error = tokens.Peek(0);
                                throw new Exception("No right expression before line " + error.get().getLineNumber() + " position " + error.get().getPosition());
                            } else
                                throw new Exception("No right expression last line");
                        }
                    }else if(tokens.MatchAndRemove(Token.tokenType.DIVIDEEQUAL).isPresent()){
                        Optional<Node> right =  parseTernary();
                        if(right.isPresent()){
                            return Optional.of(new AssignmentNode(left.get(), new OperationNode(left.get(), OperationNode.Operations.DIVIDE, right)));
                        }else{
                            if (tokens.MoreTokens()) {
                                Optional<Token> error = tokens.Peek(0);
                                throw new Exception("No right expression before line " + error.get().getLineNumber() + " position " + error.get().getPosition());
                            } else
                                throw new Exception("No right expression last line");
                        }
                    }else if(tokens.MatchAndRemove(Token.tokenType.TIMESEQUAL).isPresent()){
                        Optional<Node> right =  parseTernary();
                        if(right.isPresent()){
                            return Optional.of(new AssignmentNode(left.get(), new OperationNode(left.get(), OperationNode.Operations.MULTIPLY, right)));
                        }else{
                            if (tokens.MoreTokens()) {
                                Optional<Token> error = tokens.Peek(0);
                                throw new Exception("No right expression before line " + error.get().getLineNumber() + " position " + error.get().getPosition());
                            } else
                                throw new Exception("No right expression last line");
                        }
                    }else if(tokens.MatchAndRemove(Token.tokenType.MODULOEQUAL).isPresent()){
                        Optional<Node> right =  parseTernary();
                        if(right.isPresent()){
                            return Optional.of(new AssignmentNode(left.get(), new OperationNode(left.get(), OperationNode.Operations.MODULO, right)));
                        }else{
                            if (tokens.MoreTokens()) {
                                Optional<Token> error = tokens.Peek(0);
                                throw new Exception("No right expression before line " + error.get().getLineNumber() + " position " + error.get().getPosition());
                            } else
                                throw new Exception("No right expression last line");
                        }
                    }else if(tokens.MatchAndRemove(Token.tokenType.EXPONENTIALEQUAL).isPresent()){
                        Optional<Node> right =  parseTernary();
                        if(right.isPresent()){
                            return Optional.of(new AssignmentNode(left.get(), new OperationNode(left.get(), OperationNode.Operations.EXPONENT, right)));
                        }else{
                            if (tokens.MoreTokens()) {
                                Optional<Token> error = tokens.Peek(0);
                                throw new Exception("No right expression before line " + error.get().getLineNumber() + " position " + error.get().getPosition());
                            } else
                                throw new Exception("No right expression last line");
                        }
                    }else{
                        return left;
                    }
                }else{
                    return left;
                }
            }else{
                return left;
            }
        }
        return left;
    }

    private Optional<Node> parseTernary() throws Exception {
        Optional<Node> left = parseOR();
        if(left.isPresent()){
            //Handles ternary operators
            if(tokens.MatchAndRemove(Token.tokenType.QUESTIONMARK).isPresent()){
                Optional<Node> middle = parseAssignment();
                if(middle.isPresent()) {
                    if (tokens.MatchAndRemove(Token.tokenType.COLON).isPresent()) {
                        Optional<Node> right = parseAssignment();
                        if(right.isPresent()){
                            return Optional.of(new TernaryNode(left.get(), middle.get(), right.get()));
                        }else {
                            if (tokens.MoreTokens()) {
                                Optional<Token> error = tokens.Peek(0);
                                throw new Exception("No right ternary expression before line " + error.get().getLineNumber() + " position " + error.get().getPosition());
                            } else
                                throw new Exception("No right ternary expression last line");
                        }
                    } else{
                        if (tokens.MoreTokens()) {
                            Optional<Token> error = tokens.Peek(0);
                            throw new Exception("No ternary colon before line " + error.get().getLineNumber() + " position " + error.get().getPosition());
                        } else
                            throw new Exception("No ternary colon last line");
                    }

                }else {
                    if (tokens.MoreTokens()) {
                        Optional<Token> error = tokens.Peek(0);
                        throw new Exception("No middle ternary expression before line " + error.get().getLineNumber() + " position " + error.get().getPosition());
                    } else
                        throw new Exception("No middle ternary expression last line");
                }
            }
        }
        return left;

    }

    private Optional<Node> parseOR() throws Exception {
        Optional<Node> left = parseAND();
        if(left.isPresent()){
            //handles OR expressions
            if(tokens.MatchAndRemove(Token.tokenType.OR).isPresent()){
                Optional<Node> right = parseAND();
                if(right.isPresent()){
                    return Optional.of(new OperationNode(left.get(), OperationNode.Operations.OR, right));
                }else {
                    if (tokens.MoreTokens()) {
                        Optional<Token> error = tokens.Peek(0);
                        throw new Exception("No right expression before line " + error.get().getLineNumber() + " position " + error.get().getPosition());
                    } else
                        throw new Exception("No right expression last line");
                }
            }
        }
        return left;
    }

    private Optional<Node> parseAND() throws Exception {
        Optional<Node> left = parseArrayMembership();
        if(left.isPresent()){
            //handles AND expressions
            if(tokens.MatchAndRemove(Token.tokenType.AND).isPresent()){
                Optional<Node> right = parseArrayMembership();
                if(right.isPresent()){
                    return Optional.of(new OperationNode(left.get(), OperationNode.Operations.AND, right));
                }else {
                    if (tokens.MoreTokens()) {
                        Optional<Token> error = tokens.Peek(0);
                        throw new Exception("No right expression before line " + error.get().getLineNumber() + " position " + error.get().getPosition());
                    } else
                        throw new Exception("No right expression last line");
                }

            }
        }
        return left;
    }

    private Optional<Node> parseArrayMembership() throws Exception {
        Optional<Node> left = parseMatch();
        if(left.isPresent()){
            //Handles array membership
            if(tokens.MatchAndRemove(Token.tokenType.IN).isPresent()){
                Optional<Node> array = parseMatch();
                if(array.isPresent()){
                    if(array.get() instanceof VariableReferenceNode){
                            return Optional.of(new OperationNode(left.get(), OperationNode.Operations.IN, array));
                    }
                    else {
                        if (tokens.MoreTokens()) {
                            Optional<Token> error = tokens.Peek(0);
                            throw new Exception("Improper array name before line " + error.get().getLineNumber() + " position " + error.get().getPosition());
                        } else
                            throw new Exception("Improper array name expression last line");
                    }
                }else {
                    if (tokens.MoreTokens()) {
                        Optional<Token> error = tokens.Peek(0);
                        throw new Exception("No array name before line " + error.get().getLineNumber() + " position " + error.get().getPosition());
                    } else
                        throw new Exception("No array name last line");
                }
            }
        }
        return left;
    }

    private Optional<Node> parseMatch() throws Exception {
        Optional<Node> left = parseBooleanCompare();
        if(left.isPresent()){
            //Handles match and not match
            if(tokens.MatchAndRemove(Token.tokenType.MATCH).isPresent()){
                Optional<Node> right = parseBooleanCompare();
                if(right.isPresent()){
                    return Optional.of(new OperationNode(left.get(), OperationNode.Operations.MATCH, right));
                }else {
                    if (tokens.MoreTokens()) {
                        Optional<Token> error = tokens.Peek(0);
                        throw new Exception("No right expression before line " + error.get().getLineNumber() + " position " + error.get().getPosition());
                    } else
                        throw new Exception("No right expression last line");
                }
            }else if(tokens.MatchAndRemove(Token.tokenType.NOTMATCH).isPresent()){
                Optional<Node> right = parseBooleanCompare();
                if(right.isPresent()){
                    return Optional.of(new OperationNode(left.get(), OperationNode.Operations.NOTMATCH, right));
                }else {
                    if (tokens.MoreTokens()) {
                        Optional<Token> error = tokens.Peek(0);
                        throw new Exception("No right expression before line " + error.get().getLineNumber() + " position " + error.get().getPosition());
                    } else
                        throw new Exception("No right expression last line");
                }
            }
        }
        return left;
    }

    private Optional<Node> parseBooleanCompare() throws Exception {
        Optional<Node> left = parseConcatenation();
        if(left.isPresent()){
            //Handles boolean comparisons
            if(tokens.MatchAndRemove(Token.tokenType.GE).isPresent()) {
                Optional<Node> right = parseConcatenation();
                if(right.isPresent()){
                    return Optional.of(new OperationNode(left.get(), OperationNode.Operations.GE, right));
                }else {
                    if (tokens.MoreTokens()) {
                        Optional<Token> error = tokens.Peek(0);
                        throw new Exception("No right expression before line " + error.get().getLineNumber() + " position " + error.get().getPosition());
                    } else
                        throw new Exception("No right expression last line");
                }
            }else if (tokens.MatchAndRemove(Token.tokenType.GT).isPresent()) {
                Optional<Node> right = parseConcatenation();
                if(right.isPresent()){
                    return Optional.of(new OperationNode(left.get(), OperationNode.Operations.GT, right));
                }else {
                    if (tokens.MoreTokens()) {
                        Optional<Token> error = tokens.Peek(0);
                        throw new Exception("No right expression before line " + error.get().getLineNumber() + " position " + error.get().getPosition());
                    } else
                        throw new Exception("No right expression last line");
                }
            }else if (tokens.MatchAndRemove(Token.tokenType.COMPARETO).isPresent()) {
                Optional<Node> right = parseConcatenation();
                if(right.isPresent()){
                    return Optional.of(new OperationNode(left.get(), OperationNode.Operations.EQ, right));
                }else {
                    if (tokens.MoreTokens()) {
                        Optional<Token> error = tokens.Peek(0);
                        throw new Exception("No right expression before line " + error.get().getLineNumber() + " position " + error.get().getPosition());
                    } else
                        throw new Exception("No right expression last line");
                }
            }else if (tokens.MatchAndRemove(Token.tokenType.NOTEQUAL).isPresent()) {
                Optional<Node> right = parseConcatenation();
                if(right.isPresent()){
                    return Optional.of(new OperationNode(left.get(), OperationNode.Operations.NE, right));
                }else {
                    if (tokens.MoreTokens()) {
                        Optional<Token> error = tokens.Peek(0);
                        throw new Exception("No right expression before line " + error.get().getLineNumber() + " position " + error.get().getPosition());
                    } else
                        throw new Exception("No right expression last line");
                }
            }else if (tokens.MatchAndRemove(Token.tokenType.LE).isPresent()) {
                Optional<Node> right = parseConcatenation();
                if(right.isPresent()){
                    return Optional.of(new OperationNode(left.get(), OperationNode.Operations.LE, right));
                }else {
                    if (tokens.MoreTokens()) {
                        Optional<Token> error = tokens.Peek(0);
                        throw new Exception("No right expression before line " + error.get().getLineNumber() + " position " + error.get().getPosition());
                    } else
                        throw new Exception("No right expression last line");
                }
            }else if (tokens.MatchAndRemove(Token.tokenType.LT).isPresent()) {
                Optional<Node> right = parseConcatenation();
                if(right.isPresent()){
                    return Optional.of(new OperationNode(left.get(), OperationNode.Operations.LT, right));
                }else {
                    if (tokens.MoreTokens()) {
                        Optional<Token> error = tokens.Peek(0);
                        throw new Exception("No right expression before line " + error.get().getLineNumber() + " position " + error.get().getPosition());
                    } else
                        throw new Exception("No right expression last line");
                }
            }
        }
        return left;
    }

    private Optional<Node> parseConcatenation() throws Exception {
        Optional<Node> left = parseExpression();
        do {
            if (left.isPresent()) {
                //handles concatenations
                Optional<Node> right = parseExpression();
                if (right.isPresent()) {
                    left = Optional.of(new OperationNode(left.get(), OperationNode.Operations.CONCATENATION, right));
                }else{
                    return left;
                }
            }else{
                return left;
            }
        }while(true);
    }

    private Optional<Node> parseExpression() throws Exception {
        Optional<Node> left = parseTerm();
        do{
            //handles expressions
            Optional<Token> op = tokens.MatchAndRemove(Token.tokenType.PLUS);
            if(op.isEmpty()){
                op = tokens.MatchAndRemove(Token.tokenType.SUBTRACT);
                if(op.isEmpty()){
                    return left;
                }else{
                    Optional<Node> right = parseTerm();
                    left = Optional.of(new OperationNode(left.get(), OperationNode.Operations.SUBTRACT, right));
                }
            }else{
                Optional<Node> right = parseTerm();
                left = Optional.of(new OperationNode(left.get(), OperationNode.Operations.ADD, right));
            }
        }while(true);
    }

    private Optional<Node> parseTerm() throws Exception {
        Optional<Node> left = parseFactor();
        if(left.isPresent()) {
            //handles terms
            do {
                Optional<Token> op = tokens.MatchAndRemove(Token.tokenType.MULTIPLY);
                if (op.isEmpty()) {
                    op = tokens.MatchAndRemove(Token.tokenType.DIVIDE);
                    if (op.isEmpty()) {
                        op = tokens.MatchAndRemove(Token.tokenType.MODULO);
                        if(op.isEmpty()) {
                            return left;
                        }else{
                            Optional<Node> right = parseTerm();
                            left = Optional.of(new OperationNode(left.get(), OperationNode.Operations.MODULO, right));
                        }
                    } else {
                        Optional<Node> right = parseTerm();
                        left = Optional.of(new OperationNode(left.get(), OperationNode.Operations.DIVIDE, right));
                    }
                } else {
                    Optional<Node> right = parseTerm();
                    left = Optional.of(new OperationNode(left.get(), OperationNode.Operations.MULTIPLY, right));
                }
            } while (true);
        }
        return Optional.empty();
    }

    private Optional<Node> parseFactor() throws Exception {
        Optional<Node> left = parseExponent();
        //handles factors and numbers
        if(left.isPresent()){
            if(left.get() instanceof ConstantNode){
                return left;
            }
        }else if(tokens.MatchAndRemove(Token.tokenType.LEFTPARENTHESIS).isPresent()){
            Optional<Node> exp = parseExpression();
            if(exp.isEmpty()){
                if(tokens.MoreTokens()){
                    Optional<Token> error = tokens.Peek(0);
                    throw new Exception("Empty parenthesis before line " + error.get().getLineNumber() + " position " +error.get().getPosition());
                }else
                    throw new Exception("Empty parenthesis last line");
            }else if(tokens.MatchAndRemove(Token.tokenType.RIGHTPARENTHESIS).isEmpty()){
                if(tokens.MoreTokens()){
                    Optional<Token> error = tokens.Peek(0);
                    throw new Exception("No closing parenthesis before line " + error.get().getLineNumber() + " position " +error.get().getPosition());
                }else
                    throw new Exception("No closing parenthesis last line");
            }else{
                return exp;
            }
        }
        return left;
    }

    private Optional<Node> parseExponent() throws Exception {
        Optional<Node> left = parsePost();
        if (left.isPresent()){
            //handles exponents exponents
            if(tokens.MatchAndRemove(Token.tokenType.EXPONENTIAL).isPresent()){
                Optional<Node> right = parseExpression();
                return Optional.of(new OperationNode(left.get(), OperationNode.Operations.EXPONENT, right));
            }
        }
        return left;
    }

    private Optional<Node> parsePost() throws Exception {
        Optional<Node> node = ParseBottomLevel();
        if(node.isPresent()){
            //handles post increments and post decrements.
            if(tokens.MatchAndRemove(Token.tokenType.INCREMENT).isPresent()){
                return Optional.of(new OperationNode(node.get(), OperationNode.Operations.POSTINC));
            }else if(tokens.MatchAndRemove(Token.tokenType.DECREMENT).isPresent()){
                return Optional.of(new OperationNode(node.get(), OperationNode.Operations.POSTDEC));
            }else
                return node;
        }else
            return node;
    }

    private Optional<Node> ParseBottomLevel() throws Exception {
        Optional<Token> check = null; //this variable is used to hold the results of MatchAndRemove so that we don't loss the values inside the node.

        //The following brick of if statements handles string literals, number, and patterns.
        check = tokens.MatchAndRemove(Token.tokenType.STRINGLITERAL);
        if(check.isPresent()){
            return Optional.of(new ConstantNode(check.get().getValue()));
        }
        check = tokens.MatchAndRemove(Token.tokenType.NUMBER);
        if(check.isPresent()){
            return Optional.of(new ConstantNode(check.get().getValue()));
        }
        check = tokens.MatchAndRemove(Token.tokenType.PATTERN);
        if(check.isPresent()){
            return Optional.of(new PatternNode(check.get().getValue()));
        }

        //handles operations within parenthesis.
        check = tokens.MatchAndRemove(Token.tokenType.LEFTPARENTHESIS);
        if(check.isPresent()){
            Optional<Node> retVal = ParseOperation();
            if(retVal.isPresent()){
                if(tokens.MatchAndRemove(Token.tokenType.RIGHTPARENTHESIS).isPresent()){
                    return retVal;
                }else
                    throw new Exception("Error at line number " + check.get().getLineNumber() + " position " + check.get().getPosition() +
                            "no closing parenthesis");
            }else{
                throw new Exception();
            }
        }

        //handles NOT operation
        check = tokens.MatchAndRemove(Token.tokenType.NOT);
        if(check.isPresent()){
            Optional<Node> temporaryNode = ParseOperation();
            if(temporaryNode.isPresent()) {
                return Optional.of(new OperationNode(temporaryNode.get(), OperationNode.Operations.NOT));
            }else{
                throw new Exception();
            }
        }

        //handles SUBTRACT operation
        check = tokens.MatchAndRemove(Token.tokenType.SUBTRACT);
        if(check.isPresent()){
            Optional<Node> temporaryNode = ParseOperation();
            if(temporaryNode.isPresent()) {
                return Optional.of(new OperationNode(temporaryNode.get(), OperationNode.Operations.UNARYNEG));
            }else{
                throw new Exception();
            }
        }

        //handles PLUS operation
        check = tokens.MatchAndRemove(Token.tokenType.PLUS);
        if(check.isPresent()){
            Optional<Node> temporaryNode = ParseOperation();
            if(temporaryNode.isPresent()) {
                return Optional.of(new OperationNode(temporaryNode.get(), OperationNode.Operations.UNARYPOS));
            }else{
                throw new Exception();
            }
        }

        //handles INCREMENT operation
        check = tokens.MatchAndRemove(Token.tokenType.INCREMENT);
        if(check.isPresent()){
            Optional<Node> temporaryNode = ParseOperation();
            if(temporaryNode.isPresent()) {
                return Optional.of(new OperationNode(temporaryNode.get(), OperationNode.Operations.PREINC));
            }else{
                throw new Exception();
            }
        }

        //handles DECREMENT operation
        check = tokens.MatchAndRemove(Token.tokenType.DECREMENT);
        if(check.isPresent()){
            Optional<Node> temporaryNode = ParseOperation();
            if(temporaryNode.isPresent()) {
                return Optional.of(new OperationNode(temporaryNode.get(), OperationNode.Operations.PREDEC));
            }else{
                throw new Exception();
            }
        }

        Optional<Node> functionCall = parseFunctionCall();
        if(functionCall.isPresent()){
            return functionCall;
        }
        //if all if statements return false ParseLValue is called.
        return ParseLValue();
    }

    private Optional<Node> parseFunctionCall() throws Exception {
        if(tokens.MoreTokens()) {
            //here we peek for a word then a left parenthesis, if both are present we assume this is a function call.
            if (tokens.Peek(0).get().getType().equals(Token.tokenType.WORD)) {
                if(tokens.Peek(1) != null) {
                    if (tokens.Peek(1).get().getType().equals(Token.tokenType.LEFTPARENTHESIS)) {
                        String functionName = tokens.MatchAndRemove(Token.tokenType.WORD).get().getValue();
                        tokens.MatchAndRemove(Token.tokenType.LEFTPARENTHESIS);
                        LinkedList<Node> arguments = getStatements();
                        if (tokens.MatchAndRemove(Token.tokenType.RIGHTPARENTHESIS).isPresent()) {
                            return Optional.of(new FunctionCallNode(functionName, arguments));
                        } else {
                            throw new Exception("function call missing right parenthesis");
                        }
                    }
                }
                //handles function calls to getline
            }else if(tokens.MatchAndRemove(Token.tokenType.GETLINE).isPresent()){
                Optional<Node> variable = ParseOperation();
                if(variable.isPresent()){
                    LinkedList<Node> arguments = new LinkedList<>();
                    arguments.add(variable.get());
                    return Optional.of(new FunctionCallNode("getline", arguments));
                }else{
                    return Optional.of(new FunctionCallNode("getline"));
                }
                //handles function calls to print
            }else if(tokens.MatchAndRemove(Token.tokenType.PRINT).isPresent()){
                if(tokens.Peek(0).isPresent()) {
                    LinkedList<Node> arguments = new LinkedList<>();
                    Optional<Node> argument = ParseOperation();
                    if(argument.isPresent()) {
                        arguments.add(argument.get());
                    }else{
                        throw new Exception("print function missing arguments");
                    }
                    return Optional.of(new FunctionCallNode("print", arguments));
                }
                //handles function calls to printf
            }else if(tokens.MatchAndRemove(Token.tokenType.PRINTF).isPresent()){
                if(tokens.Peek(0).isPresent()) {
                    LinkedList<Node> arguments = getStatements();
                    return Optional.of(new FunctionCallNode("printf", arguments));
                }
                //handles function calls to exit
            }else if(tokens.MatchAndRemove(Token.tokenType.EXIT).isPresent()){
                if(tokens.Peek(0).isPresent()) {
                    LinkedList<Node> arguments = new LinkedList<>();
                    Optional<Node> exitCode = ParseOperation();
                    if(exitCode.isPresent()){
                        if(exitCode.get() instanceof ConstantNode){
                            arguments.add(exitCode.get());
                            return Optional.of(new FunctionCallNode("exit", arguments));
                        }else{
                            throw new Exception("improper exit code");
                        }
                    }else{
                        throw new Exception("missing exit code");
                    }

                }
                //handles function calls to next
            }else if(tokens.MatchAndRemove(Token.tokenType.NEXT).isPresent()){
                LinkedList<Node> arguments = new LinkedList<>();
                return Optional.of(new FunctionCallNode("NEXT", arguments));
                //handles function calls to nextfile
            }else if(tokens.MatchAndRemove(Token.tokenType.NEXTFILE).isPresent()){
                LinkedList<Node> arguments = new LinkedList<>();
                return Optional.of(new FunctionCallNode("nextfile", arguments));
            }else {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private LinkedList<Node> getStatements() throws Exception {
        LinkedList<Node> arguments = new LinkedList<>();
        do {
            Optional<Node> argument = ParseOperation();
            if (argument.isPresent()) {
                arguments.add(argument.get());
            } else if (arguments.size() >= 1) {
                //if this loop has run before that means there was a comma after the previous argument,
                //if ParseOperation returns null than that means there was no argument after the comma.
                throw new Exception("no argument after comma");
            }
        } while (tokens.MatchAndRemove(Token.tokenType.COMMA).isPresent());
        return arguments;
    }

    private Optional<Node> ParseLValue() throws Exception {
        if(tokens.MoreTokens()){
            if(tokens.MatchAndRemove(Token.tokenType.DOLLAR).isPresent()){
                Optional<Node> leftNode = ParseBottomLevel();
                if(leftNode.isPresent()){
                    return Optional.of(new OperationNode(leftNode.get(), OperationNode.Operations.DOLLAR));
                }
            }
        }
        //This if statement handles VariableReferenceNodes/Arrays
        Optional<Token> nameToken = tokens.MatchAndRemove(Token.tokenType.WORD);
        if(nameToken.isPresent()){
            Optional<Token> bracketToken = Optional.empty();
            if(tokens.MoreTokens()) {
                bracketToken = tokens.MatchAndRemove(Token.tokenType.LEFTBRACKET);
            }
            if(bracketToken.isPresent()){
                Optional<Node> index = ParseOperation();
                if (index.isPresent()) {
                    if(tokens.MatchAndRemove(Token.tokenType.RIGHTBRACKET).isPresent()) {
                        return Optional.of(new VariableReferenceNode(nameToken.get().getValue(), index.get()));
                    }else{
                        throw new Exception("No closing bracket at line number "+ bracketToken.get().getLineNumber() + " position " + bracketToken.get().getPosition());
                    }
                } else {
                    throw new Exception();
                }
            }else{
                String value = nameToken.get().getValue();
                VariableReferenceNode retVal = new VariableReferenceNode(value);
                return Optional.of(retVal);
            }
        }
        return Optional.empty();
    }
}