package src.compile.lexer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

public class Lexer {
    private BufferedReader reader;
    private int line = 1;
    private int col = 1; // 当前列号，从 1 开始

    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();
    static {
        // Type Keywords
        KEYWORDS.put("boolean", TokenType.TYPE);
        KEYWORDS.put("char", TokenType.TYPE);
        KEYWORDS.put("byte", TokenType.TYPE);
        KEYWORDS.put("short", TokenType.TYPE);
        KEYWORDS.put("int", TokenType.TYPE);
        KEYWORDS.put("long", TokenType.TYPE);
        KEYWORDS.put("float", TokenType.TYPE);
        KEYWORDS.put("double", TokenType.TYPE);
        KEYWORDS.put("void", TokenType.TYPE);
    }

    public Lexer(Reader in) {
        this.reader = new BufferedReader(in, 1024);
    }

    public Token nextToken() throws IOException {
        Token tok = nextTokenInternal();
        tok.parseSpecific();
        return tok;
    }

    private Token nextTokenInternal() throws IOException {
        skipWhitespaceAndComments(); // 执行后，line 和 col 指向 token 的起始位置

        final int tokenStartLine = line;   // 捕获 token 的起始行号
        final int tokenStartCol = col;     // 捕获 token 的起始列号

        int ch_int = nextChar(); // 读取 token 的第一个字符，这会更新全局的 line 和 col
        if (ch_int == -1) {
            return new Token(TokenType.EOF, "", tokenStartLine, tokenStartCol);
        }
        char c = (char) ch_int;

        // 使用 tokenStartLine 和 tokenStartCol 创建 Token
        if (c == '"') return readString(tokenStartLine, tokenStartCol);
        if (c == '\'') return readChar(tokenStartLine, tokenStartCol);
        if (Character.isJavaIdentifierStart(c)) return readWordOrKeyword(c, tokenStartLine, tokenStartCol);
        if (Character.isDigit(c)) return readNumber(c, tokenStartLine, tokenStartCol);

        String op = String.valueOf(c);
        // peekCharAsString 和 peek(2) 不会改变全局的 line/col
        String two = peekCharAsString() != null ? op + peekCharAsString() : null;
        String three = (two != null && peek(2) != null) ? op + peek(2) : null;


        if (three != null && isOperator(three)) {
            nextChar(); nextChar(); // 消耗掉 op 后的两个字符
            return new Token(TokenType.OPERATOR, three, tokenStartLine, tokenStartCol);
        }
        if (two != null && isOperator(two)) {
            nextChar(); // 消耗掉 op 后的一个字符
            return new Token(TokenType.OPERATOR, two, tokenStartLine, tokenStartCol);
        }
        if (isOperator(op)) {
            // 第一个字符 c 已经被消耗
            return new Token(TokenType.OPERATOR, op, tokenStartLine, tokenStartCol);
        }
        if (isDelimiter(op)) { // op 就是 String.valueOf(c)
            // 第一个字符 c 已经被消耗
            return new Token(TokenType.DELIMITER, op, tokenStartLine, tokenStartCol);
        }

        // 处理无法识别的字符 c。
        // 返回一个 ERROR 类型的 Token，包含无法识别的字符。
        return new Token(TokenType.ERROR, String.valueOf(c), tokenStartLine, tokenStartCol);
    }

    private void skipWhitespaceAndComments() throws IOException {
        while (true) {
            int markedLine = line;   // 在读取前保存 line 和 col
            int markedCol = col;
            reader.mark(3); // 标记当前读取器位置，允许预读最多3个字符（例如用于判断 /*/）

            int ch_val = nextChar(); // 读取一个字符，nextChar 会更新全局的 line 和 col
            if (ch_val == -1) { // 到达文件末尾
                return;
            }
            char c = (char) ch_val;

            if (Character.isWhitespace(c)) {
                continue;
            }

            if (c == '/') {
                int markedLineAfterFirstSlash = line;
                int markedColAfterFirstSlash = col;

                int nxt_val = nextChar();

                if (nxt_val == '/') {
                    while (true) {
                        int comment_char = nextChar();
                        if (comment_char == -1 || comment_char == '\n') {
                            break;
                        }
                    }
                    continue;
                } else if (nxt_val == '*') {
                    boolean closed = false;
                    int commentStartLine = markedLineAfterFirstSlash; // /* 开始的行
                    int commentStartCol = markedColAfterFirstSlash -1; // /* 开始的列 ( / 的位置)
                    while (true) {
                        int comment_char = nextChar();
                        if (comment_char == -1) { // 未关闭的注释，到达文件末尾
                            // 报告错误：未关闭的块注释
                            // 可以在这里创建一个ERROR Token或者直接打印错误
                            // 为了简单起见，我们先在main函数中处理
                            System.err.printf("Lexical Error: Unclosed block comment starting at %d:%d%n", commentStartLine, commentStartCol);
                            break;
                        }
                        if (comment_char == '*') {
                            int markLineBeforePotentialSlash = line;
                            int markColBeforePotentialSlash = col;
                            reader.mark(1);

                            int charAfterStar = nextChar();
                            if (charAfterStar == '/') {
                                closed = true;
                                break;
                            } else {
                                reader.reset();
                                line = markLineBeforePotentialSlash;
                                col = markColBeforePotentialSlash;
                            }
                        }
                    }
                    if (!closed) {
                        // 也可以在这里处理未关闭注释的错误，例如返回一个特殊的Token
                        // 或者依赖于上面的EOF检测
                    }
                    continue;
                } else {
                    reader.reset();
                    line = markedLine;
                    col = markedCol;
                    return;
                }
            }
            reader.reset();
            line = markedLine;
            col = markedCol;
            return;
        }
    }

