import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

public class Interpreter {
    public class LineManager {

        private List<String> file;
        public LineManager(List<String> file){
            this.file = file;
            //initializes NR and FNR
            globalVariables.put("NR", new InterpreterDataType("" + file.size()));
            globalVariables.put("FNR", new InterpreterDataType("" + file.size()));
        }

        public boolean splitAndAssign(){
            //checks to see if there are any more lines to split.
            if(file.size()>0) {
                String[] line = file.removeFirst().split(globalVariables.get("FS").getValue());
                int index = 1;
                String $0 = "";
                //adds the fields into the global variables.
                for (String string: line) {
                    globalVariables.put("$"+index, new InterpreterDataType(string));
                    if(index==line.length){
                        $0 += string;
                    }else {
                        $0 += string + " ";
                    }
                    index++;
                }
                globalVariables.put("$0", new InterpreterDataType($0));
                globalVariables.put("NF", new InterpreterDataType("" + line.length));
                return true;
            }else {
                return false;
            }
        }
    }

    private HashMap<String, InterpreterDataType> globalVariables;

    private HashMap<String, FunctionNode> functions;

    private LineManager lineManager;
    private ProgramNode programNode;
    public Interpreter(String path, String FS, ProgramNode programNode) throws IOException {
        globalVariables = new HashMap<>();
        functions = new HashMap<>();
        this.programNode = programNode;
        initializeGlobals(FS);
        //if a file path is not provided, a line manager with an empty collection of strings is created.
        if(path != null){
            globalVariables.put("FILENAME", new InterpreterDataType(path));
            Path myPath = Paths.get(path);
            lineManager = new LineManager (Files.readAllLines (myPath));
        }else{
            globalVariables.put("FILENAME", null);
            lineManager = new LineManager(new LinkedList<String>());
        }
        //initializes the user's functions from the ProgramNode
        LinkedList<FunctionNode> functionNodes = programNode.getFunctions();
        for (FunctionNode node : functionNodes) {
            functions.put(node.getName(), node);
        }
        //initializes the built-in functions
        putBIFN();
    }
    private void initializeGlobals(String FS){
        //intializes global variables

        if(FS != null){
            globalVariables.put("FS", new InterpreterDataType(FS));
        }else{
            globalVariables.put("FS", new InterpreterDataType(" "));
        }
        globalVariables.put("OFMT", new InterpreterDataType("%.6g"));
        globalVariables.put("OFS", new InterpreterDataType(" "));
        globalVariables.put("ORS", new InterpreterDataType("\n"));
    }
    private void putBIFN(){
        var string = new LinkedList<String>();
        string.add("string");
        var index = new LinkedList<String>();
        index.add("in");
        index.add("find");
        var substr = new LinkedList<String>();
        substr.add("string");
        substr.add("start");
        substr.add("length");
        var sub = new LinkedList<String>();
        sub.add("regex");
        sub.add("replacement");
        sub.add("target");
        var split = new LinkedList<String>();
        split.add("string");
        split.add("array");
        split.add("fieldsep");
        var match = new LinkedList<String>();
        match.add("string");
        match.add("regex");
        match.add("array");
        var gsub = new LinkedList<String>();
        gsub.add("regex");
        gsub.add("replacement");
        gsub.add("target");

        functions.put("length", new BuiltInFunctionNode(lengthLambda, false, 1, string));
        functions.put("toupper", new BuiltInFunctionNode(toUpperLambda, false,1, string));
        functions.put("tolower", new BuiltInFunctionNode(toLowerLambda, false,1, string));
        functions.put("substr", new BuiltInFunctionNode(subStrLambda, false, 3, substr));
        functions.put("sub", new BuiltInFunctionNode(subLambda, false,3, sub));
        functions.put("sprintf", new BuiltInFunctionNode(sprintfLambda, true, 0, null));
        functions.put("split", new BuiltInFunctionNode(splitLambda, false, 3, split));
        functions.put("match", new BuiltInFunctionNode(matchLambda, false, 3, match));
        functions.put("gsub", new BuiltInFunctionNode(gsubLambda, false, 3, gsub));
        functions.put("next", new BuiltInFunctionNode(nextLambda, false, 0, null));
        functions.put("getline", new BuiltInFunctionNode(getlineLambda, false, 0, null));
        functions.put("print", new BuiltInFunctionNode(printLambda, false, 1, string));
        functions.put("printf", new BuiltInFunctionNode(printfLambda, true, 0, null));
        functions.put("index", new BuiltInFunctionNode(indexLambda, false, 2, index));
    }

    public void interpretProgram() throws Exception {
            //here is the outermost layer of the interpreter in which each block contained in the program node is interpreted.
            var beginBlocks = programNode.getBeginBlocks();
            var blocks = programNode.getBlocks();
            var endBlocks = programNode.getEndBlocks();
            for (BlockNode beginBlock : beginBlocks) {
                interpretBlock(beginBlock);
            }
            //here we loop through the blocks for each line of the program.
            while(lineManager.splitAndAssign()) {
                for (BlockNode block : blocks) {
                    interpretBlock(block);
                }
            }
            for (BlockNode endBlock : endBlocks) {
                interpretBlock(endBlock);
            }
    }

