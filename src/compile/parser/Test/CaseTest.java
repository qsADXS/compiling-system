package src.compile.parser.Test;

import src.compile.lexer.LexicalAnalyzer;
import src.compile.lexer.Token;
import src.compile.parser.LR1Parser;

import java.util.List;

/**
 * @author sixteacher
 * @version 1.0
 * @description CaseTest
 * @date 2025/5/19
 */


public class CaseTest {
    public static void main(String[] args) {
        String path = "src/compile/input/Test5.txt";
        LexicalAnalyzer lexicalAnalyzer = new LexicalAnalyzer();
        List<Token> tokens = lexicalAnalyzer.lexicalAnalyze(path);
        LR1Parser lr1Parser = new LR1Parser(tokens);
        lr1Parser.parse();
    }

}
