package src.compile.parser.algorithm;

import java.util.*;

/**
 * @author sixteacher
 * @version 1.0
 * @description Grammar
 * @date 2025/5/19
 */


/**
 * 定义了语言的文法规则，用于 LR 解析器。
 * 这个类包含了文法的起始符号、终结符集合、非终结符集合以及所有的产生式。
 * LR 解析器将使用这些信息来构建解析表并分析输入程序的语法结构。
 * 该文法特别处理了 "悬空else" (dangling else) 问题，通过引入 NT_MATCHED_STMT 和 NT_UNMATCHED_STMT。
 */
public class Grammar {
    /**
     * 文法的起始符号（经过增广后的起始符号）。
     * 对于LR解析，通常会增广文法，例如 S' -> S，其中 S 是原始的起始符号。
     */
    public final SymbolType startSymbol;

    /**
     * 终结符集合 (Terminals)。这些是词法单元，是文法中不能再分解的基本符号。
     */
    public final Set<SymbolType> terminals = new HashSet<>();

    /**
     * 非终结符集合 (Non-Terminals)。这些是可以被一组终结符或非终结符替换的语法变量。
     */
    public final Set<SymbolType> nonTerminals = new HashSet<>();

    /**
     * 文法产生式列表。每个产生式定义了一个非终结符如何展开。
     */
    public final List<Production> productions = new ArrayList<>();

    /**
     * 构造函数，初始化文法。
     * 这包括设置起始符号、定义所有终结符和非终结符，以及加载所有产生式规则。
     */
    public Grammar() {
        this.startSymbol = SymbolType.NT_AUGMENTED_START; // 通常LR分析会使用一个增广的起始符号

        initializeSymbols(); // 初始化终结符和非终结符
        initializeProductions(); // 初始化所有产生式
    }

