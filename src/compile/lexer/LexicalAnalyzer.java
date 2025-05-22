package src.compile.lexer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author sixteacher
 * @version 1.0
 * @description LexicalAnalyzer
 * @date 2025/5/18
 */


public class LexicalAnalyzer {
    public List<Token> lexicalAnalyze(String filePath) {
        List<Token> tokens = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            Lexer lexer = new Lexer(br);
            Token token;
            while ((token = lexer.nextToken()).type != TokenType.EOF) {
                if (token.type == TokenType.ERROR) {
                    errors.add(String.format("Lex Error: '%s' at %d:%d",
                            token.value, token.line, token.pos));
                } else {
                    tokens.add(token);
                }
            }
            tokens.add(token); // EOF
        } catch (IOException e) {
            errors.add("IO Error: " + e.getMessage());
        }
        if (!errors.isEmpty()) {
            errors.forEach(System.err::println);
            return null;
        }
        return tokens;
    }

}
