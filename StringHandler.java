public class StringHandler {
    private String awkFile;
    private int index = 0;

    public StringHandler(String file){
        awkFile = file;
    }
    public char Peek(int i){
        //looks “i” characters ahead and returns that character; doesn’t move the index
        return awkFile.charAt(index+i);
    }
    public String PeekString(int i){
        //returns a string of the next “i” characters but doesn’t move the index
        return awkFile.substring(index,index+i);
    }
    public char GetChar(){
        //returns the next character and moves the index
        char retVal = awkFile.charAt(index);
        if(index<awkFile.length()) {
            index++;
        }
        return retVal;
    }
    public void Swallow(int i){
        //moves the index ahead “i” positions
        index += i;
    }
    public boolean IsDone(){
        //returns true if we are at the end of the document
        if(index >= awkFile.length()){
            return true;
        }else{
            return false;
        }
    }
    public String Remainder(){
        //returns the rest of the document as a string
        return awkFile.substring(index);
    }
}
