import java.util.LinkedList;

public class ProgramNode implements Node{

    private LinkedList<BlockNode> endBlocks = new LinkedList<>();

    private LinkedList<BlockNode> startBlocks = new LinkedList<>();

    private LinkedList<BlockNode> blocks = new LinkedList<>();

    private LinkedList<FunctionNode> functions = new LinkedList<>();

    public ProgramNode(){

    }

    public void addEndBlock(BlockNode node){
        endBlocks.add(node);
    }

    public void addBeginBlock(BlockNode node){
        startBlocks.add(node);
    }
    public void addBlock(BlockNode node){
        blocks.add(node);
    }
    public void addFunctionNode(FunctionNode node){
        functions.add(node);
    }

    public LinkedList<BlockNode> getBlocks(){
        return blocks;
    }

    public LinkedList<BlockNode> getBeginBlocks(){
        return startBlocks;
    }

    public LinkedList<BlockNode> getEndBlocks(){
        return endBlocks;
    }

    public LinkedList<FunctionNode> getFunctions(){
        return functions;
    }

    @Override
    public String toString() {
        return  "BEGIN" + startBlocks + "\nBlocks" + blocks + "\nEND" + endBlocks + "\nFunctions" + functions;

    }
}