    private void interpretBlock(BlockNode block) throws Exception {
        boolean condition = true;
        //if the block node has a condition it is evaluated
        if(block.getCondition().isPresent()){
            String blockCondition = getIDT(block.getCondition().get(), null).getValue();
            if(isNumeric(blockCondition)) {
                float numValue = Float.parseFloat(blockCondition);
                float zero = 0;
                if (numValue == zero) {
                    condition = false;
                }
            }
        }
        //if the condition is true or does not exist the statements inside the block will be processed.
        if(condition){
            for (StatementNode statement: block.getStatements()) {
                ProcessStatement(null, statement);
            }
        }
    }
    private InterpreterDataType getIDT(Node node, HashMap<String, InterpreterDataType> localVariables) throws Exception {
        if(node instanceof AssignmentNode){
            AssignmentNode assignmentNode = (AssignmentNode) node;
            //The user can only assign to variables or a global variable like $1, here these two situations are handled.
            if(assignmentNode.getTarget() instanceof VariableReferenceNode){
                if(((VariableReferenceNode) assignmentNode.getTarget()).getIndex().isPresent()){
                    InterpreterDataType expressionIDT = getIDT(assignmentNode.getExpression(), localVariables);
                    if(localVariables != null){
                        if(localVariables.containsKey(((VariableReferenceNode) assignmentNode.getTarget()).getName())){
                            if(localVariables.get(((VariableReferenceNode) assignmentNode.getTarget()).getName()) instanceof InterpreterArrayDataType){
                                HashMap<String, InterpreterDataType> array = ((InterpreterArrayDataType) localVariables.get(((VariableReferenceNode) assignmentNode.getTarget()).getName())).getVariableArray();
                                array.put(getIDT(((VariableReferenceNode) assignmentNode.getTarget()).getIndex().get(), localVariables).getValue(), expressionIDT);
                                localVariables.put(((VariableReferenceNode) assignmentNode.getTarget()).getName(), new InterpreterArrayDataType(array));
                                return expressionIDT;
                            }else{
                                throw new Exception("cannot access variable as array");
                            }
                        }
                        HashMap<String, InterpreterDataType> array = new HashMap<>();
                        array.put(getIDT(((VariableReferenceNode) assignmentNode.getTarget()).getIndex().get(), localVariables).getValue(), expressionIDT);
                        localVariables.put(((VariableReferenceNode) assignmentNode.getTarget()).getName(), new InterpreterArrayDataType(array));
                        return expressionIDT;
                    }else if(globalVariables.containsKey(((VariableReferenceNode) assignmentNode.getTarget()).getName())) {
                        if (globalVariables.get(((VariableReferenceNode) assignmentNode.getTarget()).getName()) instanceof InterpreterArrayDataType) {
                            HashMap<String, InterpreterDataType> array = ((InterpreterArrayDataType) globalVariables.get(((VariableReferenceNode) assignmentNode.getTarget()).getName())).getVariableArray();
                            array.put(getIDT(((VariableReferenceNode) assignmentNode.getTarget()).getIndex().get(), localVariables).getValue(), expressionIDT);
                            globalVariables.put(((VariableReferenceNode) assignmentNode.getTarget()).getName(), new InterpreterArrayDataType(array));
                            return expressionIDT;
                        } else {
                            throw new Exception("cannot access variable as array");
                        }
                    }else{
                        HashMap<String, InterpreterDataType> array = new HashMap<>();
                        array.put(getIDT(((VariableReferenceNode) assignmentNode.getTarget()).getIndex().get(), localVariables).getValue(), expressionIDT);
                        globalVariables.put(((VariableReferenceNode) assignmentNode.getTarget()).getName(), new InterpreterArrayDataType(array));
                        return expressionIDT;
                    }
                }else {
                    InterpreterDataType expressionIDT = getIDT(assignmentNode.getExpression(), localVariables);
                    if (localVariables == null) {
                        globalVariables.put(((VariableReferenceNode) assignmentNode.getTarget()).getName(), expressionIDT);
                    } else {
                        localVariables.put(((VariableReferenceNode) assignmentNode.getTarget()).getName(), expressionIDT);
                    }
                    return expressionIDT;
                }
            }else if(assignmentNode.getTarget() instanceof OperationNode){
                if(((OperationNode) assignmentNode.getTarget()).getOperation() == OperationNode.Operations.DOLLAR){
                    InterpreterDataType expressionIDT = getIDT(assignmentNode.getExpression(), localVariables);
                    //getIDT is called on the left side of the operation node because something like $a or $(a+1) is valid in awk.
                    InterpreterDataType dollarIDT = getIDT(((OperationNode) assignmentNode.getTarget()).getLeft(), localVariables);
                    globalVariables.put("$" + dollarIDT.getValue(), expressionIDT);
                    return expressionIDT;
                }
            }else{
                throw new Exception("Invalid assignment");
            }
        }else if (node instanceof ConstantNode){
            ConstantNode constantNode = (ConstantNode) node;
            return new InterpreterDataType(constantNode.getValue());
        }else if(node instanceof FunctionCallNode){
            return new InterpreterDataType(runFunctionCall((FunctionCallNode) node, localVariables));
        }else if(node instanceof PatternNode){
            return new InterpreterDataType(((PatternNode) node).getValue());
            //throw new Exception("Unexpected Pattern Node");
        }else if(node instanceof TernaryNode){
            //The condition of the ternary node is evaluated using getIDT, and then either the consequent
            //or the alternate is returned based on the results
            InterpreterDataType condition = getIDT(((TernaryNode) node).getCondition(), localVariables);
            if(condition.getValue().equals("1")){
                return getIDT(((TernaryNode) node).getConsequent(), localVariables);
            }else{
                return getIDT(((TernaryNode) node).getAlternate(), localVariables);
            }
        }else if(node instanceof VariableReferenceNode){
            //for variable reference node we have two situations, either an array reference or a normal variable reference.
            if(((VariableReferenceNode) node).getIndex().isPresent()){
                InterpreterDataType index = getIDT(((VariableReferenceNode) node).getIndex().get(), localVariables);
                if(localVariables != null) {
                    if (localVariables.get(((VariableReferenceNode) node).getName()) instanceof InterpreterArrayDataType) {
                        if (((InterpreterArrayDataType) localVariables.get(((VariableReferenceNode) node).getName())).getVariableArray().containsKey(index.getValue())) {
                            return ((InterpreterArrayDataType) localVariables.get(((VariableReferenceNode) node).getName())).getVariableArray().get(index.getValue());
                        } else {
                            throw new Exception("array does not contain index");
                        }
                    } else {
                        throw new Exception("cannot evaluate an index of a non array variable");
                    }
                }else{
                    if (globalVariables.get(((VariableReferenceNode) node).getName()) instanceof InterpreterArrayDataType) {
                        if (((InterpreterArrayDataType) globalVariables.get(((VariableReferenceNode) node).getName())).getVariableArray().containsKey(index.getValue())) {
                            return ((InterpreterArrayDataType) globalVariables.get(((VariableReferenceNode) node).getName())).getVariableArray().get(index.getValue());
                        } else {
                            throw new Exception("array does not contain index");
                        }
                    } else {
                        throw new Exception("no");
                    }
                }
            }else{
                if(localVariables != null) {
                    if (localVariables.containsKey(((VariableReferenceNode) node).getName())) {
                        return localVariables.get(((VariableReferenceNode) node).getName());
                    } else {
                        localVariables.put(((VariableReferenceNode) node).getName(), new InterpreterDataType(""));
                        return localVariables.get(((VariableReferenceNode) node).getName());
                    }
                }
                if(globalVariables.containsKey(((VariableReferenceNode) node).getName())){
                    if(globalVariables.get(((VariableReferenceNode) node).getName()) instanceof InterpreterArrayDataType){
                        return new InterpreterDataType(((VariableReferenceNode) node).getName());
                    }
                    return globalVariables.get(((VariableReferenceNode) node).getName());
                }else if(localVariables == null){
                    globalVariables.put(((VariableReferenceNode) node).getName(), new InterpreterDataType(""));
                    return globalVariables.get(((VariableReferenceNode) node).getName());
                }else{
                    return new InterpreterDataType("");
                }
            }
        }else if(node instanceof OperationNode){
            InterpreterDataType leftIDT = getIDT(((OperationNode) node).getLeft(), localVariables);
            InterpreterDataType rightIDT = null;
            boolean isNumber = false;
            if(((OperationNode) node).getRight().isPresent()){
                rightIDT = getIDT(((OperationNode) node).getRight().get(), localVariables);
            }
            //here the properties of the two IDTs are checked to see if they are able to convert into floats.
            if(rightIDT != null && !(rightIDT instanceof InterpreterArrayDataType)) {
                if (isNumeric(leftIDT.getValue()) && isNumeric(rightIDT.getValue())) {
                    isNumber = true;
                }
            }else{
                if (isNumeric(leftIDT.getValue())) {
                    isNumber = true;
                }
            }
            //the following if else if statement evaluates mathmatical operations, comparisons, and booleans.
            if(((OperationNode) node).getOperation() == OperationNode.Operations.ADD){
                if(isNumber){
                    return new InterpreterDataType(String.valueOf(Integer.parseInt(leftIDT.getValue()) + Integer.parseInt(rightIDT.getValue())));
                }else
                    return new InterpreterDataType("0");
            }else if(((OperationNode) node).getOperation() == OperationNode.Operations.SUBTRACT){
                if(isNumber){
                    return new InterpreterDataType(String.valueOf(Integer.parseInt(leftIDT.getValue()) - Integer.parseInt(rightIDT.getValue())));
                }else
                    return new InterpreterDataType("0");
            }else if(((OperationNode) node).getOperation() == OperationNode.Operations.MULTIPLY){
                if(isNumber){
                    return new InterpreterDataType(String.valueOf(Float.parseFloat(leftIDT.getValue()) * Float.parseFloat(rightIDT.getValue())));
                }else
                    return new InterpreterDataType("0");
            }else if(((OperationNode) node).getOperation() == OperationNode.Operations.DIVIDE){
                if(isNumber){
                    return new InterpreterDataType(String.valueOf(Float.parseFloat(leftIDT.getValue()) / Float.parseFloat(rightIDT.getValue())));
                }else
                    return new InterpreterDataType("0");
            }else if(((OperationNode) node).getOperation() == OperationNode.Operations.MODULO){
                if(isNumber){
                    return new InterpreterDataType(String.valueOf(Float.parseFloat(leftIDT.getValue()) % Float.parseFloat(rightIDT.getValue())));
                }else
                    return new InterpreterDataType("0");
            }else if(((OperationNode) node).getOperation() == OperationNode.Operations.EXPONENT){
                if(isNumber){
                    return new InterpreterDataType(String.valueOf(Math.pow(Float.parseFloat(leftIDT.getValue()),  Float.parseFloat(rightIDT.getValue()))));
                }else
                    return new InterpreterDataType("0");
            }else if(((OperationNode) node).getOperation() == OperationNode.Operations.CONCATENATION){
                return new InterpreterDataType(leftIDT.getValue() + rightIDT.getValue());
            }else if(((OperationNode) node).getOperation() == OperationNode.Operations.UNARYNEG){
                if(isNumber){
                    return new InterpreterDataType(String.valueOf(-Float.parseFloat(leftIDT.getValue())));
                }else
                    return new InterpreterDataType("0");
            }else if(((OperationNode) node).getOperation() == OperationNode.Operations.UNARYPOS){
                //after experimenting with awk, it appears that unarypos does nothing.
                if(isNumber){
                    return new InterpreterDataType(String.valueOf(Float.parseFloat(leftIDT.getValue())));
                }else
                    return new InterpreterDataType("0");
            }else if(((OperationNode) node).getOperation() == OperationNode.Operations.NOT){
                if(isNumber){
                    if(Float.parseFloat(leftIDT.getValue())!=0){
                        return new InterpreterDataType("0");
                    }
                    return new InterpreterDataType("1");
                }else
                    return new InterpreterDataType("0");
            }else if(((OperationNode) node).getOperation() == OperationNode.Operations.POSTDEC){
                if(isNumber){
                    var nodeName = ((OperationNode) node).getLeft();
                    globalVariables.put(((VariableReferenceNode)nodeName).getName(), new InterpreterDataType(String.valueOf(Integer.parseInt(leftIDT.getValue())-1)));
                    return globalVariables.get(((VariableReferenceNode)nodeName).getName());
                }else
                    return new InterpreterDataType("-1");
            }else if(((OperationNode) node).getOperation() == OperationNode.Operations.PREDEC){
                if(isNumber){
                    var nodeName = ((OperationNode) node).getLeft();
                    globalVariables.put(((VariableReferenceNode)nodeName).getName(), new InterpreterDataType(String.valueOf(Integer.parseInt(leftIDT.getValue())-1)));
                    return globalVariables.get(((VariableReferenceNode)nodeName).getName());
                }else
                    return new InterpreterDataType("-1");
            }else if(((OperationNode) node).getOperation() == OperationNode.Operations.POSTINC){
                if(isNumber){
                    var nodeName = ((OperationNode) node).getLeft();
                    globalVariables.put(((VariableReferenceNode)nodeName).getName(), new InterpreterDataType(String.valueOf(Integer.parseInt(leftIDT.getValue())+1)));
                    return globalVariables.get(((VariableReferenceNode)nodeName).getName());
                }else
                    return new InterpreterDataType("1");
            }else if(((OperationNode) node).getOperation() == OperationNode.Operations.PREINC){
                if(isNumber){
                    var nodeName = ((OperationNode) node).getLeft();
                    globalVariables.put(((VariableReferenceNode)nodeName).getName(), new InterpreterDataType(String.valueOf(Integer.parseInt(leftIDT.getValue())+1)));
                    return globalVariables.get(((VariableReferenceNode)nodeName).getName());
                }else
                    return new InterpreterDataType("1");
            }else if(((OperationNode) node).getOperation() == OperationNode.Operations.AND) {
                if (isNumber) {
                    if(Float.parseFloat(leftIDT.getValue())!=0 && Float.parseFloat(rightIDT.getValue())!=0){
                        return new InterpreterDataType("1");
                    }else{
                        return new InterpreterDataType("0");
                    }
                } else{
                    return new InterpreterDataType("1");
                }
            }else if(((OperationNode) node).getOperation() == OperationNode.Operations.OR) {
                if (isNumber) {
                    if(Float.parseFloat(leftIDT.getValue())!=0 || Float.parseFloat(rightIDT.getValue())!=0){
                        return new InterpreterDataType("1");
                    }else{
                        return new InterpreterDataType("0");
                    }
                } else{
                    return new InterpreterDataType("1");
                }
            }else if(((OperationNode) node).getOperation() == OperationNode.Operations.EQ) {
                if (isNumber) {
                    if(Float.parseFloat(leftIDT.getValue()) == Float.parseFloat(rightIDT.getValue())){
                        return new InterpreterDataType("1");
                    }else{
                        return new InterpreterDataType("0");
                    }
                } else{
                    if(leftIDT.getValue().equals(rightIDT.getValue())){
                        return new InterpreterDataType("1");
                    }else
                        return new InterpreterDataType("0");
                }
            }else if(((OperationNode) node).getOperation() == OperationNode.Operations.NE) {
                if (isNumber) {
                    if(Float.parseFloat(leftIDT.getValue()) != Float.parseFloat(rightIDT.getValue())){
                        return new InterpreterDataType("1");
                    }else{
                        return new InterpreterDataType("0");
                    }
                } else{
                    if(!leftIDT.getValue().equals(rightIDT.getValue())){
                        return new InterpreterDataType("1");
                    }else
                        return new InterpreterDataType("0");
                }
            }else if(((OperationNode) node).getOperation() == OperationNode.Operations.GE) {
                if (isNumber) {
                    if(Float.parseFloat(leftIDT.getValue()) >= Float.parseFloat(rightIDT.getValue())){
                        return new InterpreterDataType("1");
                    }else{
                        return new InterpreterDataType("0");
                    }
                } else{
                    if(leftIDT.getValue().compareTo(rightIDT.getValue()) >= 0){
                        return new InterpreterDataType("1");
                    }else
                        return new InterpreterDataType("0");
                }
            }else if(((OperationNode) node).getOperation() == OperationNode.Operations.GT) {
                if (isNumber) {
                    if(Float.parseFloat(leftIDT.getValue()) > Float.parseFloat(rightIDT.getValue())){
                        return new InterpreterDataType("1");
                    }else{
                        return new InterpreterDataType("0");
                    }
                } else{
                    if(leftIDT.getValue().compareTo(rightIDT.getValue()) > 0){
                        return new InterpreterDataType("1");
                    }else
                        return new InterpreterDataType("0");
                }
            }else if(((OperationNode) node).getOperation() == OperationNode.Operations.LE) {
                if (isNumber) {
                    if(Float.parseFloat(leftIDT.getValue()) <= Float.parseFloat(rightIDT.getValue())){
                        return new InterpreterDataType("1");
                    }else{
                        return new InterpreterDataType("0");
                    }
                } else{
                    if(leftIDT.getValue().compareTo(rightIDT.getValue()) <= 0){
                        return new InterpreterDataType("1");
                    }else
                        return new InterpreterDataType("0");
                }
            }else if(((OperationNode) node).getOperation() == OperationNode.Operations.LT) {
                if (isNumber) {
                    if(Float.parseFloat(leftIDT.getValue()) < Float.parseFloat(rightIDT.getValue())){
                        return new InterpreterDataType("1");
                    }else{
                        return new InterpreterDataType("0");
                    }
                } else{
                    if(leftIDT.getValue().compareTo(rightIDT.getValue()) < 0){
                        return new InterpreterDataType("1");
                    }else
                        return new InterpreterDataType("0");
                }
            }else if(((OperationNode) node).getOperation() == OperationNode.Operations.MATCH) {
                if(leftIDT.getValue().matches("(.*)" + rightIDT.getValue() + "(.*)")){
                    return new InterpreterDataType("1");
                }else
                    return new InterpreterDataType("0");
            }else if(((OperationNode) node).getOperation() == OperationNode.Operations.NOTMATCH) {
                if(leftIDT.getValue().matches("(.*)" + rightIDT.getValue() + "(.*)")){
                    return new InterpreterDataType("0");
                }else
                    return new InterpreterDataType("1");
            }else if(((OperationNode) node).getOperation() == OperationNode.Operations.DOLLAR) {
                if(globalVariables.containsKey("$"+leftIDT.getValue())){
                    return globalVariables.get("$"+leftIDT.getValue());
                }else
                    return new InterpreterDataType("");
            }else if(((OperationNode) node).getOperation() == OperationNode.Operations.IN) {
                if(((OperationNode) node).getRight().isPresent()){
                    if(((OperationNode) node).getRight().get() instanceof VariableReferenceNode){
                        //first the global variables are searched and then followed by the local variables if they are present.
                        if(globalVariables.containsKey(((VariableReferenceNode)(((OperationNode) node).getRight().get())).getName())){
                            InterpreterDataType array = globalVariables.get(((VariableReferenceNode)(((OperationNode) node).getRight().get())).getName());
                            if(array instanceof InterpreterArrayDataType){
                                if(((InterpreterArrayDataType) array).getVariableArray().containsKey(leftIDT.getValue())){
                                    return new InterpreterDataType("1");
                                }else
                                    return new InterpreterDataType("0");
                            }else{
                                throw new Exception("IN must be called on an array");
                            }
                        }else if(localVariables!=null){
                            if(localVariables.containsKey(((VariableReferenceNode)(((OperationNode) node).getRight().get())).getName())){
                                InterpreterDataType array = localVariables.get(((VariableReferenceNode)(((OperationNode) node).getRight().get())).getName());
                                if(array instanceof InterpreterArrayDataType){
                                    if(((InterpreterArrayDataType) array).getVariableArray().containsKey(leftIDT.getValue())){
                                        return new InterpreterDataType("1");
                                    }else
                                        return new InterpreterDataType("0");
                                }else{
                                    throw new Exception("IN must be called on an array");
                                }
                            }
                        }else{
                           throw new Exception("array does not exist");
                        }
                    }else
                        throw new Exception("IN must be called on an array");
                }else
                    throw new Exception("IN array is missing");
            }


        }else {
            throw new Exception("invalid node type");
        }
        return new InterpreterDataType();
    }

