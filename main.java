import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Optional;

public class main {
    public static void main(String []args) {
        //a try catch to catch and print readable error messages

        try {
            Path myPath = Paths.get(args[0]);
            String content = new String(Files.readAllBytes (myPath));
            Lexer lexer = new Lexer();
            LinkedList<Token> output = lexer.Lex(content);
            Parser myParse = new Parser(output);
            ProgramNode node = myParse.Parse();
            Interpreter myInter = new Interpreter(args[1], " ", node);
            myInter.interpretProgram();
        }catch(Exception e){
            System.out.println("An Error Has Occurred\n" + e);
        }
    }
}
