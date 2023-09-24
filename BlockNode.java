import java.util.LinkedList;
import java.util.Optional;

public class BlockNode implements Node{

    private LinkedList<StatementNode> statements = new LinkedList<>();

    Optional<Node> Condition;

    private int position;

    public BlockNode(){

    }
    public int getPosition(){
        return position;
    }

    public LinkedList<StatementNode> getStatements(){
        return statements;
    }

    @Override
    public String toString(){
        String retString = "{";
        for(StatementNode node: statements) {
            retString+=node.toString()+ " ";
        }
        return retString + "}";
    }

}
