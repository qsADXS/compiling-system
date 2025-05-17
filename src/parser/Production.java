package src.parser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import static src.parser.SymbolType.*; // 导入 SymbolType 的所有枚举值，方便使用
/**
 * @author zephyrsky
 * @FileName Production
 * @Description
 * @create 2025-05-17 19:29
 */


/**
 * Production: 表示文法中的一条产生式 (A -> B C D)。
 * 也静态存储所有文法产生式，并按 LR 分析所需的顺序编号。
 * 文法如下：
 * 0: _AUGMENTED_START -> program  (增强文法起始规则)
 * 1: program -> block
 * 2: block -> { decls stmts }
 * 3: decls -> decls decl
 * 4: decls -> ε
 * 5: decl -> type id ;
 * 6: type -> type [ num ]
 * 7: type -> basic
 * 8: stmts -> stmts stmt
 * 9: stmts -> ε
 * 10: stmt -> loc = bool ;
 * 11: stmt -> if ( bool ) stmt
 * 12: stmt -> if ( bool ) stmt else stmt
 * 13: stmt -> while ( bool ) stmt
 * 14: stmt -> do stmt while ( bool ) ;
 * 15: stmt -> break ;
 * 16: stmt -> block
 * 17: Loc -> loc [ num ]
 * 18: Loc -> id
 * 19: bool -> bool || join
 * 20: bool -> join
 * 21: join -> join && equality
 * 22: join -> equality
 * 23: equality -> equality == rel
 * 24: equality -> equality != rel
 * 25: equality -> rel
 * 26: rel -> expr < expr
 * 27: rel -> expr <= expr
 * 28: rel -> expr >= expr
 * 29: rel -> expr > expr
 * 30: rel -> expr
 * 31: expr -> expr + term
 * 32: expr -> expr - term
 * 33: expr -> term
 * 34: term -> term * unary
 * 35: term -> term / unary
 * 36: term -> unary
 * 37: unary -> ! unary
 * 38: unary -> - unary
 * 39: unary -> factor
 * 40: factor -> ( bool )
 * 41: factor -> loc
 * 42: factor -> num
 * 43: factor -> real
 * 44: factor -> true
 * 45: factor -> false
 */

public class Production {
    private final SymbolType lhs;       // 产生式左部 (Left-Hand Side)
    private final List<SymbolType> rhs; // 产生式右部 (Right-Hand Side)

    // 存储所有带编号的文法产生式
    private static final List<Production> ALL_PRODUCTIONS = new ArrayList<>();

