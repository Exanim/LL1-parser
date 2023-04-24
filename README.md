
# LL(1) Parser
This is a simple LL(1) parser that can parse grammars that are LL(1).

## Usage
To use this parser, you can follow these steps:

 
Create an instance of the LL1Parser class and pass in your grammar file as well as the word you want to analyze.
Example:
	
    String grammarFile = "ExampleGrammar3.txt";
    String word = "acdb";
    LL1Parser parser = new LL1Parser(grammarFile, word);
    
Call the BuildParseTable method on the parser instance.

    parser.buildParseTable();
    parser.drawParseTable();
    parser.analyze();

The parse table from example3:

![parse table](https://raw.githubusercontent.com/Exanim/LL1-parser/main/Screenshot_215.png)

## Limitations
This parser is limited to LL(1) grammars only. If your grammar is not LL(1), this parser will not be able to parse it correctly.