    /**
     * 初始化文法中的终结符和非终结符集合。
     */
    private void initializeSymbols() {
        // 非终结符 (Non-Terminals)
        nonTerminals.add(SymbolType.NT_AUGMENTED_START); // 增广文法的起始符号 S'
        nonTerminals.add(SymbolType.NT_PROGRAM);         // 程序 Program
        nonTerminals.add(SymbolType.NT_BLOCK);           // 代码块 Block
        nonTerminals.add(SymbolType.NT_DECLS);           // 声明列表 Decls
        nonTerminals.add(SymbolType.NT_DECL);            // 单个声明 Decl
        nonTerminals.add(SymbolType.NT_TYPE);            // 类型 Type
        nonTerminals.add(SymbolType.NT_STMTS);           // 语句列表 Stmts
        nonTerminals.add(SymbolType.NT_STMT);            // 单个语句 Stmt (这是一个高层抽象，具体由 matched/unmatched 实现)
        nonTerminals.add(SymbolType.NT_LOC);             // 位置/左值 Loc (通常是变量或数组元素)
        nonTerminals.add(SymbolType.NT_BOOL);            // 布尔表达式 Bool (顶层，处理 ||)
        nonTerminals.add(SymbolType.NT_JOIN);            // 连接表达式 Join (处理 &&)
        nonTerminals.add(SymbolType.NT_EQUALITY);        // 相等性表达式 Equality (处理 ==, !=)
        nonTerminals.add(SymbolType.NT_REL);             // 关系表达式 Rel (处理 <, <=, >, >=)
        nonTerminals.add(SymbolType.NT_EXPR);            // 算术表达式 Expr (处理 +, -)
        nonTerminals.add(SymbolType.NT_TERM);            // 项 Term (处理 *, /)
        nonTerminals.add(SymbolType.NT_UNARY);           // 一元表达式 Unary (处理 !, 一元-)
        nonTerminals.add(SymbolType.NT_FACTOR);          // 因子 Factor (原子操作数，如括号表达式、标识符、字面量)
        nonTerminals.add(SymbolType.NT_BOOL_PRIME);      // bool' (用于处理左递归的辅助非终结符，对应 bool)
        nonTerminals.add(SymbolType.NT_MATCHED_STMT);    // 匹配语句 (用于解决悬空else问题，如 if-then-else)
        nonTerminals.add(SymbolType.NT_UNMATCHED_STMT);  // 非匹配语句 (用于解决悬空else问题，如 if-then)

        // 终结符 (Terminals) - 这些通常是词法分析器产生的 Token 类型
        terminals.add(SymbolType.T_LBRACE);      // {
        terminals.add(SymbolType.T_RBRACE);     // }
        terminals.add(SymbolType.T_LBRACKET);    // [
        terminals.add(SymbolType.T_RBRACKET);   // ]
        terminals.add(SymbolType.T_SEMICOLON);       // ;
        terminals.add(SymbolType.T_ASSIGN);          // =
        terminals.add(SymbolType.T_IF);              // if
        terminals.add(SymbolType.T_ELSE);            // else
        terminals.add(SymbolType.T_WHILE);           // while
        terminals.add(SymbolType.T_DO);              // do
        terminals.add(SymbolType.T_BREAK);           // break
        terminals.add(SymbolType.T_OR_OR);              // ||
        terminals.add(SymbolType.T_AND_AND);             // &&
        terminals.add(SymbolType.T_EQ_EQ);              // ==
        terminals.add(SymbolType.T_NE);             // !=
        terminals.add(SymbolType.T_LT);              // <
        terminals.add(SymbolType.T_LE);             // <=
        terminals.add(SymbolType.T_GE);             // >=
        terminals.add(SymbolType.T_GT);              // >
        terminals.add(SymbolType.T_PLUS);           // +
        terminals.add(SymbolType.T_MINUS);          // - (二元减或一元负)
        terminals.add(SymbolType.T_MUL);             // *
        terminals.add(SymbolType.T_DIV);             // /
        terminals.add(SymbolType.T_NOT);             // !
        terminals.add(SymbolType.T_LPAREN);         // (
        terminals.add(SymbolType.T_RPAREN);         // )
        terminals.add(SymbolType.T_ID);              // id (标识符)
        terminals.add(SymbolType.T_NUM);            // num (整数常量)
        terminals.add(SymbolType.T_REAL);           // real (浮点数常量)
        terminals.add(SymbolType.T_TRUE);           // true (布尔真)
        terminals.add(SymbolType.T_FALSE);           // false (布尔假)
        terminals.add(SymbolType.T_BASIC);           // basic (基本类型关键字，如 int, float)
        terminals.add(SymbolType.T_EPSILON);         // ε (空串，用于表示可选部分或空产生式)
    }