    static {
        // 增强文法起始规则 (通常为规则 0)
        ALL_PRODUCTIONS.add(new Production(NT_AUGMENTED_START, Arrays.asList(NT_PROGRAM)));

        // 文法产生式 (按照上面注释中的顺序添加)
        ALL_PRODUCTIONS.add(new Production(NT_PROGRAM, Arrays.asList(NT_BLOCK)));
        ALL_PRODUCTIONS.add(new Production(NT_BLOCK, Arrays.asList(T_LBRACE, NT_DECLS, NT_STMTS, T_RBRACE)));
        ALL_PRODUCTIONS.add(new Production(NT_DECLS, Arrays.asList(NT_DECLS, NT_DECL)));
        ALL_PRODUCTIONS.add(new Production(NT_DECLS, Collections.emptyList())); // ε 产生式
        ALL_PRODUCTIONS.add(new Production(NT_DECL, Arrays.asList(NT_TYPE, T_ID, T_SEMICOLON)));
        ALL_PRODUCTIONS.add(new Production(NT_TYPE, Arrays.asList(NT_TYPE, T_LBRACKET, T_NUM, T_RBRACKET)));
        ALL_PRODUCTIONS.add(new Production(NT_TYPE, Arrays.asList(T_BASIC)));
        ALL_PRODUCTIONS.add(new Production(NT_STMTS, Arrays.asList(NT_STMTS, NT_STMT)));
        ALL_PRODUCTIONS.add(new Production(NT_STMTS, Collections.emptyList())); // ε 产生式
        ALL_PRODUCTIONS.add(new Production(NT_STMT, Arrays.asList(NT_LOC, T_ASSIGN, NT_BOOL, T_SEMICOLON)));
        ALL_PRODUCTIONS.add(new Production(NT_STMT, Arrays.asList(T_IF, T_LPAREN, NT_BOOL, T_RPAREN, NT_STMT)));
        ALL_PRODUCTIONS.add(new Production(NT_STMT, Arrays.asList(T_IF, T_LPAREN, NT_BOOL, T_RPAREN, NT_STMT, T_ELSE, NT_STMT)));
        ALL_PRODUCTIONS.add(new Production(NT_STMT, Arrays.asList(T_WHILE, T_LPAREN, NT_BOOL, T_RPAREN, NT_STMT)));
        ALL_PRODUCTIONS.add(new Production(NT_STMT, Arrays.asList(T_DO, NT_STMT, T_WHILE, T_LPAREN, NT_BOOL, T_RPAREN, T_SEMICOLON)));
        ALL_PRODUCTIONS.add(new Production(NT_STMT, Arrays.asList(T_BREAK, T_SEMICOLON)));
        ALL_PRODUCTIONS.add(new Production(NT_STMT, Arrays.asList(NT_BLOCK)));
        ALL_PRODUCTIONS.add(new Production(NT_LOC, Arrays.asList(NT_LOC, T_LBRACKET, T_NUM, T_RBRACKET)));
        ALL_PRODUCTIONS.add(new Production(NT_LOC, Arrays.asList(T_ID)));
        ALL_PRODUCTIONS.add(new Production(NT_BOOL, Arrays.asList(NT_BOOL, T_OR_OR, NT_JOIN)));
        ALL_PRODUCTIONS.add(new Production(NT_BOOL, Arrays.asList(NT_JOIN)));
        ALL_PRODUCTIONS.add(new Production(NT_JOIN, Arrays.asList(NT_JOIN, T_AND_AND, NT_EQUALITY)));
        ALL_PRODUCTIONS.add(new Production(NT_JOIN, Arrays.asList(NT_EQUALITY)));
        ALL_PRODUCTIONS.add(new Production(NT_EQUALITY, Arrays.asList(NT_EQUALITY, T_EQ_EQ, NT_REL)));
        ALL_PRODUCTIONS.add(new Production(NT_EQUALITY, Arrays.asList(NT_EQUALITY, T_NE, NT_REL)));
        ALL_PRODUCTIONS.add(new Production(NT_EQUALITY, Arrays.asList(NT_REL)));
        ALL_PRODUCTIONS.add(new Production(NT_REL, Arrays.asList(NT_EXPR, T_LT, NT_EXPR)));
        ALL_PRODUCTIONS.add(new Production(NT_REL, Arrays.asList(NT_EXPR, T_LE, NT_EXPR)));
        ALL_PRODUCTIONS.add(new Production(NT_REL, Arrays.asList(NT_EXPR, T_GE, NT_EXPR)));
        ALL_PRODUCTIONS.add(new Production(NT_REL, Arrays.asList(NT_EXPR, T_GT, NT_EXPR)));
        ALL_PRODUCTIONS.add(new Production(NT_REL, Arrays.asList(NT_EXPR)));
        ALL_PRODUCTIONS.add(new Production(NT_EXPR, Arrays.asList(NT_EXPR, T_PLUS, NT_TERM)));
        ALL_PRODUCTIONS.add(new Production(NT_EXPR, Arrays.asList(NT_EXPR, T_MINUS, NT_TERM)));
        ALL_PRODUCTIONS.add(new Production(NT_EXPR, Arrays.asList(NT_TERM)));
        ALL_PRODUCTIONS.add(new Production(NT_TERM, Arrays.asList(NT_TERM, T_MUL, NT_UNARY)));
        ALL_PRODUCTIONS.add(new Production(NT_TERM, Arrays.asList(NT_TERM, T_DIV, NT_UNARY)));
        ALL_PRODUCTIONS.add(new Production(NT_TERM, Arrays.asList(NT_UNARY)));
        ALL_PRODUCTIONS.add(new Production(NT_UNARY, Arrays.asList(T_NOT, NT_UNARY)));
        ALL_PRODUCTIONS.add(new Production(NT_UNARY, Arrays.asList(T_MINUS, NT_UNARY)));
        ALL_PRODUCTIONS.add(new Production(NT_UNARY, Arrays.asList(NT_FACTOR)));
        ALL_PRODUCTIONS.add(new Production(NT_FACTOR, Arrays.asList(T_LPAREN, NT_BOOL, T_RPAREN)));
        ALL_PRODUCTIONS.add(new Production(NT_FACTOR, Arrays.asList(NT_LOC)));
        ALL_PRODUCTIONS.add(new Production(NT_FACTOR, Arrays.asList(T_NUM)));
        ALL_PRODUCTIONS.add(new Production(NT_FACTOR, Arrays.asList(T_REAL)));
        ALL_PRODUCTIONS.add(new Production(NT_FACTOR, Arrays.asList(T_TRUE)));
        ALL_PRODUCTIONS.add(new Production(NT_FACTOR, Arrays.asList(T_FALSE)));

        // 可以在这里添加文法中可能缺少的 NT_AUGMENTED_START 符号，为了保持与上面编号一致，我们在最前面手动添加了
        // 确保 SymbolType 中有 NT_AUGMENTED_START

    }

    public Production(SymbolType lhs, List<SymbolType> rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public SymbolType getLhs() {
        return lhs;
    }

    public List<SymbolType> getRhs() {
        return rhs;
    }

    /**
     * 获取产生式右部的长度。用于归约时弹出栈的次数。
     * 对于 ε 产生式，长度为 0。
     */
    public int getRhsLength() {
        return rhs.size();
    }

    /**
     * 根据产生式编号获取产生式对象。
     */
    public static Production getProduction(int index) {
        if (index < 0 || index >= ALL_PRODUCTIONS.size()) {
            throw new IndexOutOfBoundsException("Invalid production index: " + index);
        }
        return ALL_PRODUCTIONS.get(index);
    }

    /**
     * 获取所有产生式的列表。
     */
    public static List<Production> getAllProductions() {
        return Collections.unmodifiableList(ALL_PRODUCTIONS);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(lhs);
        sb.append(" -> ");
        if (rhs.isEmpty()) {
            sb.append("ε"); //  epsilon
        } else {
            for (int i = 0; i < rhs.size(); i++) {
                sb.append(rhs.get(i));
                if (i < rhs.size() - 1) {
                    sb.append(" ");
                }
            }
        }
        return sb.toString();
    }

    // 用于在 ParsingTable 中打印时获取某个符号的字符串表示，去除 SymbolType. 的前缀
    public static String getSymbolName(SymbolType symbol) {
        return symbol.name().replace("T_", "").replace("NT_", "");
    }
}

