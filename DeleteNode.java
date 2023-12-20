import java.util.LinkedList;
import java.util.Optional;

public class DeleteNode extends StatementNode{
    private VariableReferenceNode name;
    private Optional<LinkedList<ConstantNode>> indexes = Optional.empty();

    public DeleteNode(VariableReferenceNode name){
        this.name = name;
    }
    public DeleteNode(VariableReferenceNode name, LinkedList<ConstantNode> indexes){
        this(name);
        this.indexes = Optional.ofNullable(indexes);
    }

    public Optional<LinkedList<ConstantNode>> getIndexes() {
        return indexes;
    }

    public VariableReferenceNode getName() {
        return name;
    }

    @Override
    public String toString() {
        if(indexes.isPresent()) {
            return "delete " + name + indexes.get();
        }else{
            return "delete " + name;
        }
    }
}
