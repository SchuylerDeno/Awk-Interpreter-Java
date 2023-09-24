import java.util.LinkedList;
import java.util.Optional;

public class TokenHandler {
    private LinkedList<Token> tokens = new LinkedList<>();

    public TokenHandler(LinkedList<Token> tokens){
        this.tokens = tokens;
    }
    public Optional<Token> Peek(int j){
        if(j<tokens.size()) {
            return Optional.ofNullable(tokens.get(j));
        }
        return null;
    }
    public boolean MoreTokens(){
        if(tokens.size()>0){
            return true;
        }else {
            return false;
        }
    }
    public Optional<Token> MatchAndRemove(Token.tokenType t){
        if(tokens.getFirst().getType() == t){
            return Optional.ofNullable(tokens.removeFirst());
        }else{
            return Optional.empty();
        }
    }
}