    /**
     * 初始化文法的所有产生式规则。
     * 产生式的顺序和形式对于LR解析表的构建至关重要。
     * 注意：许多产生式使用了左递归，这对于LL解析器是个问题，但LR解析器可以处理。
     * 为了解决 "悬空else" (dangling else) 的歧义，文法引入了 matched_stmt 和 unmatched_stmt。
     */
    private void initializeProductions() {
        // 0. S' → program (增广文法的起始产生式)
        addProduction(SymbolType.NT_AUGMENTED_START, Arrays.asList(SymbolType.NT_PROGRAM));

        // 1. program → block
        addProduction(SymbolType.NT_PROGRAM, Arrays.asList(SymbolType.NT_BLOCK));

        // 2. block → { decls stmts }
        addProduction(SymbolType.NT_BLOCK, Arrays.asList(
                SymbolType.T_LBRACE,
                SymbolType.NT_DECLS,
                SymbolType.NT_STMTS,
                SymbolType.T_RBRACE
        ));
        // 注意: 当前文法要求 block 中必须同时有 decls 和 stmts。
        // 如果需要支持空的 decls (即 block → { stmts }) 或空 stmts (block → { decls }) 或空 block (block → { })，
        // 需要添加相应的产生式，例如：
        // addProduction(SymbolType.NT_BLOCK, Arrays.asList(SymbolType.T_LBRACE, SymbolType.NT_STMTS, SymbolType.T_RBRACE)); // 允许 decls 为空
        // addProduction(SymbolType.NT_BLOCK, Arrays.asList(SymbolType.T_LBRACE, SymbolType.NT_DECLS, SymbolType.T_RBRACE)); // 允许 stmts 为空
        // addProduction(SymbolType.NT_BLOCK, Arrays.asList(SymbolType.T_LBRACE, SymbolType.T_RBRACE)); // 允许 block 为空

        // Decls: 声明列表
        // 3. decls → decls decl (左递归，允许多个声明)
        addProduction(SymbolType.NT_DECLS, Arrays.asList(SymbolType.NT_DECLS, SymbolType.NT_DECL));
        // 4. decls → ε (允许声明列表为空)
        addProduction(SymbolType.NT_DECLS, Arrays.asList(SymbolType.T_EPSILON));
        //  (原先可能存在的 decls → decl 规则被左递归版本和ε版本覆盖了)

        // Decl: 单个声明
        // 5. decl → type id;
        addProduction(SymbolType.NT_DECL, Arrays.asList(SymbolType.NT_TYPE, SymbolType.T_ID, SymbolType.T_SEMICOLON));

        // Type: 类型定义
        // 6. type → type [ num ] (左递归，用于数组类型，如 int[10][20])
        addProduction(SymbolType.NT_TYPE, Arrays.asList(SymbolType.NT_TYPE, SymbolType.T_LBRACKET, SymbolType.T_NUM, SymbolType.T_RBRACKET));
        // 7. type → basic (基本类型，如 int, float)
        addProduction(SymbolType.NT_TYPE, Arrays.asList(SymbolType.T_BASIC));

        // Stmts: 语句列表
        // 8. stmts → stmts stmt (左递归，允许多个语句)
        addProduction(SymbolType.NT_STMTS, Arrays.asList(SymbolType.NT_STMTS, SymbolType.NT_STMT));
        // 9. stmts → ε (允许语句列表为空)
        addProduction(SymbolType.NT_STMTS, Arrays.asList(SymbolType.T_EPSILON));

        // Stmt: 语句 (高层抽象，具体由 matched_stmt 和 unmatched_stmt 实现以解决悬空else)
        // 10. stmt → matched_stmt
        addProduction(SymbolType.NT_STMT, Arrays.asList(SymbolType.NT_MATCHED_STMT));
        // 11. stmt → unmatched_stmt
        addProduction(SymbolType.NT_STMT, Arrays.asList(SymbolType.NT_UNMATCHED_STMT));
        //  (注: stmt → decls 这类规则如果存在，可能会引入额外的移进/规约冲突，需谨慎处理)

        // Unmatched_Stmt: 非匹配语句 (主要用于处理 if-then 和 if-then-else 的悬空else情况)
        // 12. unmatched_stmt → if ( bool ) unmatched_stmt (if (...) if (...) stmt 结构)
        addProduction(SymbolType.NT_UNMATCHED_STMT, Arrays.asList(
                SymbolType.T_IF, SymbolType.T_LPAREN, SymbolType.NT_BOOL, SymbolType.T_RPAREN, SymbolType.NT_UNMATCHED_STMT
        ));
        // 13. unmatched_stmt → if ( bool ) matched_stmt (简单的 if-then 语句，then 部分是已匹配的)
        addProduction(SymbolType.NT_UNMATCHED_STMT, Arrays.asList(
                SymbolType.T_IF, SymbolType.T_LPAREN, SymbolType.NT_BOOL, SymbolType.T_RPAREN, SymbolType.NT_MATCHED_STMT
        ));
        // 14. unmatched_stmt → if ( bool ) matched_stmt else unmatched_stmt (if-then-else，但else部分可能悬空)
        addProduction(SymbolType.NT_UNMATCHED_STMT, Arrays.asList(
                SymbolType.T_IF, SymbolType.T_LPAREN, SymbolType.NT_BOOL, SymbolType.T_RPAREN, SymbolType.NT_MATCHED_STMT, SymbolType.T_ELSE, SymbolType.NT_UNMATCHED_STMT
        ));

        // Matched_Stmt: 匹配语句 (这些语句不会产生悬空else问题)
        // 15. matched_stmt → loc = bool ; (赋值语句)
        addProduction(SymbolType.NT_MATCHED_STMT, Arrays.asList(
                SymbolType.NT_LOC, SymbolType.T_ASSIGN, SymbolType.NT_BOOL, SymbolType.T_SEMICOLON
        ));
        // 16. matched_stmt → if ( bool ) matched_stmt else matched_stmt (完整的 if-then-else)
        addProduction(SymbolType.NT_MATCHED_STMT, Arrays.asList(
                SymbolType.T_IF, SymbolType.T_LPAREN, SymbolType.NT_BOOL, SymbolType.T_RPAREN, SymbolType.NT_MATCHED_STMT, SymbolType.T_ELSE, SymbolType.NT_MATCHED_STMT
        ));
        // 17. matched_stmt → while ( bool ) matched_stmt (while 循环，循环体是已匹配的语句)
        addProduction(SymbolType.NT_MATCHED_STMT, Arrays.asList(
                SymbolType.T_WHILE, SymbolType.T_LPAREN, SymbolType.NT_BOOL, SymbolType.T_RPAREN, SymbolType.NT_MATCHED_STMT
        ));
        // 18. matched_stmt → do matched_stmt while ( bool ) ; (do-while 循环，循环体是已匹配的语句)
        addProduction(SymbolType.NT_MATCHED_STMT, Arrays.asList(
                SymbolType.T_DO, SymbolType.NT_MATCHED_STMT, SymbolType.T_WHILE, SymbolType.T_LPAREN, SymbolType.NT_BOOL, SymbolType.T_RPAREN, SymbolType.T_SEMICOLON
        ));
        // 19. matched_stmt → break ;
        addProduction(SymbolType.NT_MATCHED_STMT, Arrays.asList(SymbolType.T_BREAK, SymbolType.T_SEMICOLON));
        // 20. matched_stmt → block (代码块本身也是一个匹配语句)
        addProduction(SymbolType.NT_MATCHED_STMT, Arrays.asList(SymbolType.NT_BLOCK));

        // Loc: 左值 (通常是变量或数组元素)
        // 21. loc → loc [ num ] (左递归，用于数组元素访问，如 id[expr][expr])
        addProduction(SymbolType.NT_LOC, Arrays.asList(SymbolType.NT_LOC, SymbolType.T_LBRACKET, SymbolType.T_NUM, SymbolType.T_RBRACKET)); // 注意：这里用T_NUM作为索引，实际编译器中通常是表达式NT_EXPR
        // 22. loc → id (简单变量)
        addProduction(SymbolType.NT_LOC, Arrays.asList(SymbolType.T_ID));

        // Bool: 布尔表达式 (处理 || 逻辑或)
        // 23. bool → bool' (使用 bool' 辅助非终结符来消除直接左递归，如果解析器生成工具不直接支持)
        addProduction(SymbolType.NT_BOOL, Arrays.asList(SymbolType.NT_BOOL_PRIME));
        // 24. bool' → bool' || join (左递归)
        addProduction(SymbolType.NT_BOOL_PRIME, Arrays.asList(SymbolType.NT_BOOL_PRIME, SymbolType.T_OR_OR, SymbolType.NT_JOIN));
        // 25. bool' → join
        addProduction(SymbolType.NT_BOOL_PRIME, Arrays.asList(SymbolType.NT_JOIN));

        // Join: 连接表达式 (处理 && 逻辑与)
        // 26. join → join && equality (左递归)
        addProduction(SymbolType.NT_JOIN, Arrays.asList(SymbolType.NT_JOIN, SymbolType.T_AND_AND, SymbolType.NT_EQUALITY));
        // 27. join → equality
        addProduction(SymbolType.NT_JOIN, Arrays.asList(SymbolType.NT_EQUALITY));

        // Equality: 相等性表达式 (处理 ==, !=)
        // 28. equality → equality == rel (左递归)
        addProduction(SymbolType.NT_EQUALITY, Arrays.asList(SymbolType.NT_EQUALITY, SymbolType.T_EQ_EQ, SymbolType.NT_REL));
        // 29. equality → equality != rel (左递归)
        addProduction(SymbolType.NT_EQUALITY, Arrays.asList(SymbolType.NT_EQUALITY, SymbolType.T_NE, SymbolType.NT_REL));
        // 30. equality → rel
        addProduction(SymbolType.NT_EQUALITY, Arrays.asList(SymbolType.NT_REL));

        // Rel: 关系表达式 (处理 <, <=, >, >=)
        // 31. rel → expr < expr
        addProduction(SymbolType.NT_REL, Arrays.asList(SymbolType.NT_EXPR, SymbolType.T_LT, SymbolType.NT_EXPR));
        // 32. rel → expr <= expr
        addProduction(SymbolType.NT_REL, Arrays.asList(SymbolType.NT_EXPR, SymbolType.T_LE, SymbolType.NT_EXPR));
        // 33. rel → expr >= expr
        addProduction(SymbolType.NT_REL, Arrays.asList(SymbolType.NT_EXPR, SymbolType.T_GE, SymbolType.NT_EXPR));
        // 34. rel → expr > expr
        addProduction(SymbolType.NT_REL, Arrays.asList(SymbolType.NT_EXPR, SymbolType.T_GT, SymbolType.NT_EXPR));
        // 35. rel → expr (一个算术表达式本身也可以是一个关系表达式的基础)
        addProduction(SymbolType.NT_REL, Arrays.asList(SymbolType.NT_EXPR));

        // Expr: 算术表达式 (处理 +, -)
        // 36. expr → expr + term (左递归)
        addProduction(SymbolType.NT_EXPR, Arrays.asList(SymbolType.NT_EXPR, SymbolType.T_PLUS, SymbolType.NT_TERM));
        // 37. expr → expr - term (左递归)
        addProduction(SymbolType.NT_EXPR, Arrays.asList(SymbolType.NT_EXPR, SymbolType.T_MINUS, SymbolType.NT_TERM));
        // 38. expr → term
        addProduction(SymbolType.NT_EXPR, Arrays.asList(SymbolType.NT_TERM));

        // Term: 项 (处理 *, /)
        // 39. term → term * unary (左递归)
        addProduction(SymbolType.NT_TERM, Arrays.asList(SymbolType.NT_TERM, SymbolType.T_MUL, SymbolType.NT_UNARY));
        // 40. term → term / unary (左递归)
        addProduction(SymbolType.NT_TERM, Arrays.asList(SymbolType.NT_TERM, SymbolType.T_DIV, SymbolType.NT_UNARY));
        // 41. term → unary
        addProduction(SymbolType.NT_TERM, Arrays.asList(SymbolType.NT_UNARY));

        // Unary: 一元表达式 (处理 !, 一元-)
        // 42. unary → ! unary (逻辑非)
        addProduction(SymbolType.NT_UNARY, Arrays.asList(SymbolType.T_NOT, SymbolType.NT_UNARY));
        // 43. unary → - unary (一元负号)
        addProduction(SymbolType.NT_UNARY, Arrays.asList(SymbolType.T_MINUS, SymbolType.NT_UNARY));
        // 44. unary → factor
        addProduction(SymbolType.NT_UNARY, Arrays.asList(SymbolType.NT_FACTOR));

        // Factor: 因子 (表达式中的原子成分)
        // 45. factor → ( bool ) (括号括起来的布尔表达式)
        addProduction(SymbolType.NT_FACTOR, Arrays.asList(SymbolType.T_LPAREN, SymbolType.NT_BOOL, SymbolType.T_RPAREN));
        // 46. factor → loc (变量或数组元素)
        addProduction(SymbolType.NT_FACTOR, Arrays.asList(SymbolType.NT_LOC));
        // 47. factor → num (整数常量)
        addProduction(SymbolType.NT_FACTOR, Arrays.asList(SymbolType.T_NUM));
        // 48. factor → real (浮点数常量)
        addProduction(SymbolType.NT_FACTOR, Arrays.asList(SymbolType.T_REAL));
        // 49. factor → true (布尔真常量)
        addProduction(SymbolType.NT_FACTOR, Arrays.asList(SymbolType.T_TRUE));
        // 50. factor → false (布尔假常量)
        addProduction(SymbolType.NT_FACTOR, Arrays.asList(SymbolType.T_FALSE));
    }

