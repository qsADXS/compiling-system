package src.parser;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections; // For unmodifiable lists/maps

import static src.parser.SymbolType.*; // 导入 SymbolType 的所有枚举值
/**
 * @author zephyrsky
 * @FileName ParsingTable
 * @Description
 *  * 存储 LR(1) 分析表的 ACTION 和 GOTO 部分，并提供查询和打印功能。
 * @create 2025-05-17 19:32
 */

//todo
// actionTable 和 gotoTable 的具体数据需要根据的文法
// 通过 LR(1) 构造算法计算得出或使用工具生成后，填充到代码中。
public class ParsingTable {

    // 定义可能的分析动作
    public enum ActionType {
        SHIFT,      // 移进 (Shift)
        REDUCE,     // 归约 (Reduce)
        ACCEPT,     // 接受 (Accept)
        ERROR       // 错误 (Error)
    }

    // 表示一个分析动作，包含类型和可能的参数（移进到的状态或归约使用的产生式编号）
    public static class Action {
        public final ActionType type;
        public final int value; // 对于 SHIFT 是状态编号，对于 REDUCE 是产生式编号

        public Action(ActionType type, int value) {
            this.type = type;
            this.value = value;
        }

        public Action(ActionType type) {
            this(type, -1); // ACCEPT 和 ERROR 通常不需要 value
        }

        @Override
        public String toString() {
            switch (type) {
                case SHIFT: return "S" + value;
                case REDUCE: return "R" + value;
                case ACCEPT: return "acc";
                case ERROR: return "error";
                default: return "unknown";
            }
        }
    }

    // === 分析表数据结构 ===
    private final int numStates;
    private final Map<SymbolType, Integer> terminalIndexMap;
    private final Map<SymbolType, Integer> nonTerminalIndexMap;
    // ACTION 表: [状态][终端符号索引] -> 动作编码 (正数 S#, 负数 R#, -1 acc, 0 error)
    private final int[][] actionTable;
    // GOTO 表: [状态][非终端符号索引] -> 目标状态编号
    private final int[][] gotoTable;

    // === 用于构建索引映射的符号列表 ===
    // 终端符号列表 (需要按照分析表生成工具或手动计算时使用的顺序排列)
    private static final List<SymbolType> TERMINALS = Collections.unmodifiableList(Arrays.asList(
            T_LBRACE, T_RBRACE, T_SEMICOLON, T_LBRACKET, T_RBRACKET,
            T_ASSIGN, T_IF, T_LPAREN, T_RPAREN, T_ELSE,
            T_WHILE, T_DO, T_BREAK, T_OR_OR, T_AND_AND,
            T_EQ_EQ, T_NE, T_LT, T_LE, T_GE, T_GT,
            T_PLUS, T_MINUS, T_MUL, T_DIV, T_NOT,
            T_ID, T_NUM, T_REAL, T_TRUE, T_FALSE, T_BASIC,
            T_EOF // EOF 必须包含在终端符号列表中
    ));

    // 非终端符号列表 (需要按照分析表生成工具或手动计算时使用的顺序排列)
    private static final List<SymbolType> NON_TERMINALS = Collections.unmodifiableList(Arrays.asList(
            NT_AUGMENTED_START, // 增强文法起始符号
            NT_PROGRAM, NT_BLOCK, NT_DECLS, NT_DECL, NT_TYPE,
            NT_STMTS, NT_STMT, NT_LOC, NT_BOOL, NT_JOIN,
            NT_EQUALITY, NT_REL, NT_EXPR, NT_TERM, NT_UNARY, NT_FACTOR
    ));