    private Token readString(int tokenLine, int tokenCol) throws IOException {
        StringBuilder sb = new StringBuilder();
        boolean closed = false;
        while (true) {
            int ch = nextChar();
            if (ch == -1 || ch == '\n') { // 错误: 未关闭的字符串或字符串跨行
                // 可以在这里决定是否允许字符串跨行，如果不允许，ch == '\n'也是错误
                System.err.printf("Lexical Error: Unclosed string literal at %d:%d. String starts with: \"%s...%n", tokenLine, tokenCol, sb.substring(0, Math.min(sb.length(), 10)));
                return new Token(TokenType.ERROR, sb.toString(), tokenLine, tokenCol); // 返回错误 Token
            }
            if (ch == '"') {
                closed = true;
                break;
            }
            if (ch == '\\') {
                sb.append((char) ch);
                int next_ch = nextChar();
                if (next_ch == -1) { // 反斜杠后是EOF, 错误
                    System.err.printf("Lexical Error: EOF after escape character in string literal at %d:%d.%n", line, col-1); // col-1 因为nextChar已经移动
                    return new Token(TokenType.ERROR, sb.toString(), tokenLine, tokenCol);
                }
                // 简单处理，直接添加转义后的字符
                sb.append((char)next_ch);
            } else {
                sb.append((char) ch);
            }
        }
        // 如果closed为false，意味着是因为EOF或换行符退出的循环，已在循环内处理
        return new Token(TokenType.STRING, sb.toString(), tokenLine, tokenCol);
    }

    private Token readChar(int tokenLine, int tokenCol) throws IOException {
        StringBuilder sb = new StringBuilder();
        int ch1 = nextChar();
        if (ch1 == -1) {
            System.err.printf("Lexical Error: EOF in char literal at %d:%d.%n", tokenLine, tokenCol);
            return new Token(TokenType.ERROR, "", tokenLine, tokenCol);
        }

        if (ch1 == '\'') { // 处理空字符 '' 的情况，通常是错误的
            System.err.printf("Lexical Error: Empty char literal at %d:%d.%n", tokenLine, tokenCol);
            return new Token(TokenType.ERROR, "", tokenLine, tokenCol);
        }


        if (ch1 == '\\') {
            sb.append((char) ch1);
            int ch2 = nextChar();
            if (ch2 == -1) {
                System.err.printf("Lexical Error: EOF after escape character in char literal at %d:%d.%n", line, col-1);
                return new Token(TokenType.ERROR, sb.toString(), tokenLine, tokenCol);
            }
            // 简单处理，直接添加转义后的字符
            sb.append((char) ch2);
        } else {
            sb.append((char) ch1);
        }

        int closingQuote = nextChar();
        if (closingQuote != '\'') {
            // 可能是字符字面量过长，或未正确闭合
            String errVal = sb.toString();
            // 尝试读取直到找到单引号或换行/EOF，以包含更多错误信息
            if (closingQuote != -1 && closingQuote != '\n') {
                errVal += (char)closingQuote;
            }
            System.err.printf("Lexical Error: Unclosed or malformed char literal at %d:%d. Started with: '%s%n", tokenLine, tokenCol, errVal);
            return new Token(TokenType.ERROR, errVal, tokenLine, tokenCol);
        }
        // 检查字符字面量是否过长 (例如 '\ab')
        if (sb.length() > 2 || (sb.length() == 1 && sb.charAt(0) == '\\')) { // 例如 '\' 单独存在
            System.err.printf("Lexical Error: Malformed char literal (too long or invalid escape) '%s' at %d:%d.%n", sb.toString(), tokenLine, tokenCol);
            return new Token(TokenType.ERROR, sb.toString(), tokenLine, tokenCol);
        }


        return new Token(TokenType.CHAR, sb.toString(), tokenLine, tokenCol);
    }

