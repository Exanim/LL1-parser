import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class LL1Parser {

    private final TreeMap<String, ArrayList<String>> grammar = new TreeMap<>();
    private String startingSymbol;
    private final Set<String> terminals;
    private final Set<String> nonTerminals;
    private final String wordToAnalyze;
    private final Map<String, Map<String, String>> parseTable;
    public static TreeMap<String,ArrayList<NumberedProduction>> numberedGrammar = new TreeMap<>();
    public static class NumberedProduction {
        private final String variation;
        private final Integer number;

        public NumberedProduction(String l, Integer r) {
            this.variation = l;
            this.number = r;
        }

        @Override
        public java.lang.String toString() {
            return "(" + variation +", " + number +")";
        }
    }

    public LL1Parser(String grammarFile, String wordToAnalyze) throws FileNotFoundException {
        readGrammar(grammarFile, wordToAnalyze);
        this.wordToAnalyze = wordToAnalyze;
        this.terminals = new HashSet<>();
        this.nonTerminals = new HashSet<>(grammar.keySet());
        this.parseTable = new HashMap<>();
        calculateTerminals();
    }

    public void readGrammar(String grammarFile, String word) throws FileNotFoundException {

        File inputFile = new File(grammarFile);
        Scanner fileScanner = new Scanner(inputFile);

        startingSymbol = fileScanner.nextLine();
        fileScanner.nextLine(); // jumping over terminals
        fileScanner.nextLine(); // jumping over nonTerminals

        while (fileScanner.hasNextLine()) {
            String line = fileScanner.nextLine();

            String[] parts = line.split("\\s+");
            String key = parts[0];
            ArrayList<String> values = new ArrayList<>(Arrays.asList(parts).subList(1, parts.length));
            grammar.put(key, values);
        }
        convertGrammarToNumberedGrammar(grammar);
    }

    public void convertGrammarToNumberedGrammar(TreeMap<String,ArrayList<String>> grammar) {
        int variationCounter = 0;
        for (String key : grammar.keySet()) {
            ArrayList<String> values = grammar.get(key);
            ArrayList<NumberedProduction> tmpRightSide = new ArrayList<>();
            for (int i = 0; i < values.size(); i++) {
                variationCounter++;
                NumberedProduction tmpNumberedVariation = new NumberedProduction(values.get(i), i);
                tmpRightSide.add(i, tmpNumberedVariation);
            }
            numberedGrammar.put(key, tmpRightSide);
        }

    }

    private void calculateTerminals() {
        for (var rule : grammar.entrySet()) {
            String nonTerminal = rule.getKey();
            ArrayList<String> productions = rule.getValue();
            for (String production : productions) {
                for (int i = 0; i < production.length(); i++) {
                    char symbol = production.charAt(i);
                    if (!Character.isUpperCase(symbol)) {
                        terminals.add(Character.toString(symbol));
                    }
                }
            }
        }
    }

    private Set<String> calculateFirst(String production) {
        Set<String> firstSet = new HashSet<>();
        if (production.isEmpty()) {
            firstSet.add("/");
            return firstSet;
        }
        char symbol = production.charAt(0);
        if (Character.isUpperCase(symbol)) {
            ArrayList<String> productions = grammar.get(Character.toString(symbol));
            for (String p : productions) {
                Set<String> pFirstSet = calculateFirst(p);
                firstSet.addAll(pFirstSet);
            }
            if (firstSet.contains("/")) {
                firstSet.remove("/");
                firstSet.addAll(calculateFirst(production.substring(1)));
            }
        } else {
            firstSet.add(Character.toString(symbol));
        }
        return firstSet;
    }

    private Set<String> calculateFollow(String nonTerminal) {
        Set<String> followSet = new HashSet<>();
        if (nonTerminal.equals(startingSymbol)) {
            followSet.add("$");
        }
        for (var rule : grammar.entrySet()) {
            String leftHandSide = rule.getKey();
            ArrayList<String> productions = rule.getValue();
            for (String production : productions) {
                int index = production.indexOf(nonTerminal);
                if (index >= 0 && index < production.length() - 1) {
                    Set<String> firstSet = calculateFirst(production.substring(index + 1));
                    followSet.addAll(firstSet);
                    if (firstSet.contains("/")) {
                        followSet.remove("/");
                        followSet.addAll(calculateFollow(leftHandSide));
                    }
                } else if (index == production.length() - 1) {
                    followSet.addAll(calculateFirst(leftHandSide));
                }
            }
        }
        return followSet;
    }

    public void buildParseTable() {
        // making a list out of the non-terminals so it can be sorted
        List<String> nonTerminalsList = new ArrayList<>(grammar.keySet());

        // putting the starting symbol first
        Collections.sort(nonTerminalsList);
        nonTerminalsList.remove(startingSymbol);
        nonTerminalsList.add(0, startingSymbol);

        for (String nonTerminal : nonTerminalsList) {
            Map<String, String> row = new HashMap<>();
            for (String terminal : terminals) {
                if (grammar.get(nonTerminal).contains(terminal)) {
                    row.put(terminal, terminal);
                } else {
                    Set<String> followSet = calculateFollow(nonTerminal);
                    for (String production : grammar.get(nonTerminal)) {
                        Set<String> firstSet = calculateFirst(production);
                        if (firstSet.contains(terminal) || (firstSet.contains("/")
                                && followSet.contains(terminal))) {
                            row.put(terminal, production);
                        }
                    }
                }
            }
            parseTable.put(nonTerminal, row);
        }
    }
// TODO : Show parsing tree if the grammar produces the given word
    public void analyze() {
        List<String> inputTokens = Arrays.asList(wordToAnalyze.split(""));

        // Initialize stack with starting symbol
        Stack<String> stack = new Stack<>();
        stack.push("$");
        stack.push(startingSymbol);

        int inputIndex = 0;
        String currentInputToken = inputTokens.get(inputIndex);

        while (!(Objects.equals(stack.peek(), "$") && stack.size() == 1)) {
            String stackTop = stack.peek();

            // If stack top is a terminal, compare with input token
            if (terminals.contains(stackTop)) {
                if (stackTop.equals(currentInputToken)) {
                    // Match found, move to next input token
                    inputIndex++;
                    if (inputIndex < inputTokens.size()) {
                        currentInputToken = inputTokens.get(inputIndex);
                    } else {
                        currentInputToken = "$";
                    }
                    stack.pop();
                } else {
                    if (currentInputToken.equals("$")) {
                        System.out.println("Grammar does not produce \"" + wordToAnalyze + "\"");
                        return;
                    }
                    System.out.println("Error: unexpected token " + currentInputToken);
                    return;
                }
            }
            // If stack top is a non-terminal, look up in parsing table
            else if (nonTerminals.contains(stackTop)) {
                Map<String, String> row = parseTable.get(stackTop);
                String production = row.get(currentInputToken);
                if (production != null) {
                    // Apply production to stack
                    stack.pop();
                    if (!production.equals("/")) {
                        String[] symbols = production.split("");
                        for (int i = symbols.length - 1; i >= 0; i--) {
                            stack.push(symbols[i]);
                        }
                    }
                } else {
                    System.out.println("Error: unexpected token " + currentInputToken + " for non-terminal " + stackTop);
                    return;
                }
            }
        }

        System.out.println("Grammar produces \"" + wordToAnalyze + "\"");
    }

    public void drawParseTable() {
        int nonTerminalWidth = Math.max(5, nonTerminals.stream().mapToInt(String::length).max().orElse(0));
        int terminalWidth = Math.max(8, terminals.stream().mapToInt(String::length).max().orElse(0));

        // Draw the top row of the table
        System.out.printf("%-" + nonTerminalWidth + "s |", "");
        for (String terminal : terminals) {
            System.out.printf("%-" + terminalWidth + "s |", terminal);
        }
        System.out.println();

        // Draw a line separator
        System.out.print("-".repeat(nonTerminalWidth) + "-+");
        for (int i = 0; i < terminals.size(); i++) {
            System.out.print("-".repeat(terminalWidth+1) + "+");
        }
        System.out.println();

        // Draw the body of the table
        ArrayList<String> sortedNonTerminals = new ArrayList<>();
        sortedNonTerminals.addAll(nonTerminals);
        Collections.sort(sortedNonTerminals);
        sortedNonTerminals.remove(startingSymbol);
        sortedNonTerminals.add(0, startingSymbol);
        for (String nonTerminal : sortedNonTerminals) {
            System.out.printf("%-" + nonTerminalWidth + "s |", nonTerminal);
            Map<String, String> row = parseTable.get(nonTerminal);
            for (String terminal : terminals) {
                String value = row.get(terminal);
                System.out.printf("%-" + terminalWidth + "s |", value != null ? value : "");
            }
            System.out.println();
        }
    }
}