    // === 构造函数 ===
    public ParsingTable() {
        // 初始化符号到索引的映射
        terminalIndexMap = new HashMap<>();
        for (int i = 0; i < TERMINALS.size(); i++) {
            terminalIndexMap.put(TERMINALS.get(i), i);
        }

        nonTerminalIndexMap = new HashMap<>();
        for (int i = 0; i < NON_TERMINALS.size(); i++) {
            nonTerminalIndexMap.put(NON_TERMINALS.get(i), i);
        }

        // === ！！！ 这里需要根据您的文法填入实际的分析表大小和数据 ！！！ ===
        // 假设分析表有 N 个状态
        this.numStates = 100; // <-- 更改为您的实际状态数量！这个文法可能需要很多状态

        actionTable = new int[numStates][TERMINALS.size()];
        gotoTable = new int[numStates][NON_TERMINALS.size()];

        // 使用占位符（例如 0 表示错误，-1 表示未初始化或接受）填充表格
        for (int i = 0; i < numStates; i++) {
            Arrays.fill(actionTable[i], 0); // 默认错误
            Arrays.fill(gotoTable[i], 0);   // 默认错误
        }

        // === ！！！ 将您计算或生成的 LR(1) 分析表数据填充到 actionTable 和 gotoTable 中 ！！！ ===
        // 示例：假设状态 0 遇到 { 移进到状态 2
        // int state0_lbrace_idx = terminalIndexMap.get(T_LBRACE);
        // if (state0_lbrace_idx != null) {
        //     actionTable[0][state0_lbrace_idx] = 2; // S2
        // }

        // 示例：假设状态 5 遇到 EOF 归约产生式 0 (_AUGMENTED_START -> program)
        // int state5_eof_idx = terminalIndexMap.get(T_EOF);
        // if (state5_eof_idx != null) {
        //    actionTable[5][state5_eof_idx] = -Production.getProduction(0).getRhsLength(); // 或者直接 encode 为 -1 表示 Accept
        // }

        // 示例：假设状态 10 归约产生式 4 (decls -> ε)，对应编号 4
        // actionTable[10][...] = -4; // R4 (注意：ε 产生式归约可能会发生在多种 lookahead 符号下)

        // 示例：假设状态 1 遇到非终端符号 NT_PROGRAM 后 GOTO 到状态 5
        // int state1_program_idx = nonTerminalIndexMap.get(NT_PROGRAM);
        // if (state1_program_idx != null) {
        //    gotoTable[1][state1_program_idx] = 5; // GOTO 5
        // }

        // === 实际数据填充示例 (极简化，仅供格式参考，不代表实际文法分析表) ===
        // 假设有 3 个状态，和极简文法 S -> a S b | ε
        // S -> a . S b, {$}
        // S -> . a S b, {$}
        // S -> . , {$}
        // S -> a S . b, {$}
        // S -> a S b ., {$}
        /*
        this.numStates = 4; // 假设状态数为 4
        actionTable = new int[numStates][TERMINALS.size()];
        gotoTable = new int[numStates][NON_TERMINALS.size()];
         for (int i = 0; i < numStates; i++) {
            Arrays.fill(actionTable[i], 0); // Default to Error
            Arrays.fill(gotoTable[i], 0);   // Default to Error
        }

        // Define indices for a, b, $, S
        SymbolType T_A = SymbolType.T_ID; // Example mapping 'a' to T_ID
        SymbolType T_B = SymbolType.T_SEMICOLON; // Example mapping 'b' to T_SEMICOLON
        SymbolType T_DOLLAR = SymbolType.T_EOF;
        SymbolType NT_S = SymbolType.NT_PROGRAM; // Example mapping S to NT_PROGRAM
        SymbolType NT_S_PRIME = SymbolType.NT_AUGMENTED_START; // S'

        Map<SymbolType, Integer> tempTermMap = new HashMap<>();
        tempTermMap.put(T_A, 0); tempTermMap.put(T_B, 1); tempTermMap.put(T_DOLLAR, 2);
        Map<SymbolType, Integer> tempNonTermMap = new HashMap<>();
        tempNonTermMap.put(NT_S, 0); tempNonTermMap.put(NT_S_PRIME, 1);

        // === Fill with example data for S -> a S b | ε ===
        // State 0: . S' -> . S $, {$}
        //          . S -> . a S b, {$, b}
        //          . S -> ., {$, b}
        actionTable[0][tempTermMap.get(T_A)] = 3; // S3 (Shift to state 3)
        actionTable[0][tempTermMap.get(T_B)] = -1; // R2 (Reduce S -> ε, prod 2) - Assuming ε is prod 2
        actionTable[0][tempTermMap.get(T_DOLLAR)] = -1; // R2

        gotoTable[0][tempNonTermMap.get(NT_S)] = 1; // GOTO 1
        gotoTable[0][tempNonTermMap.get(NT_S_PRIME)] = 2; // GOTO 2 (Accept state)

        // State 1: S' -> S ., {$} (Accept)
        //          S -> a S ., {b}
        actionTable[1][tempTermMap.get(T_B)] = 0; // S4 (Example shift for S -> a S . b - if state 4 exists)
        actionTable[1][tempTermMap.get(T_DOLLAR)] = - Production.getProduction(0).getRhsLength(); // ACC (Reduce S' -> S) - Let's use a specific code for ACC
        actionTable[1][tempTermMap.get(T_DOLLAR)] = -1; // Use -1 for ACC

        // State 2: S' -> S . $, {$} (Final Accept State)
        actionTable[2][tempTermMap.get(T_DOLLAR)] = -1; // ACC

        // State 3: S -> a . S b, {$, b}
        //          S -> . a S b, {$, b}
        //          S -> ., {$, b}
        actionTable[3][tempTermMap.get(T_A)] = 3; // S3
        actionTable[3][tempTermMap.get(T_B)] = -1; // R2
        actionTable[3][tempTermMap.get(T_DOLLAR)] = -1; // R2
        gotoTable[3][tempNonTermMap.get(NT_S)] = 1; // GOTO 1

        // State 4: S -> a S b ., {$, b}
        actionTable[4][tempTermMap.get(T_B)] = -3; // R1 (Reduce S -> a S b, prod 1)
        actionTable[4][tempTermMap.get(T_DOLLAR)] = -3; // R1
        */
        // === 结束示例数据 ===

        // === 确保至少有一个接受状态的例子 ===
        // 通常增强文法 S' -> program 的归约（产生式 0）在遇到 EOF 时表示接受。
        // 找到接收状态（例如状态 numStates-1）
        // actionTable[numStates - 1][terminalIndexMap.get(T_EOF)] = -1; // ACC
        // gotoTable[0][nonTerminalIndexMap.get(NT_AUGMENTED_START)] = numStates - 1; // GOTO to accept state
        // 请根据您的实际分析表结构设置接收状态和对应的 ACCEPT 动作。
    }