    private Token readWordOrKeyword(char firstChar, int tokenLine, int tokenCol) throws IOException {
        // firstChar 已经被 nextTokenInternal 中的 initial nextChar() 消耗
        StringBuilder sb = new StringBuilder();
        sb.append(firstChar);
        while (true) {
            Integer p = peekChar(); // peekChar 不改变全局 line/col
            if (p == null) break;
            char c_peek = (char)(int)p;
            if (Character.isJavaIdentifierPart(c_peek)) {
                sb.append((char) nextChar()); // 消耗字符, 更新全局 line/col
            } else {
                break;
            }
        }
        String s = sb.toString();
        // 原始逻辑判断是 KEYWORD 还是 IDENTIFIER，Token 的 parseSpecific 会进一步细化
        if (KEYWORDS.containsKey(s)) {
            return new Token(TokenType.TYPE, s, tokenLine, tokenCol);
        }

        return new Token(Token.KEYWORDS.contains(s) ? TokenType.RESERVED : TokenType.IDENTIFIER, s, tokenLine, tokenCol);
    }

    private Token readNumber(char firstChar, int tokenLine, int tokenCol) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(firstChar);
        boolean isFloat = false;
        boolean isHex = false;
        boolean hasDecimalPoint = false; // 标记是否已经遇到小数点

        // 检查十六进制前缀
        if (firstChar == '0') {
            Integer p = peekChar();
            if (p != null && (Character.toLowerCase((char)(int)p) == 'x')) {
                isHex = true;
                sb.append((char)nextChar()); // 消耗 'x' 或 'X'
            }
        }

        while (true) {
            Integer p = peekChar();
            if (p == null) break;
            char c_peek = (char)(int)p;

            if (Character.isDigit(c_peek)) {
                sb.append((char)nextChar());
            } else if (isHex && isHexDigit(c_peek)) {
                sb.append((char)nextChar());
            } else if (!isHex && c_peek == '.' && !hasDecimalPoint) { // 非十六进制数，且是第一个小数点
                isFloat = true;
                hasDecimalPoint = true;
                sb.append((char)nextChar()); // 消耗 '.'
                // 处理类似 "1." 后直接跟非数字的情况，例如 "1.toString()"
                // 此时 "1." 应该是一个浮点数
                Integer charAfterDot = peekChar();
                if (charAfterDot == null || !Character.isDigit((char)(int)charAfterDot)) {
                    // 如果点后面不是数字，那么 "1." 本身就是一个浮点数
                    // （除非语言规范不允许，例如要求小数点后必须有数字）
                    // 对于大多数语言，"1." 是合法的浮点数
                }

            } else if (!isHex && (c_peek == 'f' || c_peek == 'F' || c_peek == 'd' || c_peek == 'D') && isFloat) {
                // 处理浮点数后缀 f/F/d/D
                sb.append((char)nextChar());
                break; // 后缀后通常数字结束
            } else if (!isHex && (c_peek == 'l' || c_peek == 'L') && !isFloat) {
                // 处理长整型后缀 L/l
                sb.append((char)nextChar());
                break; // 后缀后通常数字结束
            }
            else {
                // 检查是否是科学计数法 (e/E)
                if (!isHex && (c_peek == 'e' || c_peek == 'E')) {
                    // 可能是科学计数法的开始
                    StringBuilder tempSb = new StringBuilder(sb.toString()); // 保存当前数字部分
                    tempSb.append((char)nextChar()); // 消耗 e/E

                    Integer signOrDigit = peekChar();
                    if (signOrDigit != null && ( (char)(int)signOrDigit == '+' || (char)(int)signOrDigit == '-' ) ) {
                        tempSb.append((char)nextChar()); // 消耗符号
                    }

                    // 检查符号后是否有数字
                    Integer digitAfterSign = peekChar();
                    if (digitAfterSign != null && Character.isDigit((char)(int)digitAfterSign)) {
                        isFloat = true; // 科学计数法一定是浮点数
                        sb.setLength(0); // 清空sb，使用tempSb的内容
                        sb.append(tempSb);
                        // 继续读取指数部分的数字
                        while(true){
                            Integer expDigit = peekChar();
                            if(expDigit != null && Character.isDigit((char)(int)expDigit)){
                                sb.append((char)nextChar());
                            } else {
                                break;
                            }
                        }
                    } else {
                        // e/E 后面没有有效的指数，回溯
                        // 这种情况 'e'/'E' 将作为下一个Token（可能是标识符）被处理
                        // 我们需要将读取的 'e'/'E' 和可能的 '+/-' 退回
                        // 由于 `readNumber` 在 `nextTokenInternal` 中被调用，
                        // `nextTokenInternal` 的 `peekChar` 和 `nextChar` 机制
                        // 会处理紧跟在数字后的字符。
                        // 所以这里直接 break，让 `sb` 保持为 'e'/'E'之前的内容。
                        break;
                    }
                } else {
                    break; // 其他非数字、非十六进制、非小数点的字符，数字结束
                }
            }
        }

