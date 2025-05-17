package src.parser;

/**
 * @author zephyrsky
 * @FileName SymbolType
 * @Description 表示文法中的终端符号和非终端符号。
 * @create 2025-05-16 23:31
 */
public enum SymbolType {
    // 非终端符号 (Non-terminals) - 通常以 NT_ 开头
    NT_PROGRAM,
    NT_BLOCK,
    NT_DECLS,
    NT_DECL,
    NT_TYPE,
    NT_STMTS,
    NT_STMT,
    NT_LOC,
    NT_BOOL,
    NT_JOIN,
    NT_EQUALITY,
    NT_REL,
    NT_EXPR,
    NT_TERM,
    NT_UNARY,
    NT_FACTOR,
    NT_AUGMENTED_START,
    // 终端符号 (Terminals) - 通常以 T_ 开头
    // 从文法规则右侧提取的终端符号：
    T_LBRACE,       // {
    T_RBRACE,       // }
    T_SEMICOLON,    // ;
    T_LBRACKET,     // [
    T_RBRACKET,     // ]
    T_ASSIGN,       // =
    T_IF,           // if
    T_LPAREN,       // (
    T_RPAREN,       // )
    T_ELSE,         // else
    T_WHILE,        // while
    T_DO,           // do
    T_BREAK,        // break
    T_OR_OR,        // ||
    T_AND_AND,      // &&
    T_EQ_EQ,        // ==
    T_NE,           // !=
    T_LT,           // <
    T_LE,           // <=
    T_GE,           // >=
    T_GT,           // >
    T_PLUS,         // +
    T_MINUS,        // -
    T_MUL,          // *
    T_DIV,          // /
    T_NOT,          // !
    T_ID,           // id (标识符)
    T_NUM,          // num (整数常量，从 Lexer 的 INTEGER 映射)
    T_REAL,         // real (浮点数常量，从 Lexer 的 FLOAT 映射)
    T_TRUE,         // true (布尔常量，从 Lexer 的 RESERVED 映射)
    T_FALSE,        // false (布尔常量，从 Lexer 的 RESERVED 映射)
    T_BASIC,        // basic (基本类型关键字，从 Lexer 的 TYPE 映射)
    // 特殊终端符：
    T_EOF,          // 文件结束 (End of File)
    T_ERROR;        // 词法错误或无法映射的 Token
    /**
     * 判断是否为非终端符号
     */
    public boolean isNonTerminal() {
        // 简单判断：以 NT_ 开头的为非终端符号
        return this.name().startsWith("NT_");
    }
    /**
     * 判断是否为终端符号
     */
    public boolean isTerminal() {
        return !isNonTerminal();
    }
}


