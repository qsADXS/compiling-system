// TokenType.java
package lexer;

/**
 * TokenType 表示词法单元的大类
 */
public enum TokenType {
    EOF,
    TYPE,
    INTEGER,
    FLOAT,
    STRING,
    CHAR,
    OPERATOR,
    DELIMITER,
    RESERVED,
    IDENTIFIER,
    ERROR,
    DEL_LBRACE,
    DEL_RBRACE,
    DEL_SEMICOLON,
    DEL_LBRACKET,
    DEL_RBRACKET,
    OP_ASSIGN,
    DEL_LPAREN,
    DEL_RPAREN,
    ELSE,
    WHILE,
    DO,
    BREAK,
    OP_OR_OR,
    OP_AND_AND,
    OP_EQ,
    OP_NE,
    OP_LT,
    OP_LE,
    OP_GE,
    OP_GT,
    OP_PLUS,
    OP_MINUS,
    OP_MUL,
    OP_DIV,
    OP_NOT,
}

