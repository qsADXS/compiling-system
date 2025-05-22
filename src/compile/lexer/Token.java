package src.compile.lexer;

import java.util.HashSet;
import java.util.Set;

/**
 * Token：词法单元
 */
public class Token {
    public TokenType type;                   // 词法单元的类型
    public TokenSpecificType specific;       // 词法单元的具体类型
    public String value;                     // 词法单元的值
    public int line, pos;                    // 行号和列号


    public Token(TokenType type, String value, int line, int pos) {
        this.type = type;
        this.value = value;
        this.line = line;
        this.pos = pos;
        this.specific = TokenSpecificType.UNKNOWN;
    }


    public void parseSpecific() {
        // 根据 type 和 value 设定 specific
        switch (type) {
            case TYPE:
                switch (value) {
                    case "int":
                        specific = TokenSpecificType.TYPE_INT;
                        break;
                    case "long":
                        specific = TokenSpecificType.TYPE_LONG;
                        break;
                    case "float":
                        specific = TokenSpecificType.TYPE_FLOAT;
                        break;
                    case "double":
                        specific = TokenSpecificType.TYPE_DOUBLE;
                        break;
                    case "boolean":
                        specific = TokenSpecificType.TYPE_BOOLEAN;
                        break;
                    case "char":
                        specific = TokenSpecificType.TYPE_CHAR;
                        break;
                    case "byte":
                        specific = TokenSpecificType.TYPE_BYTE;
                        break;
                    case "short":
                        specific = TokenSpecificType.TYPE_SHORT;
                        break;
                }
                break;
            case INTEGER:
                specific = TokenSpecificType.CONST_INT;
                break;
            case FLOAT:
                specific = TokenSpecificType.CONST_FLOAT;
                break;
            case CHAR:
                specific = TokenSpecificType.CONST_CHAR;
                break;
            case STRING:
                specific = TokenSpecificType.CONST_STRING;
                break;
            case IDENTIFIER:
                specific = TokenSpecificType.IDENTIFIER;
                break;
            case RESERVED:
                switch (value) {
                    case "if":
                    case "else":
                    case "for":
                    case "while":
                    case "do":
                    case "switch":
                    case "case":
                    case "default":
                    case "break":
                    case "continue":
                    case "return":
                        specific = TokenSpecificType.valueOf("RES_" + value.toUpperCase());
                        break;
                    case "class":
                    case "interface":
                    case "extends":
                    case "implements":
                        specific = TokenSpecificType.valueOf("RES_" + value.toUpperCase());
                        break;
                    case "import":
                        specific = TokenSpecificType.RES_IMPORT;
                        break;
                    case "package":
                        specific = TokenSpecificType.RES_PACKAGE;
                        break;
                    case "public":
                    case "private":
                    case "protected":
                    case "static":
                    case "final":
                    case "abstract":
                    case "synchronized":
                        specific = TokenSpecificType.valueOf("RES_" + value.toUpperCase());
                        break;
                }
                break;
            case OPERATOR:
                switch (value) {
                    case "+":
                        specific = TokenSpecificType.OP_PLUS;
                        break;
                    case "-":
                        specific = TokenSpecificType.OP_MINUS;
                        break;
                    case "*":
                        specific = TokenSpecificType.OP_MUL;
                        break;
                    case "/":
                        specific = TokenSpecificType.OP_DIV;
                        break;
                    case "%":
                        specific = TokenSpecificType.OP_MOD;
                        break;
                    case "=":
                        specific = TokenSpecificType.OP_ASSIGN;
                        break;
                    case "==":
                        specific = TokenSpecificType.OP_EQ;
                        break;
                    case "!=":
                        specific = TokenSpecificType.OP_NE;
                        break;
                    case "<":
                        specific = TokenSpecificType.OP_LT;
                        break;
                    case "<=":
                        specific = TokenSpecificType.OP_LE;
                        break;
                    case ">":
                        specific = TokenSpecificType.OP_GT;
                        break;
                    case ">=":
                        specific = TokenSpecificType.OP_GE;
                        break;
                    case "&&":
                        specific = TokenSpecificType.OP_AND_AND;
                        break;
                    case "||":
                        specific = TokenSpecificType.OP_OR_OR;
                        break;
                    case "!":
                        specific = TokenSpecificType.OP_NOT;
                        break;
                    case "^":
                        specific = TokenSpecificType.OP_XOR;
                        break;
                    case "&":
                        specific = TokenSpecificType.OP_AND;
                        break;
                    case "|":
                        specific = TokenSpecificType.OP_OR;
                        break;
                    case "<<":
                        specific = TokenSpecificType.OP_SHL;
                        break;
                    case ">>":
                        specific = TokenSpecificType.OP_SHR;
                        break;
                    case ">>>":
                        specific = TokenSpecificType.OP_USHR;
                        break;
                    case "++":
                        specific = TokenSpecificType.OP_INC;
                        break;
                    case "--":
                        specific = TokenSpecificType.OP_DEC;
                        break;
                }
                break;
            case DELIMITER:
                switch (value) {
                    case "(":
                        specific = TokenSpecificType.DEL_LPAREN;
                        break;
                    case ")":
                        specific = TokenSpecificType.DEL_RPAREN;
                        break;
                    case "{":
                        specific = TokenSpecificType.DEL_LBRACE;
                        break;
                    case "}":
                        specific = TokenSpecificType.DEL_RBRACE;
                        break;
                    case "[":
                        specific = TokenSpecificType.DEL_LBRACKET;
                        break;
                    case "]":
                        specific = TokenSpecificType.DEL_RBRACKET;
                        break;
                    case ";":
                        specific = TokenSpecificType.DEL_SEMICOLON;
                        break;
                    case ",":
                        specific = TokenSpecificType.DEL_COMMA;
                        break;
                    case ".":
                        specific = TokenSpecificType.DEL_DOT;
                        break;
                    case ":":
                        specific = TokenSpecificType.DEL_COLON;
                        break;
                }
                break;
            default:
                specific = TokenSpecificType.UNKNOWN;
        }
    }

    @Override
    public String toString() {
        return String.format("(%s, '%s', '%s', %d:%d)", type, value, specific, line, pos);
    }

    /**
     * 关键字集合
     */
    public static final Set<String> KEYWORDS = new HashSet<>();

    static {
        String[] keys = {
                "if", "else", "for", "while", "do", "switch", "case", "default", "break", "continue", "return",
                "class", "interface", "extends", "implements", "import", "package",
                "public", "protected", "private", "static", "final", "abstract", "synchronized",
                "boolean", "char", "byte", "short", "int", "long", "float", "double", "void", "true", "false", "null"
        };
        for (String k : keys) KEYWORDS.add(k);
    }
}
