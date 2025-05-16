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
    ERROR
}