    private static boolean isNumeric(String str) {
        //checks to see if the string can properly convert into a float.
        try {
            Float.parseFloat(str);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }

    private String runFunctionCall(FunctionCallNode functionCall, HashMap<String, InterpreterDataType> localVariables) throws Exception {
        FunctionNode function = functions.get(functionCall.getFunctionName());
        if(function instanceof BuiltInFunctionNode){
            //this statement decides if the function is a variadic or non-variadic function.
            if(((BuiltInFunctionNode) function).isVariadic()){
                var map = new HashMap<String, InterpreterDataType>();
                var parameters = functionCall.getParameters();
                int index = 0;
                //I noticed that the requirements for runFunctionCall mention having to
                // "Maps non-variadic parameters, makes array for remaining parameters" now this simply isn't necessary to do it here
                // as I handled placing them into an array within the variadic functions themselves(one of the results of building backwards.).
                while(index < parameters.size()){
                    map.put(index+"", getIDT(parameters.get(index), localVariables));
                    index++;
                }
                return ((BuiltInFunctionNode) function).execute(map);
            }else{
                //It won't throw an error for less than the required amount,
                //but I made this choice to deal with the functions that can have different numbers of variables that aren't variadic like substr which can take 3-2.
                if(functionCall.getParameterCount() > ((BuiltInFunctionNode) function).getParameterCount()){
                    throw new Exception("Improper number of arguments to function call: " + functionCall.getFunctionName());
                }
                var map = new HashMap<String, InterpreterDataType>();
                var parameters = functionCall.getParameters();
                int index = 0;
                for(String parameter: function.getParameterNames()){
                    if(parameters.size()>index)
                        map.put(parameter, getIDT(parameters.get(index), localVariables));
                    index++;
                }
                return ((BuiltInFunctionNode) function).execute(map);
            }
        }else{
            //this is where user defined functions are dealt with.
            if(functionCall.getParameterCount() > function.getParameterCount()){
                throw new Exception("Improper number of arguments to function call: " + functionCall.getFunctionName());
            }
            var map = new HashMap<String, InterpreterDataType>();
            int index = 0;
            for(String parameter: function.getParameterNames()){
                map.put(parameter, getIDT(functionCall.getParameters().get(index), localVariables));
                index++;
            }
            return InterpretListOfStatements(function.getStatements(), map).getValue(); //performs the statements within the User Defined Function.
        }
    }

    private ReturnType ProcessStatement(HashMap<String, InterpreterDataType> locals, StatementNode stmt) throws Exception {
        if(stmt instanceof BreakNode){
            return new ReturnType(ReturnType.type.BREAK);
        }else if(stmt instanceof ContinueNode){
            return new ReturnType(ReturnType.type.CONTINUE);
        }else if(stmt instanceof DoWhileNode){
            //the condition of the loop is set this way because the booleans will return 0,
            //but a mathematical operation might return 0.0(at least in the current state of the interpreter, this will probably have to be addressed).
            do{
                ReturnType returnType = InterpretListOfStatements(((DoWhileNode) stmt).getStatements(), locals);
                //Here we account for the only two reasons we should exit the loop, if we encounter a break or a return value.
                if(returnType.getReturnType() == ReturnType.type.BREAK){
                    return new ReturnType(ReturnType.type.NONE);//we return NONE here because a break in a nested loop should not exit the whole loop.
                }else if(returnType.getReturnType() == ReturnType.type.RETURN){
                    return returnType;
                }
            }while(!getIDT(((DoWhileNode) stmt).getCondition(), locals).getValue().equals("0") && !getIDT(((DoWhileNode) stmt).getCondition(), locals).getValue().equals("0.0"));
        }else if(stmt instanceof WhileNode){
            //The While loop works identically to the do while loop, except in a while loop.
            while(!getIDT(((WhileNode) stmt).getCondition(), locals).getValue().equals("0") && !getIDT(((WhileNode) stmt).getCondition(), locals).getValue().equals("0.0")){
                ReturnType returnType = InterpretListOfStatements(((WhileNode) stmt).getStatements(), locals);
                if(returnType.getReturnType() == ReturnType.type.BREAK){
                    return new ReturnType(ReturnType.type.NONE);
                }else if(returnType.getReturnType() == ReturnType.type.RETURN){
                    return returnType;
                }
            }
        }else if(stmt instanceof ForEachNode){
            //The parameters of the forEach loop should be an OperationNode that looks like a IN array
            //where a and array are VariableReferenceNodes, so we check for that first.
            if(((ForEachNode) stmt).getForEach() instanceof OperationNode){
                if (((OperationNode) ((ForEachNode) stmt).getForEach()).getRight().isPresent()) {
                    if (((OperationNode) ((ForEachNode) stmt).getForEach()).getRight().get() instanceof VariableReferenceNode && ((OperationNode) ((ForEachNode) stmt).getForEach()).getLeft() instanceof VariableReferenceNode) {
                        VariableReferenceNode arrayReference = (VariableReferenceNode) ((OperationNode) ((ForEachNode) stmt).getForEach()).getRight().get();
                        //now that it's established that the format of the OperationNode is correct we can retrieve the array
                        //and confirm that it is in fact an array and not a normal variable.
                        if(globalVariables.containsKey(arrayReference.getName())) {
                            if (globalVariables.get(arrayReference.getName()) instanceof InterpreterArrayDataType) {
                                InterpreterArrayDataType arrayHolder = (InterpreterArrayDataType) globalVariables.get(arrayReference.getName());
                                var array = arrayHolder.getVariableArray();
                                var keys = array.keySet();
                                //here we loop over the keys and update and place the associated value inside the local and global variables
                                //mapped to the name of the variable on the left of the OperationNode and performing the statements in the loop
                                //each time the value of the variable is updated.
                                for (String key: keys) {
                                    var idt = array.get(key);
                                    var variable = (VariableReferenceNode)((OperationNode) ((ForEachNode) stmt).getForEach()).getLeft();
                                    if(locals!=null)
                                        locals.put(variable.getName(), idt);
                                    globalVariables.put(variable.getName(), idt);
                                    var returnType = InterpretListOfStatements(((ForEachNode) stmt).getStatements(), locals);
                                    if(returnType.getReturnType() == ReturnType.type.BREAK){
                                        return new ReturnType(ReturnType.type.NONE);
                                    }else if(returnType.getReturnType() == ReturnType.type.RETURN){
                                        return returnType;
                                    }
                                }
                            } else {
                                throw new Exception("attempted to reference variable as an array");
                            }
                        }else if(locals != null){ //the same as the first, but using locals instead.
                            if(locals.containsKey(arrayReference.getName())) {
                                if (locals.get(arrayReference.getName()) instanceof InterpreterArrayDataType) {
                                    InterpreterArrayDataType arrayHolder = (InterpreterArrayDataType) globalVariables.get(arrayReference.getName());
                                    var array = arrayHolder.getVariableArray();
                                    var keys = array.keySet();
                                    for (String key: keys) {
                                        var idt = array.get(key);
                                        var variable = (VariableReferenceNode)((OperationNode) ((ForEachNode) stmt).getForEach()).getLeft();
                                        locals.put(variable.getName(), idt);
                                        globalVariables.put(variable.getName(), idt);
                                        var returnType = InterpretListOfStatements(((ForEachNode) stmt).getStatements(), locals);
                                        if(returnType.getReturnType() == ReturnType.type.BREAK){
                                            return new ReturnType(ReturnType.type.NONE);
                                        }else if(returnType.getReturnType() == ReturnType.type.RETURN){
                                            return returnType;
                                        }
                                    }
                                } else {
                                    throw new Exception("attempted to reference variable as an array");
                                }
                            }else{
                                //so in the case that the array has not yet been initiated, AWK will initialize it.
                                locals.put(arrayReference.getName(), new InterpreterArrayDataType(new HashMap<>()));
                                globalVariables.put(arrayReference.getName(), new InterpreterArrayDataType(new HashMap<>()));
                                return new ReturnType(ReturnType.type.NONE);
                            }
                        }else{
                            globalVariables.put(arrayReference.getName(), new InterpreterArrayDataType(new HashMap<>()));
                            return new ReturnType(ReturnType.type.NONE);
                        }
                    }else{
                        throw new Exception("Invalid format of parameters in foreach loop, must be for(variable in array){}");
                    }
                }else{
                    throw new Exception("Missing array to iterate in foreach loop");
                }
            }else{
                throw new Exception("Error inside parameters of foreach loop");
            }
        }else if(stmt instanceof DeleteNode){
            //first we handle situations like delete a[1, 2, 4, 6] where we delete indexes of the array and not the entire array.
            if(((DeleteNode) stmt).getIndexes().isPresent()){
                if(locals!=null){
                    if(locals.containsKey(((DeleteNode) stmt).getName().getName())) {
                        //here we assert that the variable is referencing an array.
                        if (locals.get(((DeleteNode) stmt).getName().getName()) instanceof InterpreterArrayDataType) {
                            HashMap<String, InterpreterDataType> array = ((InterpreterArrayDataType) locals.get(((DeleteNode) stmt).getName().getName())).getVariableArray();
                            //here we loop over each index contained in the delete node and remove each one from the array.
                            for (ConstantNode index: ((DeleteNode) stmt).getIndexes().get()) {
                                array.remove(index.getValue());
                            }
                            ((InterpreterArrayDataType) locals.get(((DeleteNode) stmt).getName().getName())).setVariableArray(array);
                        } else {
                            throw new Exception("Delete must be called on an array");
                        }
                    }
                }
                //identical to the locals, but with globals.
                if(globalVariables.containsKey(((DeleteNode) stmt).getName().getName())){
                    if (globalVariables.get(((DeleteNode) stmt).getName().getName()) instanceof InterpreterArrayDataType) {
                        HashMap<String, InterpreterDataType> array = ((InterpreterArrayDataType) globalVariables.get(((DeleteNode) stmt).getName().getName())).getVariableArray();
                        for (ConstantNode index: ((DeleteNode) stmt).getIndexes().get()) {
                            array.remove(index.getValue());
                        }
                        ((InterpreterArrayDataType) globalVariables.get(((DeleteNode) stmt).getName().getName())).setVariableArray(array);
                    } else {
                        throw new Exception("Delete must be called on an array");
                    }
                }
                //in the situation that the array has not been initialized yet, awk will go ahead and initialize it as an array.
                //we handle that here and directly below.
                if(!globalVariables.containsKey(((DeleteNode) stmt).getName().getName()) || locals!=null){
                    if(locals!=null){
                        if(!locals.containsKey(((DeleteNode) stmt).getName().getName())){
                            locals.put(((DeleteNode) stmt).getName().getName(), new InterpreterArrayDataType(new HashMap<>()));
                            globalVariables.put(((DeleteNode) stmt).getName().getName(), new InterpreterArrayDataType(new HashMap<>()));
                        }else{
                            globalVariables.put(((DeleteNode) stmt).getName().getName(), new InterpreterArrayDataType(new HashMap<>()));
                        }
                    }else{
                        globalVariables.put(((DeleteNode) stmt).getName().getName(), new InterpreterArrayDataType(new HashMap<>()));
                    }
                }
            }else{
                if(locals!=null){
                    locals.remove(((DeleteNode) stmt).getName().getName());
                }
                globalVariables.remove(((DeleteNode) stmt).getName().getName());
            }
            return new ReturnType(ReturnType.type.NONE);
        }else if(stmt instanceof ForNode){
            //intes are pretty straight forward, first the statement is evaluated if there is one,
            //than if the condition is present we use it as the condition of the while loop,
            //if it is not present we treat it as an infinite loop.
            if(((ForNode) stmt).getLeft().isPresent()){
                getIDT(((ForNode) stmt).getLeft().get(), locals);
            }
            String a;
            if(((ForNode) stmt).getMiddle().isPresent()){
                while(!getIDT(((ForNode) stmt).getMiddle().get(), locals).getValue().equals("0") && !getIDT(((ForNode) stmt).getMiddle().get(), locals).getValue().equals("0.0")){
                    ReturnType returnType = InterpretListOfStatements(((ForNode) stmt).getStatements(), locals);
                    if(returnType.getReturnType() == ReturnType.type.BREAK){
                        return new ReturnType(ReturnType.type.NONE);
                    }else if(returnType.getReturnType() == ReturnType.type.RETURN){
                        return returnType;
                    }
                    //if the increment is present, we evaluate it.
                    if(((ForNode) stmt).getRight().isPresent()){
                        getIDT(((ForNode) stmt).getRight().get(), locals);
                    }
                }
            }else{
                while(true){
                    ReturnType returnType = InterpretListOfStatements(((ForNode) stmt).getStatements(), locals);
                    if(returnType.getReturnType() == ReturnType.type.BREAK){
                        return new ReturnType(ReturnType.type.NONE);
                    }else if(returnType.getReturnType() == ReturnType.type.RETURN){
                        return returnType;
                    }
                    if(((ForNode) stmt).getRight().isPresent()){
                        getIDT(((ForNode) stmt).getRight().get(), locals);
                    }
                }
            }

        }else if(stmt instanceof ReturnNode){
            return new ReturnType(ReturnType.type.RETURN, getIDT(((ReturnNode) stmt).getValue(), locals).getValue());
        }else if(stmt instanceof IfNode){
            //Since the IfNode is set up as a linked list we can crawl through it and evaluate the condition.
            while(stmt != null){
                if(((IfNode) stmt).getCondition().isPresent()){
                    if(!getIDT(((IfNode) stmt).getCondition().get(), locals).getValue().equals("0") && !getIDT(((IfNode) stmt).getCondition().get(), locals).getValue().equals("0.0")){
                        var returnType = InterpretListOfStatements(((IfNode) stmt).getStatements(), locals);
                        if(returnType.getReturnType() != ReturnType.type.NONE){
                            return returnType;
                        }else{
                            return new ReturnType(ReturnType.type.NONE);
                        }
                    }
                }else{ //this will run if we encounter a IfNode that does not have a condition, which would be an else.
                    var returnType = InterpretListOfStatements(((IfNode) stmt).getStatements(), locals);
                    if(returnType.getReturnType() != ReturnType.type.NONE){
                        return returnType;
                    }else{
                        return new ReturnType(ReturnType.type.NONE);
                    }
                }
                stmt = ((IfNode)stmt).getNext(); //here is where we crawl to the next IfNode in the list
            }
            return new ReturnType(ReturnType.type.NONE);
        }else if(stmt instanceof AssignmentNode){
            getIDT(stmt, locals);
            return new ReturnType(ReturnType.type.NONE);
        }else if(stmt instanceof FunctionCallNode){
            return new ReturnType(ReturnType.type.NONE, getIDT(stmt, locals).getValue());
        }else{
            throw new Exception("Encountered Syntax Error: Encountegsred instance of " + stmt.getClass() + " in improper location");
        }
        return new ReturnType(ReturnType.type.NONE);
    }

    private ReturnType InterpretListOfStatements(LinkedList<StatementNode> statements, HashMap<String, InterpreterDataType> locals) throws Exception {
        //here we loop through a linked list of statements passed into the method calling ProcessStatement on each.
        for (StatementNode statement: statements) {
            var returnType = ProcessStatement(locals, statement);
            //if the return type is Continue, Break, or Return we will return that and break out of the loop.
            if(returnType.getReturnType() != ReturnType.type.NONE){
                return returnType;
            }
        }
        return new ReturnType(ReturnType.type.NONE);
    }


    Function<HashMap<String, InterpreterDataType>, String> lengthLambda = (map) -> {
        return String.valueOf(map.get("string").getValue().length());
    };
    Function<HashMap<String, InterpreterDataType>, String> toUpperLambda = (map) -> {
        return map.get("string").getValue().toUpperCase();
    };
    Function<HashMap<String, InterpreterDataType>, String> toLowerLambda = (map) -> {
        return map.get("string").getValue().toLowerCase();
    };
    Function<HashMap<String, InterpreterDataType>, String> subStrLambda = (map) -> {
        String string;
        if(globalVariables.containsKey(map.get("string").getValue())){
            string = globalVariables.get(map.get("string").getValue()).getValue();
        }else
            string = map.get("string").getValue();

        if(map.containsKey("length")){
            //the -1s here account for AWK starting at 1 for file indexes.
            return  string.substring(Integer.parseInt(map.get("start").getValue())-1, Integer.parseInt(map.get("length").getValue())-1);
        }else{
            return string.substring(Integer.parseInt(map.get("start").getValue())-1);
        }
    };
    Function<HashMap<String, InterpreterDataType>, String> subLambda = (map) -> {
        //this if statement checks to see what form of sub we are interpreting.
        if(map.containsKey("target")) {
            String str;
            if(globalVariables.containsKey(map.get("target").getValue())){
                str = globalVariables.get(map.get("target").getValue()).getValue();
            }else
                str = map.get("target").getValue();

            String replacement;
            if(globalVariables.containsKey(map.get("replacement").getValue())){
                replacement = globalVariables.get(map.get("replacement").getValue()).getValue();
            }else
                replacement = map.get("replacement").getValue();

            String regex;
            if(globalVariables.containsKey(map.get("regex").getValue())){
                regex = globalVariables.get(map.get("regex").getValue()).getValue();
            }else
                regex = map.get("regex").getValue();

            return str.replaceFirst(regex, replacement);
        }else{
            String replacement;
            if(globalVariables.containsKey(map.get("replacement").getValue())){
                replacement = globalVariables.get(map.get("replacement").getValue()).getValue();
            }else
                replacement = map.get("replacement").getValue();

            String regex;
            if(globalVariables.containsKey(map.get("regex").getValue())){
                regex = globalVariables.get(map.get("regex").getValue()).getValue();
            }else
                regex = map.get("regex").getValue();
            globalVariables.put("$0", new InterpreterDataType(globalVariables.get("$0").getValue().replaceFirst(regex, replacement)));
            return globalVariables.get("$0").getValue();
        }
    };
    Function<HashMap<String, InterpreterDataType>, String> sprintfLambda = (map) -> {
        ArrayList<String> arguments = new ArrayList<>();
        int index = 1;
        //this loop collects all the arguments into an array, so they can be used in String.format().
        while(map.containsKey("" + index)){
            if(globalVariables.containsKey(map.get("" + index).getValue())){
                arguments.add(globalVariables.get(map.get("" + index).getValue()).getValue());
            }else{
                arguments.add(map.get("" + index).getValue());
            }
            index++;
        }
        return String.format(map.get("0").getValue(), arguments.toArray());
    };
    Function<HashMap<String, InterpreterDataType>, String> splitLambda = (map) -> {
        String str;
        if(globalVariables.containsKey(map.get("string").getValue())){
            str = globalVariables.get(map.get("string").getValue()).getValue();
        }else{
            str = map.get("string").getValue();
        }

        String separator;
        if(globalVariables.containsKey(map.get("fieldsep").getValue())){
            separator = globalVariables.get(map.get("fieldsep").getValue()).getValue();
        }else{
            separator = map.get("fieldsep").getValue();
        }
        String[] split = str.split(separator);
        String arrayName = map.get("array").getValue();
        var array = new HashMap<String, InterpreterDataType>();
        int index = 1;
        for(String string: split){
            array.put(""+index, new InterpreterDataType(string));
            index++;
        }
        globalVariables.put(arrayName, new InterpreterArrayDataType(array));
        return String.valueOf(split.length);
    };
    Function<HashMap<String, InterpreterDataType>, String> matchLambda = (map) -> {
        String str;
        if(globalVariables.containsKey(map.get("string").getValue())){
            str = globalVariables.get(map.get("string").getValue()).getValue();
        }else{
            str = map.get("string").getValue();
        }

        String match;
        if(globalVariables.containsKey(map.get("regex").getValue())){
            match = globalVariables.get(map.get("regex").getValue()).getValue();
        }else{
            match = map.get("regex").getValue();
        }
        //lastIndexOf() returns -1 if it never encounters the pattern.
        if(str.lastIndexOf(match)==-1) {
            return "0";
        }else{
            return String.valueOf(str.lastIndexOf(match) + 1);
        }
    };
    Function<HashMap<String, InterpreterDataType>, String> indexLambda = (map) -> {
        String str;
        if(globalVariables.containsKey(map.get("in").getValue())){
            str = globalVariables.get(map.get("in").getValue()).getValue();
        }else{
            str = map.get("in").getValue();
        }

        String index;
        if(globalVariables.containsKey(map.get("find").getValue())){
            index = globalVariables.get(map.get("find").getValue()).getValue();
        }else{
            index = map.get("find").getValue();
        }
        //indexOf() returns -1 if it never encounters the pattern.
        if(str.indexOf(index)==-1) {
            return "0";
        }else{
            return String.valueOf(str.indexOf(index) + 1);
        }
    };
    Function<HashMap<String, InterpreterDataType>, String> gsubLambda = (map) -> {
        //this if statement checks to see if we were given a string to work with
        if(map.containsKey("target")) {
            if(globalVariables.containsKey(map.get("target").getValue())){
                String str = globalVariables.get(map.get("target").getValue()).getValue();
                globalVariables.put(map.get("target").getValue(), new InterpreterDataType(str.replaceAll(map.get("regex").getValue(), map.get("replacement").getValue())));
                return globalVariables.get(map.get("target").getValue()).getValue();
            }else
                return map.get("target").getValue().replaceAll(map.get("regex").getValue(), map.get("replacement").getValue());
        }else{
            String str = globalVariables.get("$0").getValue();
            globalVariables.put("$0", new InterpreterDataType(str.replaceAll(map.get("regex").getValue(), map.get("replacement").getValue())));
            return globalVariables.get("$0").getValue();
        }
    };
    Function<HashMap<String, InterpreterDataType>, String> nextLambda = (map) -> {
        if(lineManager.splitAndAssign())
            return "1";
        else
            return "-1";
    };
    Function<HashMap<String, InterpreterDataType>, String> getlineLambda = (map) -> {
        //if split and assign is successful a 1 is returned.
        if(lineManager.splitAndAssign())
            return "1";
        else
            return "-1";
    };
    Function<HashMap<String, InterpreterDataType>, String> printLambda = (map) -> {
        if(globalVariables.containsKey(map.get("string").getValue())){
            String field = globalVariables.get(map.get("string").getValue()).getValue();
            System.out.print(field);
        }else {
            System.out.println(map.get("string").getValue());
        }
        return "";
    };
    Function<HashMap<String, InterpreterDataType>, String> printfLambda = (map) -> {
        ArrayList<String> arguments = new ArrayList<>();
        int index = 1;
        //a while loop identical to the one used for sprintf(), it collects the arguments for use in String.format();
        while(map.containsKey("" + index)){
            if(globalVariables.containsKey(map.get("" + index).getValue())){
                arguments.add(globalVariables.get(map.get("" + index).getValue()).getValue());
            }else{
                arguments.add(map.get("" + index).getValue());
            }
            index++;
        }
        String format = map.get("0").getValue();
        System.out.printf(format, arguments.toArray());
        return "";
    };

}
