import java.io.FileNotFoundException;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {

        // feel free to try out other examples, or your own grammar (must be LL(1) grammar)
        String grammarFile = "Forditoprogramok/ExampleGrammar3.txt";
        String word = "acdb";

        LL1Parser parser = new LL1Parser(grammarFile, word);
        parser.buildParseTable(); // must be built before calling analyze
        parser.drawParseTable();
        parser.analyze();
    }
}