        String numStr = sb.toString();
        // 后续的错误检查，例如 "0x" 后面没有十六进制数字, "1.2.3"
        if (isHex && numStr.length() == 2 && numStr.toLowerCase().endsWith("x")) {
            System.err.printf("Lexical Error: Malformed hexadecimal number '%s' at %d:%d.%n", numStr, tokenLine, tokenCol);
            return new Token(TokenType.ERROR, numStr, tokenLine, tokenCol);
        }
        // 可以在这里添加更多数字格式的校验
        // 例如，如果 "1." 后跟一个字母，当前逻辑会正确地将 "1." 识别为 FLOAT，字母作为下一个 IDENTIFIER。
        // 如果要求小数点后必须有数字 (例如 "1.a" 中 "1." 不是合法浮点数)，则需要修改这里的逻辑。

        return new Token(isFloat ? TokenType.FLOAT : TokenType.INTEGER, numStr, tokenLine, tokenCol);
    }

    private boolean isHexDigit(char c) {
        return Character.isDigit(c)
                || (c >= 'a' && c <= 'f')
                || (c >= 'A' && c <= 'F');
    }

    private boolean isOperator(String s) {
        return Arrays.asList("+", "-", "*", "/", "%", "=", "==", "!=", "<", "<=", ">", ">=",
                "&&", "||", "!", "^", "&", "|", "<<", ">>", "<<<", "++", "--").contains(s);
    }

    private boolean isDelimiter(String s) {
        return Arrays.asList("(", ")", "{", "}", "[", "]", ";", ",", ".", ":").contains(s);
    }

    private int nextChar() throws IOException {
        int ch = reader.read();
        if (ch == '\n') {
            line++;
            col = 1;
        } else if (ch != -1) {
            col++;
        }
        return ch;
    }

    private Integer peekChar() throws IOException {
        reader.mark(1);
        int ch = reader.read();
        reader.reset();
        return ch == -1 ? null : ch;
    }

    private String peekCharAsString() throws IOException {
        Integer p = peekChar();
        return p == null ? null : String.valueOf((char)(int)p);
    }

    private String peek(int k) throws IOException {
        reader.mark(k);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < k; i++) {
            int ch = reader.read();
            if (ch == -1) break;
            sb.append((char) ch);
        }
        reader.reset();
        return sb.length() == k ? sb.toString() : null;
    }

    public static void main(String[] args) {
        String path = "src/compile/input/Test.txt"; // 固定路径
        List<Token> tokens = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> identifiers = new HashSet<>(); // 用于存储标识符

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            Lexer lexer = new Lexer(br);
            Token token;
            while ((token = lexer.nextToken()).type != TokenType.EOF) {
                if (token.type == TokenType.ERROR) {
                    errors.add(String.format("Lexical Error: '%s' at %d:%d", token.value, token.line, token.pos));
                } else {
                    tokens.add(token);
                    if (token.type == TokenType.IDENTIFIER) {
                        identifiers.add(token.value);
                    }
                }
                // 原始的打印方式，可以保留用于调试
                // System.out.println(token);
            }
            // 添加最后的EOF Token（如果需要）
            tokens.add(token);


        } catch (IOException e) {
            System.err.println("读取文件失败: " + e.getMessage());
            e.printStackTrace();
            return; // 发生IO错误则提前返回
        }

        // 打印所有识别的 Token
        System.out.println("Tokens:");
        for (Token t : tokens) {
            System.out.println(t);
        }
        System.out.println("--------------------");

        // 打印所有词法错误
        if (!errors.isEmpty()) {
            System.out.println("Lexical Errors Found:");
            for (String err : errors) {
                System.out.println(err);
            }
            System.out.println("--------------------");
        } else {
            System.out.println("No lexical errors found.");
            System.out.println("--------------------");
        }

        // 打印标识符表（初级符号表）
        System.out.println("Identifiers (Symbol Table - Lexer Level):");
        for (String id : identifiers) {
            System.out.println(id);
        }
        System.out.println("--------------------");

    }
}