    /**
     * 将表中的整数编码解码为 Action 对象。
     * > 0 : Shift k (value k)
     * -1 : Accept
     * < -1 : Reduce k (value -k)
     * = 0 : Error
     */
    private Action decodeAction(int encodedAction) {
        if (encodedAction > 0) {
            return new Action(ActionType.SHIFT, encodedAction);
        } else if (encodedAction == -1) {
            return new Action(ActionType.ACCEPT);
        } else if (encodedAction < -1) {
            return new Action(ActionType.REDUCE, -encodedAction); // Reduce production |-k|
        } else { // encodedAction == 0
            return new Action(ActionType.ERROR);
        }
    }

    /**
     * 查询 ACTION 表。
     * @param state 当前状态。
     * @param terminal 当前输入终端符号。
     * @return 对应的分析动作 (Shift, Reduce, Accept, Error)。
     */
    public Action getAction(int state, SymbolType terminal) {
        Integer terminalIndex = terminalIndexMap.get(terminal);
        if (terminalIndex == null) {
            // 输入了未知的终端符号，这通常是词法分析器的错误，或文法/映射不匹配
            System.err.println("Error: Query for unknown terminal symbol in ACTION table: " + terminal);
            return new Action(ActionType.ERROR);
        }

        if (state < 0 || state >= numStates) {
            System.err.println("Error: Query for invalid state in ACTION table: " + state);
            return new Action(ActionType.ERROR);
        }

        int encodedAction = actionTable[state][terminalIndex];
        return decodeAction(encodedAction);
    }

