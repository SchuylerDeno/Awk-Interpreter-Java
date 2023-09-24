import java.util.LinkedList;
import java.util.Optional;

public class Parser {
    private TokenHandler tokens;
    public Parser(LinkedList<Token> tokens){
        this.tokens = new TokenHandler(tokens);
    }
    public ProgramNode Parse() throws Exception {
        ProgramNode program = new ProgramNode();
        while(tokens.MoreTokens()){
            if(ParseFunction(program) || ParseAction(program)){
            }else{
                throw new Exception("Error in parse, unrecognized syntax");
            }
        }
        return program;
    }

    private boolean ParseFunction(ProgramNode node) throws Exception {
        /*return false if this is not a function. If it is a function,
         it should create the FunctionDefinitionNode, populate it with name and parameters
         and add it to the ProgramNode’s function list.
         Call ParseBlock() (see below) and add the statements from the BlockNode
         to the FunctionDefinitionNode’s linked list of StatementNode.*/
        if(tokens.MoreTokens()){
            String name = "";
            LinkedList<String> parameters = new LinkedList<>();
            if(tokens.MatchAndRemove(Token.tokenType.FUNCTION).equals(Optional.empty())){
                return false;
            }
            if(tokens.Peek(0).equals(Token.tokenType.WORD)) {
                name = tokens.MatchAndRemove(Token.tokenType.WORD).get().getValue();
            }else
                throw new Exception("Function has no name");
            if(tokens.MatchAndRemove(Token.tokenType.LEFTPARENTHESIS).equals(Optional.empty())){
                throw new Exception("Function "+ name + " is missing \"(\"");
            }
            while(tokens.MatchAndRemove(Token.tokenType.RIGHTPARENTHESIS).equals(Optional.empty())){
                if(tokens.MoreTokens()){
                        if (tokens.Peek(0).equals(Token.tokenType.WORD)) {
                            parameters.add(tokens.MatchAndRemove(Token.tokenType.WORD).get().getValue());
                            if(!AcceptSeparators()){
                                if(tokens.MatchAndRemove(Token.tokenType.RIGHTPARENTHESIS).equals(Optional.empty())) {
                                    throw new Exception("Function " + name + " is missing \")\"");
                                }else{
                                    break;
                                }
                            }
                        } else {
                            throw new Exception("In function " + name + " Keyword " + tokens.Peek(0).get().getType() + " cannot be used as a parameter");
                        }
                }else{
                    throw new Exception("Function "+ name + " is missing \")\"");
                }
            }
            FunctionNode newFunction = new FunctionNode(name, parameters);
            newFunction.addStatements(parseBlock().getStatements());
            node.addFunctionNode(newFunction);
        }else{
            return false;
        }
        return false;
    }

    private boolean ParseAction(ProgramNode node) {
        if(tokens.MatchAndRemove(Token.tokenType.BEGIN).equals(Token.tokenType.BEGIN)){
            node.addBeginBlock(parseBlock());
            return true;
        }else if(tokens.MatchAndRemove(Token.tokenType.END).equals(Token.tokenType.END)){
            node.addEndBlock(parseBlock());
            return true;
        }else{
            ParseOperation();
            node.addBlock(parseBlock());
            return true;
        }
    }

    private BlockNode parseBlock(){
        return new BlockNode();
    }

    private Optional<Node> ParseOperation(){
        return Optional.empty();
    }
    private boolean AcceptSeparators(){
        boolean found = false;
        while (tokens.MatchAndRemove(Token.tokenType.SEPARATOR).equals(Token.tokenType.SEPARATOR)){
            found = true;
        }
        return found;
    }
}