    /**
     * 向文法中添加一条产生式。
     * @param left 产生式的左部 (一个非终结符)
     * @param right 产生式的右部 (一个符号列表，可以是终结符、非终结符或ε)
     */
    public void addProduction(SymbolType left, List<SymbolType> right) {
        productions.add(new Production(left, right, productions.size())); // productions.size() 作为该产生式的唯一ID
    }

    /**
     * 根据ID获取产生式。
     * @param id 产生式的唯一ID。
     * @return 对应的产生式对象。
     */
    public Production getProduction(int id) {
        return productions.get(id);
    }

    /**
     * 获取所有左部为指定非终结符的产生式。
     * @param leftSymbol 要查找的左部非终结符。
     * @return 一个包含所有匹配产生式的列表。
     */
    public List<Production> getProductionsByLeft(SymbolType leftSymbol) {
        List<Production> result = new ArrayList<>();
        for (Production p : productions) {
            if (p.left.equals(leftSymbol)) {
                result.add(p);
            }
        }
        return result;
    }
}
/*
文法
program  → block
block    → { decls stmts}
decls    → decls decl | ε
decl     → type id;
type     → type[num] | basic
stmts    → stmts stmt | ε
stmt     → loc = bool;
          | if ( bool ) stmt
          | if ( bool ) stmt else stmt
          | while ( bool ) stmt
          | do stmt while ( bool );
          | break;
          | block
loc      → loc[num] | id
bool     → bool || join | join
join     → join && equality | equality
equality → equality == rel | equality != rel | rel
rel      → expr < expr | expr <= expr | expr >= expr | expr > expr | expr
expr     → expr + term | expr - term | term
term     → term * unary | term / unary | unary
unary    → ! unary | - unary | factor
factor   → ( bool ) | loc | num | real | true | false
 */


/*
文法概览 (用于快速参考，具体解析时以代码中 matched/unmatched_stmt 为准来处理悬空else):
S'       → program                  // 增广文法起始
program  → block
block    → { decls stmts }
decls    → decls decl | ε
decl     → type id;
type     → type [ num ] | basic     // 数组索引 num 实际应为 expr
stmts    → stmts stmt | ε
stmt     → matched_stmt | unmatched_stmt // 高层抽象

matched_stmt → loc = bool ;
             | if ( bool ) matched_stmt else matched_stmt
             | while ( bool ) matched_stmt
             | do matched_stmt while ( bool ) ;
             | break ;
             | block

unmatched_stmt → if ( bool ) matched_stmt
               | if ( bool ) unmatched_stmt
               | if ( bool ) matched_stmt else unmatched_stmt

loc      → loc [ num ] | id        // 数组索引 num 实际应为 expr
bool     → bool'
bool'    → bool' || join | join
join     → join && equality | equality
equality → equality == rel | equality != rel | rel
rel      → expr < expr | expr <= expr | expr >= expr | expr > expr | expr
expr     → expr + term | expr - term | term
term     → term * unary | term / unary | unary
unary    → ! unary | - unary | factor
factor   → ( bool ) | loc | num | real | true | false

*/
