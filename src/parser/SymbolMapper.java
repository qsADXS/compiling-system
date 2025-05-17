package src.parser;


import src.lexer.Token;
import src.lexer.TokenType;
import src.lexer.TokenSpecificType;
/**
 * @author zephyrsky
 * @FileName SymbolMapper
 * @Description 将词法分析器生成的 Token 映射到语法分析器使用的 SymbolType。
 * @create 2025-05-17 19:26
 */

public class SymbolMapper {

    /**
     * 将一个 Lexer Token 映射到对应的 Parser SymbolType。
     * @param token 词法单元
     * @return 对应的 SymbolType，如果 Token 无法映射到文法中的终端符号，则返回 T_ERROR。
     */
    public static SymbolType mapToken(Token token) {
        if (token == null) {
            return SymbolType.T_ERROR; // 或者抛出异常
        }

        // 根据 TokenType 和 TokenSpecificType/value 进行映射
        switch (token.type) {
            case EOF:
                return SymbolType.T_EOF;

            case ERROR:
                return SymbolType.T_ERROR;

            case IDENTIFIER: // id
                return SymbolType.T_ID;

            case INTEGER: // num
                return SymbolType.T_NUM;

            case FLOAT: // real
                return SymbolType.T_REAL;

            // 字符串和字符字面量在这个文法中不是终端符号，映射为错误
            case STRING:
            case CHAR:
                return SymbolType.T_ERROR;

            case TYPE: // basic 类型 (int, float, etc.)
                // 根据文法，所有基本类型都被抽象为 basic 终端符号
                switch (token.specific) {
                    case TYPE_INT:
                    case TYPE_LONG:
                    case TYPE_FLOAT:
                    case TYPE_DOUBLE:
                    case TYPE_BOOLEAN:
                    case TYPE_CHAR:
                    case TYPE_BYTE:
                    case TYPE_SHORT:
                        return SymbolType.T_BASIC;
                    default:
                        // 其他 TYPE 可能不是 basic (例如 void)，但在此文法中不作为终端符号
                        return SymbolType.T_ERROR;
                }


            case RESERVED: // 关键字
                switch (token.value) {
                    case "if": return SymbolType.T_IF;
                    case "else": return SymbolType.T_ELSE;
                    case "while": return SymbolType.T_WHILE;
                    case "do": return SymbolType.T_DO;
                    case "break": return SymbolType.T_BREAK;
                    case "true": return SymbolType.T_TRUE; // 布尔常量
                    case "false": return SymbolType.T_FALSE; // 布尔常量
                    // 其他关键字 (class, public, return etc.) 在这个文法中不是终端符号，映射为错误
                    default:
                        return SymbolType.T_ERROR;
                }

            case OPERATOR: // 运算符
                switch (token.value) {
                    case "=": return SymbolType.T_ASSIGN;
                    case "||": return SymbolType.T_OR_OR;
                    case "&&": return SymbolType.T_AND_AND;
                    case "==": return SymbolType.T_EQ_EQ;
                    case "!=": return SymbolType.T_NE;
                    case "<": return SymbolType.T_LT;
                    case "<=": return SymbolType.T_LE;
                    case ">": return SymbolType.T_GT;
                    case ">=": return SymbolType.T_GE;
                    case "+": return SymbolType.T_PLUS;
                    case "-": return SymbolType.T_MINUS;
                    case "*": return SymbolType.T_MUL;
                    case "/": return SymbolType.T_DIV;
                    case "!": return SymbolType.T_NOT;
                    // 其他运算符 (% ^ & | << >> >>> ++ --) 在这个文法中不是终端符号，映射为错误
                    default:
                        return SymbolType.T_ERROR;
                }

            case DELIMITER: // 分隔符
                switch (token.value) {
                    case "{": return SymbolType.T_LBRACE;
                    case "}": return SymbolType.T_RBRACE;
                    case ";": return SymbolType.T_SEMICOLON;
                    case "[": return SymbolType.T_LBRACKET;
                    case "]": return SymbolType.T_RBRACKET;
                    case "(": return SymbolType.T_LPAREN;
                    case ")": return SymbolType.T_RPAREN;
                    // 其他分隔符 (, . :) 在这个文法中不是终端符号，映射为错误
                    default:
                        return SymbolType.T_ERROR;
                }

            default:
                // 未知的 TokenType，映射为错误
                return SymbolType.T_ERROR;
        }
    }
}

