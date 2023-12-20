import java.util.Optional;

public class VariableReferenceNode implements Node{
    private String name;

    private Optional<Node> index = Optional.empty();

    public VariableReferenceNode(String name){
        this.name = name;
    }

    public VariableReferenceNode(String name, Node index){
        this(name);
        this.index = Optional.ofNullable(index);
    }

    public String getName() {
        return name;
    }

    public Optional<Node> getIndex() {
        return index;
    }

    @Override
    public String toString() {
        if(index.isPresent()){
            return name + "[" + index.get() + "]";
        }
        return name;
    }
}