    /**
     * 查询 GOTO 表。
     * @param state 当前状态。
     * @param nonTerminal 非终端符号。
     * @return GOTO 到的新状态编号，如果 GOTO 表中该项为错误，则返回 0 或其他约定值。
     */
    public int getGoto(int state, SymbolType nonTerminal) {
        Integer nonTerminalIndex = nonTerminalIndexMap.get(nonTerminal);
        if (nonTerminalIndex == null) {
            // 输入了未知的非终端符号，这不应该发生（除非是内部错误）
            System.err.println("Error: Query for unknown non-terminal symbol in GOTO table: " + nonTerminal);
            return 0; // 返回一个错误状态或约定值
        }

        if (state < 0 || state >= numStates) {
            System.err.println("Error: Query for invalid state in GOTO table: " + state);
            return 0; // 返回一个错误状态或约定值
        }

        return gotoTable[state][nonTerminalIndex];
    }

    /**
     * 打印 LR(1) 分析表的结构。
     * （请注意，如果表很大，打印可能非常耗时和占用空间）
     */
    public void printTable() {
        System.out.println("===== LR(1) Parsing Table =====");

        // 打印表头
        System.out.print(String.format("%-5s | ", "State")); // 状态列
        // 打印终端符号列
        System.out.print("ACTION");
        for (int i = 0; i < TERMINALS.size(); i++) {
            System.out.print(String.format(" %-6s", Production.getSymbolName(TERMINALS.get(i))));
        }
        System.out.print(" | GOTO");
        // 打印非终端符号列
        for (int i = 0; i < NON_TERMINALS.size(); i++) {
            System.out.print(String.format(" %-6s", Production.getSymbolName(NON_TERMINALS.get(i))));
        }
        System.out.println();
        System.out.println(repeatStr("-", 5 + 3 + TERMINALS.size() * 7 + 3 + NON_TERMINALS.size() * 7)); // 分隔线

        // 打印表内容
        for (int state = 0; state < numStates; state++) {
            System.out.print(String.format("%-5d | ", state)); // 状态号

            // 打印 ACTION 行
            System.out.print("      "); // 为 ACTION 留出空间
            for (int i = 0; i < TERMINALS.size(); i++) {
                SymbolType terminal = TERMINALS.get(i);
                Action action = getAction(state, terminal);
                System.out.print(String.format(" %-6s", action.toString()));
            }
            System.out.print(" |      "); // 分隔符和 GOTO 留出空间

            // 打印 GOTO 行
            for (int i = 0; i < NON_TERMINALS.size(); i++) {
                SymbolType nonTerminal = NON_TERMINALS.get(i);
                int nextState = getGoto(state, nonTerminal);
                String gotoStr = (nextState > 0) ? String.valueOf(nextState) : ""; // GOTO 0 通常表示错误或未定义，不打印数字
                System.out.print(String.format(" %-6s", gotoStr));
            }
            System.out.println();
        }
        System.out.println("===============================");
    }

    // 辅助方法：重复字符串
    private String repeatStr(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    // 暴露符号索引映射，可能在 Parser 中需要
    public Map<SymbolType, Integer> getTerminalIndexMap() {
        return Collections.unmodifiableMap(terminalIndexMap);
    }

    public Map<SymbolType, Integer> getNonTerminalIndexMap() {
        return Collections.unmodifiableMap(nonTerminalIndexMap);
    }

    public int getNumStates() {
        return numStates;
    }
}
