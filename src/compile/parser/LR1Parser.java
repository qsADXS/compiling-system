package src.compile.parser;

import src.compile.lexer.TokenType;
import src.compile.lexer.Token;
import src.compile.parser.algorithm.*;
import src.compile.parser.ast.*;
import src.compile.parser.tac.*;
import src.compile.parser.tac.Address.Address;
import src.compile.parser.tac.Address.ConstantAddress;
import src.compile.parser.tac.Address.NameAddress;
import src.compile.parser.tac.Instruction.*;
import src.compile.parser.algorithm.ParseTable.Action;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;



/**
 * LR(1) 分析器。
 * 该类负责根据输入的 Token 序列执行 LR(1) 分析算法，
 * 主要功能包括：
 * 1. 使用 LR(1) 分析表 ({@link ParseTable}) 对 Token 序列进行语法分析。
 * 2. 在分析过程中构建抽象语法树 (AST, {@link ASTNode})。
 * 3. 在 AST 构建完成后，遍历 AST 生成三地址码 (TAC, {@link Instruction})。
 * 它还包含错误处理和调试输出的功能。
 */
public class LR1Parser {
    /**
     * 状态栈。在 LR 分析过程中，用于存储已识别的文法状态的 ID。
     */
    private Stack<Integer> stateStack;
    /**
     * 符号栈。在 LR 分析过程中，用于存储已移入的 Token 或已规约生成的 AST 节点。
     * 栈中元素的类型可以是 {@link Token} 或 {@link ASTNode}。
     */
    private Stack<Object> symbolStack; // Can contain Token or ASTNode

    /**
     * 输入的 Token 序列，由词法分析器生成。
     */
    private List<Token> tokens;
    /**
     * 指向 {@link #tokens} 列表中当前待处理 Token 的索引。
     */
    private int currentTokenIndex;
    /**
     * LR(1) 分析表，包含了 ACTION 表和 GOTO 表，指导分析器的动作。
     * 由 {@link LRParserGenerator} 生成。
     */
    private ParseTable parseTable;
    /**
     * 语法分析成功后生成的抽象语法树的根节点。
     */
    private ASTNode rootNode;

    // TAC 生成相关
    /**
     * 三地址码生成器，负责将 AST 节点转换为三地址指令。
     */
    private TACGenerator tacGenerator;
    /**
     * 存储生成的三地址码指令的列表。
     */
    private List<Instruction> threeAddressCode;
    /**
     * 调试模式标志。如果为 true，则在解析过程中打印详细的步骤信息。
     */
    private boolean debugMode = true;
    /**
     * 解析步骤计数器，用于在调试模式下标记每个解析步骤。
     */
    private int stepCounter = 1;

    /**
     * 构造一个 LR1Parser 实例。
     * 初始化状态栈、符号栈，设置输入的 Token 序列，
     * 生成 LR(1) 分析表，并初始化三地址码生成器。
     *
     * @param tokens 词法分析器生成的 Token 列表。
     * @throws IllegalStateException 如果分析表未能成功构建。
     */
    public LR1Parser(List<Token> tokens) {
        this.stateStack = new Stack<>();
        this.symbolStack = new Stack<>();
        this.tokens = tokens;
        this.currentTokenIndex = 0;
        LRParserGenerator generator = new LRParserGenerator();
        this.parseTable = generator.buildParseTable(); // 这一步的结果至关重要

        if (this.parseTable == null || this.parseTable.getActionTable().isEmpty()) {
            // 如果分析表为空或未正确生成，解析过程必然失败
            System.err.println("错误：LR1Parser 初始化失败 - 分析表为空或未初始化。");
            // 考虑抛出异常或设置一个错误状态
            throw new IllegalStateException("解析表未能成功构建。");
        }
        this.stateStack.push(0); // 初始状态压栈，通常是状态 0

        // 1. 首先初始化 LR1Parser 用来存储指令的列表
        this.threeAddressCode = new ArrayList<>();
        // 2. 然后，在创建 TACGenerator 对象时，将这个列表传递过去
        this.tacGenerator = new TACGenerator(this.threeAddressCode);
    }

    public ParseTable getParseTable() {
        return parseTable;
    }

    public List<Instruction> getThreeAddressCode() {
        return threeAddressCode;
    }

    /**
     * 格式化状态栈内容为字符串，用于调试输出。
     *
     * @return 表示状态栈内容的字符串，例如 "[0 5 12]"。
     */
    private String formatStateStack() {
        return stateStack.stream().map(String::valueOf).collect(Collectors.joining(" "));
    }

    /**
     * 格式化符号栈内容为字符串，用于调试输出。
     * 栈中元素可能是 Token 或 ASTNode。
     *
     * @return 表示符号栈内容的字符串。
     */
    private String formatSymbolStack() {
        return symbolStack.stream().map(s -> {
            if (s instanceof src.compile.lexer.Token) {
                return ((src.compile.lexer.Token) s).value; // 显示 Token 的值
            } else if (s instanceof ASTNode) { // 使用 src.compile.parser.ast.ASTNode
                return ((ASTNode) s).getNodeTypeString(); // 显示 AST 节点的类型字符串
            } else if (s instanceof SymbolType) { // 有时符号栈可能直接暂存文法符号（虽然在此实现中不常见）
                return ((SymbolType) s).name(); // 显示文法符号的名称
            }
            return s.toString(); // 其他情况，使用默认的 toString
        }).collect(Collectors.joining(" "));
    }

    /**
     * 格式化剩余输入 Token 序列为字符串，用于调试输出。
     *
     * @return 表示剩余输入 Token 的字符串，例如 "id + num $ (EOF)"。
     */
    private String formatRemainingInput() {
        StringBuilder sb = new StringBuilder();
        for (int i = currentTokenIndex; i < tokens.size(); i++) {
            sb.append(tokens.get(i).value).append(" ");
            if (sb.length() > 50) { // 限制输出长度以保持可读性
                sb.append("...");
                break;
            }
        }
        if (currentTokenIndex >= tokens.size()) { // 如果所有 Token 都已处理
            sb.append("$ (EOF)"); // 通常用 $ 表示输入结束
        }
        return sb.toString();
    }

    /**
     * 打印当前解析步骤的详细信息，用于调试。
     * 包括状态栈、符号栈、当前输入、剩余输入和执行的动作。
     *
     * @param actionDescription 对当前执行动作的描述字符串。
     */
    private void printParseStep(String actionDescription) {
        if (debugMode) {
            System.out.println("----------------------------------------------------");
            System.out.printf("步骤: %d\n", stepCounter++);
            System.out.printf("状态栈: [%s]\n", formatStateStack());
            System.out.printf("符号栈: [%s]\n", formatSymbolStack());
            // 获取当前 Token，如果已到末尾则特殊处理
            Token currentDisplayToken = (currentTokenIndex < tokens.size()) ? tokens.get(currentTokenIndex) : new Token(TokenType.EOF, "$", -1, -1);
            System.out.printf("当前输入: %s (文法符号: %s)\n", currentDisplayToken.value, SymbolMapper.mapToken(currentDisplayToken));
            System.out.printf("剩余输入: %s\n", formatRemainingInput());
            System.out.printf("动作: %s\n", actionDescription);
        }
    }


    /**
     * 执行LR(1)语法分析过程，将词法单元（Tokens）序列转换为抽象语法树（AST）。
     * 该方法是语法分析器的核心驱动程序。它模拟了一个LR(1)自动机的行为，
     * 使用一个状态栈、一个符号栈以及一个预先构建的LR(1)分析表来决定对输入词法单元串采取的动作
     * （移入、规约、接受或报错）。
     *
     * 主要步骤包括：
     * 1.  循环处理输入词法单元流，直到接受输入或遇到不可恢复的错误。
     * 2.  根据当前状态栈顶的状态和当前的输入符号（从词法单元映射而来），查询LR分析表以获取下一个动作（移入、规约、接受）。
     * 3.  执行相应的动作：
     * a.  **移入 (SHIFT)**：将当前词法单元（及其对应的符号）和分析表指定的新状态分别压入符号栈和状态栈，然后前进到下一个输入词法单元。
     * b.  **规约 (REDUCE)**：根据分析表指定的产生式，从符号栈和状态栈中弹出相应数量的符号和状态。
     * 然后，使用这些弹出的符号（它们构成了产生式的右部）来构建一个新的AST节点（代表产生式的左部非终结符）。
     * 这个新创建的AST节点（或代表非终结符的符号）被压入符号栈。
     * 接着，根据当前状态栈顶的状态和被规约出的非终结符，查询GOTO表以确定下一个状态，并将该状态压入状态栈。
     * 在此项目中，AST节点的创建可能委托给 `createASTNode` 方法 (在 `handleReduceAction` 内部调用)。
     * c.  **接受 (ACCEPT)**：当遇到接受动作时，表示整个输入串已成功匹配文法的开始符号。
     * 此时，符号栈顶应该包含整个程序的AST根节点。
     * 4.  **错误处理与恢复**:
     * a.  如果遇到无效的词法单元（如词法分析器产生的错误标记），会尝试跳过该词法单元。
     * b.  如果在分析表中找不到对应动作（即发生语法错误），方法会调用 `handleSyntaxError` 报告错误，
     * 并尝试进行恐慌模式（panic mode）错误恢复：跳过输入词法单元，直到找到一个已知的同步符号
     * （如分号或右大括号）或一个可以继续解析的状态。
     * c.  如果错误无法恢复，则解析失败并返回 `null`。
     * 5.  **三地址码生成**:
     * a.  在成功接受输入并构建了完整的AST之后，此方法会启动三地址码 (TAC) 的生成过程。
     * b.  它会调用AST根节点的 `generateTac` 方法，并传入一个 `TACGenerator` 实例。
     * `TACGenerator` 负责管理符号表和收集生成的TAC指令。
     * 6.  **调试信息**: 如果 `debugMode` 标志被设置，该方法会打印详细的解析步骤、错误信息、
     * 最终的AST结构、生成的三地址码以及符号表内容，有助于调试语法分析器和TAC生成器。
     *
     * @return 如果解析成功，返回构建的抽象语法树的根节点 ({@link ASTNode})；
     * 如果解析过程中遇到不可恢复的错误，则返回 `null`。
     * @throws RuntimeException 如果在解析过程中遇到未知的动作类型（通常表示分析表或解析器逻辑存在问题）。
     */
    public ASTNode parse() {
        // 调试模式下，打印LR1解析过程开始的信息
        if (debugMode) {
            System.out.println("开始 LR1 解析过程...");
        }
        // 无限循环，直到接受或遇到无法恢复的错误
        while (true) {
            // 获取当前状态栈顶的状态
            int currentState = stateStack.peek();
            // 获取当前待处理的词法单元（Token）
            Token currentToken = getCurrentToken();
            // 将当前词法单元映射为语法分析器使用的符号类型
            SymbolType currentSymbolType = SymbolMapper.mapToken(currentToken);

            // 检查词法单元是否有效
            if (currentSymbolType == SymbolType.T_ERROR) {
                // 处理无效词法单元的语法错误
                handleSyntaxError(currentToken, "在映射过程中遇到无效词法单元");
                // 如果当前词法单元不是最后一个词法单元
                if (currentTokenIndex < tokens.size() - 1) {
                    // 跳过当前无效词法单元，处理下一个
                    currentTokenIndex++;
                } else {
                    // 如果是文件末尾的无效词法单元，则无法恢复
                    if (debugMode) {
                        printParseStep("错误 - 无效词法单元，无法在文件末尾恢复。解析失败。");
                    }
                    System.err.println("语法错误在行 " + currentToken.line + ":" + currentToken.pos +
                            " - 无效词法单元: " + currentToken.value + ". 无法在文件末尾恢复。");
                    // 解析失败，返回null
                    return null;
                }
                // 继续下一轮解析循环
                continue;
            }

            // 从分析表中获取当前状态和当前符号对应的动作
            ParseTable.Action action = parseTable.getAction(currentState, currentSymbolType);

            // 如果分析表中没有找到对应的动作，则进入错误恢复流程
            if (action == null) {
                handleSyntaxError(currentToken, "分析表中状态 " + currentState + " 和符号 " + currentSymbolType + " 没有对应的动作");
                boolean recovered = false; // 标记是否成功恢复
                // 尝试跳过一些词法单元以找到一个可以继续解析的状态
                while (currentTokenIndex < tokens.size()) {
                    currentTokenIndex++; // 向前移动到下一个词法单元
                    Token nextToken = getCurrentToken(); // 获取下一个词法单元
                    // 如果到达文件末尾且已处理完所有词法单元，则中断恢复
                    if (nextToken.type == TokenType.EOF && currentTokenIndex >= tokens.size() - 1) break;
                    // 映射下一个词法单元的符号类型
                    SymbolType nextSymbolType = SymbolMapper.mapToken(nextToken);
                    // 检查当前状态栈顶的状态与下一个符号是否在分析表中有动作
                    if (parseTable.getAction(stateStack.peek(), nextSymbolType) != null) {
                        if (debugMode) System.out.println("错误恢复：跳过到词法单元 " + nextToken.value);
                        recovered = true; // 标记成功恢复
                        break; // 找到可恢复点，退出恢复循环
                    }
                    // 常见的同步符号，如分号或右大括号，可以作为恢复点
                    if (nextSymbolType == SymbolType.T_SEMICOLON || nextSymbolType == SymbolType.T_RBRACE) {
                        if (debugMode) System.out.println("错误恢复：跳过到同步词法单元 " + nextToken.value);
                        recovered = true; // 标记成功恢复
                        break; // 找到同步符号，退出恢复循环
                    }
                }
                // 如果错误恢复失败
                if (!recovered) {
                    if (debugMode) printParseStep("错误 - 没有动作且无法恢复。解析失败。");
                    System.err.println("语法错误：无法恢复的情况。");
                    return null; // 解析失败，返回null
                }
                // 如果成功恢复，继续下一轮解析循环
                continue;
            }

            // 根据分析表中的动作类型执行相应的操作
            switch (action.type) {
                case SHIFT: // 移入动作
                    // 打印移入操作的调试信息
                    printParseStep("移入状态 " + action.stateOrProduction + "，使用词法单元 '" + currentToken.value + "'");
                    // 执行移入操作：将新状态和当前词法单元压入栈
                    handleShiftAction(action, currentToken);
                    break;
                case REDUCE: // 规约动作
                    // 获取要规约的产生式
                    Production productionToReduce = parseTable.getProduction(action.stateOrProduction);
                    // 打印规约操作的调试信息
                    printParseStep("通过产生式 " + action.stateOrProduction + " 进行规约: " + productionToReduce.toString());
                    // 执行规约操作：根据产生式从栈中弹出符号和状态，并将产生式左侧的非终结符压入符号栈，然后根据GOTO表确定新状态
                    // 在此过程中，可能会创建AST节点并通过 createASTNode 隐式传递 tacGenerator
                    handleReduceAction(action);
                    break;
                case ACCEPT: // 接受动作
                    // 打印接受操作的调试信息，表示解析成功
                    printParseStep("接受。解析成功。");

                    // 尝试从符号栈顶获取AST的根节点
                    if (!symbolStack.isEmpty() && symbolStack.peek() instanceof ASTNode) {
                        // 如果符号栈不为空且栈顶是ASTNode实例，则将其设为根节点
                        this.rootNode = (ASTNode) symbolStack.peek();
                    } else if (symbolStack.isEmpty()) {
                        // 如果符号栈为空，则无法获取根节点
                        if (debugMode) {
                            System.err.println("警告：接受状态时符号栈为空。无法检索 AST 根节点。");
                        }
                        this.rootNode = null;
                    } else {
                        // 如果符号栈顶不是ASTNode实例，则记录警告
                        if (debugMode) {
                            System.err.println("警告：接受状态时符号栈顶不是 ASTNode：" +
                                    (symbolStack.peek() != null ? symbolStack.peek().getClass().getName() : "null"));
                        }
                        this.rootNode = null;
                    }

                    // --- 开始在抽象语法树 (AST) 上进行三地址码 (TAC) 生成过程 ---
                    if (this.rootNode != null) {
                        if (debugMode) {
                            System.out.println("\n开始在 AST 上进行 TAC 生成...");
                        }
                        try {
                            // 调用AST根节点的 generateTac 方法开始生成三地址码
                            // TACGenerator 的符号表应该已经准备好（例如，全局作用域已初始化）
                            this.rootNode.generateTac(this.tacGenerator);
                            if (debugMode) {
                                System.out.println("TAC 生成过程完成。");
                            }
                        } catch (Exception e) {
                            // 捕获并打印TAC生成过程中发生的任何异常
                            System.err.println("TAC 生成过程中出错：" + e.getMessage());
                            e.printStackTrace();
                            // 可选：清除已生成的三地址码或进行其他错误处理
                        }
                    } else {
                        // 如果AST根节点为空，则无法开始TAC生成
                        if (debugMode) {
                            System.err.println("无法开始 TAC 生成过程：AST 根节点为空。");
                        }
                    }
                    // --- TAC 生成过程结束 ---

                    // 调试模式下，打印最终的AST根节点和生成的三地址码
                    if (debugMode) {
                        System.out.println("\n最终 AST 根节点：");
                        // 使用AST节点的toString方法打印（或考虑使用更合适的AST打印器）
                        System.out.println(rootNode != null ? rootNode.toString() : "null");
                        System.out.println("\n生成的三地址码（四元式）：");
                        if (threeAddressCode.isEmpty()) {
                            System.out.println("(未生成 TAC 或 TAC 生成过程失败/未执行)");
                        } else {
                            // 遍历并打印所有生成的三地址码指令
                            for (Instruction instr : threeAddressCode) {
                                System.out.println(instr.toString());
                            }
                        }
                        // 打印符号表管理器中所有曾经添加过的符号
                        tacGenerator.getSymbolTableManager().printAllSymbolsEverAdded();
                    }
                    // 返回解析得到的AST根节点
                    return rootNode;
                default: // 未知动作类型
                    // 构造错误信息
                    String errorMessage = "未知的动作类型，位于行 " +
                            (currentToken != null ? currentToken.line : -1) + ":" + (currentToken != null ? currentToken.pos : -1) +
                            "，状态 " + currentState + "，符号 " + currentSymbolType;
                    if (debugMode) printParseStep("错误 - " + errorMessage);
                    // 抛出运行时异常，表示解析器内部错误或分析表损坏
                    throw new RuntimeException(errorMessage);
            }
        }
    }

    /**
     * 处理LR分析过程中的移入（SHIFT）动作。
     * 当分析表指示对当前输入符号执行移入操作时调用此方法。
     *
     * @param action 包含要移入的状态的Action对象。
     * @param token 当前正在处理的词法单元（Token），它将被压入符号栈。
     */
    private void handleShiftAction(Action action, Token token) {
        // 将动作中指定的新状态（action.stateOrProduction）压入状态栈
        stateStack.push(action.stateOrProduction);
        // 将当前处理的词法单元（token）压入符号栈
        symbolStack.push(token);
        // 移动到下一个输入词法单元
        currentTokenIndex++;
    }

    /**
     * 处理LR分析过程中的规约（REDUCE）动作。
     * 当分析表指示根据某个产生式进行规约操作时调用此方法。
     *
     * @param action 包含用于规约的产生式编号的Action对象。
     * @throws RuntimeException 如果产生式ID无效，或在规约过程中发生栈下溢，或GOTO表查找失败。
     */
    private void handleReduceAction(Action action) throws RuntimeException {
        // 根据action中的产生式编号从分析表中获取产生式对象
        Production production = parseTable.getProduction(action.stateOrProduction);
        // 检查产生式是否存在
        if (production == null) {
            String errorMsg = "规约时产生式 ID 无效：" + action.stateOrProduction;
            if(debugMode) printParseStep("错误 - " + errorMsg);
            // 抛出运行时异常，因为这是解析器内部或分析表配置的严重错误
            throw new RuntimeException(errorMsg);
        }

        // 创建一个列表，用于存放从符号栈中弹出的、与产生式右部对应的符号
        List<Object> rhsSymbolsPopped = new ArrayList<>();
        // 计算需要从栈中弹出的符号数量
        // 如果是ε产生式 (epsilon production)，则不弹出任何符号 (popCount = 0)
        // 否则，弹出数量等于产生式右部符号的个数
        int popCount = production.isEpsilon() ? 0 : production.right.size();

        // 循环弹出状态和符号
        for (int i = 0; i < popCount; i++) {
            // 检查状态栈或符号栈是否为空，防止栈下溢
            if (stateStack.isEmpty() || symbolStack.isEmpty()) {
                String errorMsg = "对产生式 " + production.toString() + " 进行规约时栈下溢。" +
                        "期望 " + popCount + " 个符号，但栈太浅。";
                if(debugMode) printParseStep("错误 - " + errorMsg);
                throw new RuntimeException(errorMsg);
            }
            // 从状态栈中弹出一个状态
            stateStack.pop();
            // 从符号栈中弹出一个符号，并将其添加到rhsSymbolsPopped列表的开头，以保持原始顺序
            rhsSymbolsPopped.add(0, symbolStack.pop());
        }

        // 根据产生式和弹出的右部符号创建AST（抽象语法树）节点。
        // tacGenerator (三地址码生成器) 被传递给AST节点构造器，
        // 但AST节点通常不会在构造时立即生成TAC，而是在后续的 generateTac 方法中按需使用它。
        ASTNode node = createASTNode(production, rhsSymbolsPopped, this.tacGenerator);

        // 获取当前状态栈顶的状态，这将作为GOTO查找的起始状态
        int GOTO_currentState = stateStack.peek();
        // 获取产生式左部的非终结符
        SymbolType lhsNonTerminal = production.left;
        // 在GOTO表中查找 (当前状态, 左部非终结符)对应的下一个状态
        Integer nextState = parseTable.getGoto(GOTO_currentState, lhsNonTerminal);

        // 检查GOTO表查找结果是否有效
        if (nextState == null || nextState == -1) { // -1 通常也表示GOTO失败
            String errorMsg = "状态 " + GOTO_currentState + " 和符号 " + lhsNonTerminal + " 没有 GOTO 条目";
            if(debugMode) {
                // 打印详细的调试信息，帮助定位GOTO失败的原因
                System.err.println("当前状态栈: " + formatStateStack());
                System.err.println("当前符号栈: " + formatSymbolStack());
                System.err.println("规约的产生式: " + production);
                System.err.println("GOTO 的左部符号: " + lhsNonTerminal);
                System.err.println("GOTO 的栈顶状态: " + GOTO_currentState);
                printParseStep("错误 - " + errorMsg);
            }
            throw new RuntimeException(errorMsg);
        }

        // 将GOTO查找到的下一个状态压入状态栈
        stateStack.push(nextState);
        // 将新创建的AST节点（代表产生式左部的非终结符）压入符号栈
        symbolStack.push(node);

        // 特殊处理：如果规约的是文法的增广开始符号 (augmented start symbol)，
        // 例如 S' -> Program，那么新创建的节点 'node' 就是整个程序的根AST节点。
        // 假设产生式列表中的第0个产生式是增广产生式 (如 S' -> Program)。
        if (production.equals(this.parseTable.getProduction(0))) {
            // 如果 S' -> Program，并且生成的节点确实是 ProgramNode 类型（或者其子类型），
            // 则将其设置为解析器的根节点。
            if (node instanceof ProgramNode) {
                rootNode = node;
            } else if (node instanceof BlockNode && production.left == SymbolType.NT_PROGRAM) {
                // 这种情况可能对应于 Program -> Block 这样的产生式，
                // 如果文法结构允许 Program 直接规约为 BlockNode，并且 Program 是起始符号（非增广）。
                // 如果 S' -> Program 已经在上面处理，这个赋值可能是多余的。
                // 为了通用性，如果直接 Program 规约是目标，也可以设置。
                // rootNode = node; // 或者如果需要一致性，可以创建一个 ProgramNode 包装器。
            } else if (node != null && production.left == SymbolType.NT_AUGMENTED_START){
                // 这是一个更通用的检查，如果产生式的左部确实是增广开始符号 (NT_AUGMENTED_START)，
                // 那么规约后得到的 AST 节点 'node' 就应该是整个解析树的根。
                rootNode = node;
            }
        }


        // 如果启用了调试模式，打印关于此次规约操作的详细信息
        if (debugMode) {
            System.out.println("   (规约到 " + lhsNonTerminal + ", GOTO 状态 " + nextState + ")");
        }
    }

    // createASTNode 将不再直接生成 TAC 指令。
    // 它只构造 AST 节点。
    // 传递 'gen' 参数是为了以防 ASTNode 构造函数在构造期间需要它来进行符号表访问（很少见）。
    private ASTNode createASTNode(Production production, List<Object> rhsSymbols, TACGenerator gen) {
        // 获取产生式右部第一个符号的行号，如果右部为空或第一个符号不是 Token，则使用默认或先前确定的行号。
        int line = getLineNumber(rhsSymbols);
        // 获取产生式右部第一个符号的位置，如果右部为空或第一个符号不是 Token，则使用默认或先前确定的位置。
        int pos = getPos(rhsSymbols);

        // 根据产生式左部的非终结符类型来创建对应的 AST 节点
        switch (production.left) {
            case NT_AUGMENTED_START: // S' -> program  （增广开始符号）
                // 如果右部符号列表不为空，并且第一个符号已经是 ASTNode (通常是 ProgramNode)
                if (!rhsSymbols.isEmpty() && rhsSymbols.get(0) instanceof ASTNode) {
                    return (ASTNode) rhsSymbols.get(0); // 直接返回该节点
                }
                // 否则，创建一个新的 ProgramNode (这通常发生在 program 为空的情况，但更常见的是上面那个 if 分支)
                return new ProgramNode(null, line, pos);

            case NT_PROGRAM: // program -> block （程序定义）
                // 检查右部的第一个符号是否为 BlockNode
                if (rhsSymbols.get(0) instanceof BlockNode) {
                    // 创建 ProgramNode，其子节点是 BlockNode
                    ProgramNode progNode = new ProgramNode((BlockNode) rhsSymbols.get(0), line, pos);
                    return progNode;
                }
                // 如果类型不匹配，则抛出类型转换异常
                throw new ClassCastException("NT_PROGRAM 期望 BlockNode 但得到 " + rhsSymbols.get(0).getClass().getSimpleName());

            case NT_BLOCK: // block -> { decls stmts } 或 block -> { stmts } （程序块定义）
                BlockNode blockNode = new BlockNode(line, pos); // 创建空的块节点
                // rhsSymbols[0] 应该是 '{', rhsSymbols[1] 是 decls 或 stmts, rhsSymbols[2] 是 stmts 或 '}'
                Object declsOrStmtsSymbol = rhsSymbols.get(1); // 获取第二个符号 (可能是声明或语句)
                Object stmtsOrRbraceSymbol = rhsSymbols.get(2); // 获取第三个符号 (可能是语句或右大括号)

                // 情况1: block -> { decls stmts }
                if (declsOrStmtsSymbol instanceof DeclsNode) {
                    blockNode.setDeclarations((DeclsNode) declsOrStmtsSymbol); // 设置声明部分
                    // 接着的符号应该是语句部分
                    if (stmtsOrRbraceSymbol instanceof StmtsNode) {
                        blockNode.setStatements((StmtsNode) stmtsOrRbraceSymbol); // 设置语句部分
                    }
                    // 如果 decls 后面不是 stmts (例如 { decls } 而 stmts 是 ε)，则 stmtsOrRbraceSymbol 可能是 '}'
                    // 这种情况下，StmtsNode 应该在 StmtsNode 的产生式中通过 ε 规则创建并为空。
                }
                // 情况2: block -> { stmts } (即 decls 是 ε)
                else if (declsOrStmtsSymbol instanceof StmtsNode) {
                    blockNode.setStatements((StmtsNode) declsOrStmtsSymbol); // 设置语句部分
                }
                // 如果 { } 为空块, declsOrStmtsSymbol 可能是 '}' (因为decls和stmts都是epsilon)
                // 这种情况下，DeclsNode 和 StmtsNode 会是空的，在各自的产生式中创建。
                return blockNode;

            case NT_DECLS: // decls → decls decl | ε （声明列表）
                DeclsNode declsNode;
                if (production.isEpsilon()) { // decls → ε (空声明)
                    declsNode = new DeclsNode(line, pos); // 创建空的声明列表节点
                } else { // decls → decls decl (递归定义)
                    declsNode = (DeclsNode) rhsSymbols.get(0); // 获取已有的声明列表节点 (decls)
                    declsNode.addDeclaration((DeclNode) rhsSymbols.get(1)); // 添加新的声明 (decl)
                }
                return declsNode;

            case NT_DECL: // decl → type id; （单个声明语句）
                DeclNode declNode = new DeclNode(
                        (TypeNode) rhsSymbols.get(0), // 类型节点
                        ((Token) rhsSymbols.get(1)).value, // 标识符名称 (从 Token 中获取)
                        line, pos
                );
                return declNode;

            case NT_TYPE: // type → type[num] | basic （类型定义：数组或基本类型）
                if (production.right.size() == 1 && production.right.get(0) == SymbolType.T_BASIC) { // type -> basic (基本类型)
                    String typeNameStr = ((Token) rhsSymbols.get(0)).value; // 获取基本类型名称 (如 "int", "float")

                    TypeNode basicTypeNode = new TypeNode(typeNameStr, line, pos);

                    basicTypeNode.place = new NameAddress(typeNameStr);
                    return basicTypeNode;
                } else { // type -> type[num] (数组类型)
                    TypeNode baseType = (TypeNode) rhsSymbols.get(0); // 数组的基类型
                    Token numToken = (src.compile.lexer.Token) rhsSymbols.get(2); // 'num' Token，代表数组大小
                    int size = Integer.parseInt(numToken.value); // 将 Token 的值转换为整数大小
                    ArrayTypeNode arrayTypeNode = new ArrayTypeNode(baseType, size, line, pos);
                    // 同样，ArrayTypeNode 的 place 的含义需要明确。
                    // 如果是描述性字符串，应该是一个独立的字段。如果是 Address，则需要是 Address 对象。
                    // String arrayTypeDesc = "array(" + (baseType.getPlace() != null ? baseType.getPlace().toString() : baseType.getTypeName()) + ", " + size + ")";
                    // arrayTypeNode.place = new NameAddress(arrayTypeDesc); // {{ 修改：确保 place 是 Address 类型 }}

                    String baseTypeNameForDesc;
                    if (baseType.place != null && baseType.place instanceof NameAddress) { // 假设 baseType.place (一个 NameAddress) 存储了其类型名
                        baseTypeNameForDesc = baseType.place.toString();
                    } else {
                        baseTypeNameForDesc = baseType.getTypeName(); // 后备方案，直接获取类型名字符串
                    }
                    // 创建一个描述数组类型的 NameAddress，例如 "array(int, 10)"
                    arrayTypeNode.place = new NameAddress("array(" + baseTypeNameForDesc + ", " + size + ")"); // {{ 修改：确保 place 是 Address 类型 }}
                    return arrayTypeNode;
                }

            case NT_STMTS: // stmts → stmts stmt | ε （语句列表）
                StmtsNode stmtsNode;
                if (production.isEpsilon()) { // stmts → ε (空语句列表)
                    stmtsNode = new StmtsNode(line, pos); // 创建空的语句列表节点
                } else { // stmts → stmts stmt (递归定义)
                    stmtsNode = (StmtsNode) rhsSymbols.get(0); // 获取已有的语句列表节点 (stmts)
                    stmtsNode.addStatement((StmtNode) rhsSymbols.get(1)); // 添加新的语句 (stmt)
                }
                return stmtsNode;

            // 处理所有类型的语句节点 (普通语句，匹配的if语句，未匹配的if/else语句)
            case NT_STMT:           // stmt -> matched_stmt | unmatched_stmt | other_stmt_types (如 assign, while 等)
            case NT_MATCHED_STMT:   // matched_stmt -> IF expr THEN matched_stmt ELSE matched_stmt | other_simple_stmts
            case NT_UNMATCHED_STMT: // unmatched_stmt -> IF expr THEN stmt | IF expr THEN matched_stmt ELSE unmatched_stmt
                // {{ 注解：所有语句相关的非终结符（stmt, matched_stmt, unmatched_stmt）的 AST 节点创建 }}
                // {{ 现在都统一委托给 createStmtNode 方法。}}
                // {{ createStmtNode 会分析传入的 'production' (即当前的归约产生式) }}
                // {{ 来决定是创建具体的语句节点 (如 AssignStmtNode, IfStmtNode)，}}
                // {{ 还是在产生式是简单传递 (如 stmt -> matched_stmt) 时直接返回已构建的子语句节点。}}
                return createStmtNode(production, rhsSymbols, gen); // 委托给专门的语句节点创建方法

            case NT_LOC: // loc → id | loc [ index_symbol ] （左值表达式，可以是变量或数组元素）
                // 检查产生式是否为 loc -> id (简单变量)
                if (production.right.size() == 1 && production.right.get(0) == SymbolType.T_ID) {
                    src.compile.lexer.Token idToken = (src.compile.lexer.Token) rhsSymbols.get(0); // 获取 id Token
                    LocNode idLocNode = new LocNode(idToken.value, line, pos); // 创建 LocNode 代表变量
                    // 对于简单变量，其名称就是其地址的表示。
                    idLocNode.place = new NameAddress(idToken.value); // {{ 修改：确保 place 是 Address 类型，存储变量名 }}
                    return idLocNode;
                } else { // loc -> loc [ index_symbol ] (数组元素访问)
                    // index_symbol 可以是 NT_EXPR 或 T_NUM，取决于具体的语法规则
                    LocNode baseLoc = (LocNode) rhsSymbols.get(0); // 数组基地址 (loc)
                    ExprNode indexExprNode; // 将用于存储最终的索引表达式节点

                    Object indexActualSymbol = rhsSymbols.get(2); // 获取中括号内的实际符号对象 (可能是 ExprNode 或 Token)
                    SymbolType expectedIndexSymbolTypeOnProd = production.right.get(2); // 获取产生式中定义的索引符号类型 (如 T_NUM 或 NT_EXPR)

                    // 情况1: 产生式是 loc -> loc [ T_NUM ] (索引是数字字面量)
                    if (expectedIndexSymbolTypeOnProd == SymbolType.T_NUM) {
                        // 此时 indexActualSymbol 应该是一个 Token
                        if (indexActualSymbol instanceof src.compile.lexer.Token) {
                            src.compile.lexer.Token numToken = (src.compile.lexer.Token) indexActualSymbol;
                            int val = Integer.parseInt(numToken.value); // 将数字 Token 转换为整数
                            // 为数字创建一个 LiteralNode，代表常量索引
                            LiteralNode<Integer> numLiteralNode = new LiteralNode<>(val, "int", line, pos);
                            // 设置 LiteralNode 的 place，与 NT_FACTOR 中的处理类似
                            numLiteralNode.place = new ConstantAddress<>(val); // 常量地址
                            indexExprNode = numLiteralNode; // 索引表达式就是这个数字字面量节点
                        } else {
                            // 如果产生式期望 T_NUM 但实际符号不是 Token，则抛出异常
                            throw new ClassCastException("数组索引期望得到一个词法单元 (Token) 作为数字 (T_NUM)，但在处理产生式 " + production + " 时，实际得到的是 " +
                                    indexActualSymbol.getClass().getName());
                        }
                    }
                    // 情况2: 产生式是 loc -> loc [ NT_EXPR ] (索引是表达式)
                    else if (indexActualSymbol instanceof ExprNode) {
                        // 此时 indexActualSymbol 应该已经是一个 ExprNode (由解析器更早的步骤构建)
                        indexExprNode = (ExprNode) indexActualSymbol; // 索引表达式就是这个已存在的表达式节点
                    }
                    // 意外情况: 索引符号既不是预期的 T_NUM Token 也不是 ExprNode
                    else {
                        throw new RuntimeException("无法为数组索引创建 AST 节点。在处理产生式 " + production + " 时遇到意外的符号类型：" +
                                indexActualSymbol.getClass().getName() + "，而产生式中期望的符号类型是：" + expectedIndexSymbolTypeOnProd);
                    }

                    // 创建数组访问节点
                    ArrayAccessNode arrayLocNode = new ArrayAccessNode(baseLoc, indexExprNode, line, pos);
                    // arrayLocNode 的 place (实际计算出的元素地址) 将在 ArrayAccessNode.generateTac() 中设置。
                    // ArrayAccessNode 内部通常会存储 baseLoc (或其 place) 和 indexExprNode (或其 place)
                    // 以便在 generateTac 时计算最终地址。
                    // 例如: if (baseLoc.getPlace() != null) arrayLocNode.setBaseAddress(baseLoc.getPlace());
                    // 这里的 basePlace 是你之前代码中的一个假设字段，如果它存在且类型为 Address，则 baseLoc.getPlace() 可以直接赋值。
                    // arrayLocNode.basePlace = baseLoc.getPlace(); // 假设 basePlace 是 Address 类型
                    return arrayLocNode;
                }

                // 处理各种二元和一元表达式的非终结符
                // 这些通常遵循 expr -> expr op expr | term 或 term -> term op factor | factor 等模式
                // 以及 expr -> term, term -> factor 等单传递规则
            case NT_BOOL:         // bool -> bool || join | join
            case NT_BOOL_PRIME:   // (可能用于消除左递归的辅助非终结符, 例如: bool -> join bool', bool' -> || join bool' | ε)
                // 或者 bool -> bool OR bool | bool AND bool | NOT bool | (bool) | rel
            case NT_JOIN:         // join -> join && equality | equality
            case NT_EQUALITY:     // equality -> equality == rel | equality != rel | rel
            case NT_REL:          // rel -> expr < expr | expr <= expr | expr > expr | expr >= expr | expr
            case NT_EXPR:         // expr -> expr + term | expr - term | term
            case NT_TERM:         // term -> term * factor | term / factor | factor
                // 单一右部符号: 通常是 X -> Y 形式的产生式，直接返回 Y 对应的 AST 节点
                if (production.right.size() == 1) {
                    if (rhsSymbols.get(0) instanceof ExprNode) {
                        return (ExprNode) rhsSymbols.get(0); // 直接返回已构建的子表达式节点
                    }
                    // 如果右部不是 ExprNode，说明语法或AST构建逻辑有误
                    throw new ClassCastException("对于单个 RHS 产生式 " + production + "，期望 ExprNode，但得到 " + rhsSymbols.get(0).getClass().getName());
                } else { // 二元操作: X -> Y op Z
                    ExprNode leftOperand = (ExprNode) rhsSymbols.get(0);    // 左操作数节点
                    Token operatorToken = (Token) rhsSymbols.get(1);        // 操作符 Token
                    ExprNode rightOperand = (ExprNode) rhsSymbols.get(2);   // 右操作数节点

                    // 特殊处理逻辑与/或操作符 (用于支持短路求值)
                    if ("&&".equals(operatorToken.value) || "||".equals(operatorToken.value)) {
                        // 创建 ShortCircuitExprNode 并返回，它在生成TAC时会处理短路逻辑
                        ShortCircuitExprNode shortCircuitNode = new ShortCircuitExprNode(line, pos, leftOperand, operatorToken.value, rightOperand);
                        return shortCircuitNode;
                    }

                    // 对于其他普通二元操作符
                    BinaryExprNode binExprNode = new BinaryExprNode(leftOperand, rightOperand, operatorToken.value, line, pos);
                    // binExprNode.place (结果存放的地址) 将在 BinaryExprNode.generateTac() 中设置，通常是一个临时变量。
                    return binExprNode;
                }

            case NT_UNARY: // unary→ !unary | -unary | factor （一元表达式）
                if (production.right.size() == 2) { // unary -> op operand (例如: !expr, -expr)
                    src.compile.lexer.Token operatorToken = (src.compile.lexer.Token) rhsSymbols.get(0); // 操作符 Token
                    ExprNode operand = (ExprNode) rhsSymbols.get(1); // 操作数节点
                    UnaryExprNode unaryNode = new UnaryExprNode(operand, operatorToken.value, line, pos);
                    // unaryNode.place (结果存放的地址) 将在 UnaryExprNode.generateTac() 中设置。
                    return unaryNode;
                } else { // unary -> factor (单传递规则)
                    if (rhsSymbols.get(0) instanceof ExprNode) { // factor 本身也是一种 ExprNode
                        return (ExprNode) rhsSymbols.get(0); // 直接返回 factor 对应的节点
                    }
                    throw new ClassCastException("对于 NT_UNARY -> NT_FACTOR，期望 ExprNode，但得到 " + rhsSymbols.get(0).getClass().getName());
                }

            case NT_FACTOR: // factor → (expr) | loc | num | real | true | false （因子）
                Object firstFactorSymbolObj = rhsSymbols.get(0); // 获取产生式右部的第一个符号

                // 情况1: factor -> (expr) (括号表达式)
                if (firstFactorSymbolObj instanceof src.compile.lexer.Token &&
                        SymbolMapper.mapToken((src.compile.lexer.Token)firstFactorSymbolObj) == SymbolType.T_LPAREN) { // 检查是否是左括号
                    // rhsSymbols.get(1) 应该是 expr 对应的 ExprNode
                    ParenExprNode parenNode = new ParenExprNode((ExprNode) rhsSymbols.get(1), line, pos);
                    // parenNode.place 将在 ParenExprNode.generateTac() 中设置，通常是其内部 expr 的 place。
                    return parenNode;
                }
                // 情况2: factor -> loc (左值，可以是变量或数组元素)
                else if (firstFactorSymbolObj instanceof LocNode) {
                    LocNode locNode = (LocNode) firstFactorSymbolObj;
                    // locNode.place (如果是简单ID如 'x') 已在 NT_LOC 中设置为 NameAddress("x")。
                    // 如果是 ArrayAccessNode (如 a[i])，其 place (最终计算的元素地址) 将在其 generateTac 中设置。
                    // 作为右值使用时，generateTac会加载这个地址的值到新的临时变量。
                    return locNode;
                }
                // 情况3: factor -> num | real | true | false (字面量)
                else if (firstFactorSymbolObj instanceof src.compile.lexer.Token) {
                    src.compile.lexer.Token literalToken = (src.compile.lexer.Token) firstFactorSymbolObj;
                    LiteralNode<?> litNode = null; // 泛型字面量节点
                    Address literalAddress = null; // 用于存储字面量对应的地址 (通常是 ConstantAddress)

                    // 使用 SymbolMapper 将 Token 映射到内部的符号类型，以区分 T_NUM, T_REAL 等
                    SymbolType mappedType = SymbolMapper.mapToken(literalToken);
                    switch (mappedType) {
                        case T_NUM: //整数
                            int intVal = Integer.parseInt(literalToken.value);
                            litNode = new LiteralNode<>(intVal, "int", line, pos);
                            literalAddress = new ConstantAddress<>(intVal); // 整数常量地址
                            break;
                        case T_REAL: //浮点数
                            double doubleVal = Double.parseDouble(literalToken.value);
                            litNode = new LiteralNode<>(doubleVal, "float", line, pos);
                            literalAddress = new ConstantAddress<>(doubleVal); // 浮点数常量地址
                            break;
                        case T_TRUE: //布尔真
                            litNode = new LiteralNode<>(true, "bool", line, pos);
                            literalAddress = new ConstantAddress<>(true); // 布尔真常量地址
                            break;
                        case T_FALSE: //布尔假
                            litNode = new LiteralNode<>(false, "bool", line, pos);
                            literalAddress = new ConstantAddress<>(false); // 布尔假常量地址
                            break;
                        default:
                            // 如果 Token 不是预期的字面量类型，则不应在此处处理
                            // (由外层 throw new RuntimeException 捕获)
                            break;
                    }
                    if (litNode != null) {
                        // LiteralNode 的 place 通常在其 generateTac 方法中设置。
                        // 在这里设置是冗余的，但如果必须，则确保类型正确。
                        // 此处设置 place 是为了让 LiteralNode 在被用作其他表达式 (如一元、二元表达式) 的一部分时，
                        // 能够提供其值的直接地址表示 (ConstantAddress)。
                        litNode.place = literalAddress; // {{ 修改：直接赋 Address 对象 }}
                        return litNode;
                    }
                }
                // 如果 factor 的产生式不匹配以上任何一种情况，则抛出运行时异常
                throw new RuntimeException("未处理的 factor 产生式变体或意外的符号：" + production + "，使用 " + rhsSymbols);

            default: // 未显式处理的非终结符
                System.err.println("警告：createASTNode 中没有为 " + production.left + " 提供特定的 ASTNode 创建逻辑。产生式：" + production);
                // 尝试一种通用处理：如果右部第一个符号是 ASTNode，则直接返回它
                // 这可能适用于某些简单的传递规则 A -> B，其中 B 已经被构造成 ASTNode
                if (!rhsSymbols.isEmpty() && rhsSymbols.get(0) instanceof ASTNode) {
                    return (ASTNode) rhsSymbols.get(0);
                }
                // 否则，创建一个通用的 GenericNode，仅包含非终结符名称
                return new GenericNode(production.left.toString(), line, pos);
        }
    }


    /**
     * 根据给定的产生式 (production) 和产生式右部符号列表 (rhsSymbols)，创建相应的语句抽象语法树（AST）节点。
     * 此方法负责处理所有与语句相关的产生式，例如赋值语句、if语句、while语句等。
     *
     * @param production 当前正在归约的产生式规则。
     * @param rhsSymbols 产生式右部的符号列表，这些符号可能是词法单元 (Token) 或已经构建好的子 AST 节点。
     * @param gen        三地址码生成器，在此方法中通常不直接使用，但可能传递给子节点的构造函数（尽管在此代码中未直接体现）。
     * @return 构建完成的 StmtNode (语句节点) 的子类实例。
     * @throws RuntimeException 如果没有找到与产生式匹配的语句构造逻辑，则抛出异常。
     * @throws ClassCastException 如果 rhsSymbols 中的对象类型与预期不符。
     */
    private StmtNode createStmtNode(Production production, List<Object> rhsSymbols, TACGenerator gen) {
        // 获取产生式右部第一个符号的行号和位置，用于新创建的 AST 节点
        int line = getLineNumber(rhsSymbols);
        int pos = getPos(rhsSymbols);

        // {{ 注解：情况1 - 产生式直接将一个 StmtNode 类型的子节点向上传递 }}
        // {{ 例如：stmt -> matched_stmt, 或 matched_stmt -> block (此时 rhsSymbols.get(0) 是 BlockNode, 而 BlockNode 本身也是一个 StmtNode) }}
        // {{ 这确保了如果一个语句非终结符只是包装了另一个已创建的语句节点，我们直接使用那个节点。 }}
        // 检查产生式右部是否只有一个符号，并且该符号已经是 StmtNode 的实例。
        // 这种情况对应于文法中的单元产生式，如 Stmt -> IfStmt，此时 IfStmtNode 已经构建好，直接返回即可。

        if (production.right.size() == 1 && rhsSymbols.size() == 1 && rhsSymbols.get(0) instanceof StmtNode) {
            return (StmtNode) rhsSymbols.get(0);
        }

        // {{ 注解：情况2 - 根据 'production' 的右部结构，创建具体的语句节点。}}
        // {{ 所有具体的语句构造逻辑都集中在这里。}}
        // 下面是根据不同的产生式规则构造特定类型的语句节点。

        // 产生式规则匹配: loc = bool ; (赋值语句)
        // 例如，LHS (产生式左部) 可以是 NT_STMT, NT_MATCHED_STMT 等等，只要其归约到了这个结构。
        if (production.right.size() == 4 && // 产生式右部有4个符号
                production.right.get(0) == SymbolType.NT_LOC &&      // 第一个符号是 Loc (左值)
                production.right.get(1) == SymbolType.T_ASSIGN &&   // 第二个符号是 '=' (赋值号)
                production.right.get(2) == SymbolType.NT_BOOL &&     // 第三个符号是 Bool (表达式，其AST节点类型为ExprNode)
                production.right.get(3) == SymbolType.T_SEMICOLON && // 第四个符号是 ';' (分号)
                // 确保实际的符号栈上的对象是正确的AST节点类型
                rhsSymbols.get(0) instanceof LocNode && // 第一个符号已归约为 LocNode
                rhsSymbols.get(2) instanceof ExprNode) { // 第三个符号已归约为 ExprNode (因为 Bool 最终会成为表达式的一部分)
            LocNode loc = (LocNode) rhsSymbols.get(0);       // 获取 LocNode
            ExprNode expr = (ExprNode) rhsSymbols.get(2);   // 获取 ExprNode
            return new AssignStmtNode(loc, expr, line, pos); // 创建并返回赋值语句节点
        }
        // 产生式规则匹配: if ( bool ) stmt_form (不带 else 的 if 语句)
        // stmt_form 可以是 NT_STMT, NT_MATCHED_STMT, NT_UNMATCHED_STMT 中的一种
        else if (production.right.size() == 5 && // 产生式右部有5个符号
                production.right.get(0) == SymbolType.T_IF &&         // 'if' 关键字
                production.right.get(1) == SymbolType.T_LPAREN &&     // '(' 左括号
                production.right.get(2) == SymbolType.NT_BOOL &&      // bool 条件表达式
                production.right.get(3) == SymbolType.T_RPAREN &&     // ')' 右括号
                // 检查产生式右部的第5个符号是否为语句类型的非终结符 (stmt, matched_stmt, unmatched_stmt)
                (production.right.get(4) == SymbolType.NT_STMT ||
                        production.right.get(4) == SymbolType.NT_MATCHED_STMT ||
                        production.right.get(4) == SymbolType.NT_UNMATCHED_STMT) &&
                // 确保实际的符号栈上的对象是正确的AST节点类型
                rhsSymbols.get(2) instanceof ExprNode && // 条件是 ExprNode
                rhsSymbols.get(4) instanceof StmtNode) { // then 分支是 StmtNode
            ExprNode condition = (ExprNode) rhsSymbols.get(2); // 获取条件表达式节点
            StmtNode thenBranch = (StmtNode) rhsSymbols.get(4); // 获取 then 分支语句节点
            return new IfStmtNode(condition, thenBranch, null, line, pos); // 创建并返回 If 语句节点 (else 分支为 null)
        }
        // 产生式规则匹配: if ( bool ) matched_stmt else stmt_form (带 else 的 if 语句)
        // 为了解决悬挂else问题，then 分支必须是 matched_stmt
        // stmt_form 可以是 NT_STMT, NT_MATCHED_STMT, NT_UNMATCHED_STMT 中的一种
        else if (production.right.size() == 7 && // 产生式右部有7个符号
                production.right.get(0) == SymbolType.T_IF &&            // 'if'
                production.right.get(1) == SymbolType.T_LPAREN &&        // '('
                production.right.get(2) == SymbolType.NT_BOOL &&         // bool 条件
                production.right.get(3) == SymbolType.T_RPAREN &&        // ')'
                production.right.get(4) == SymbolType.NT_MATCHED_STMT && // then 分支必须是 matched_stmt (已匹配语句)
                production.right.get(5) == SymbolType.T_ELSE &&          // 'else'
                // else 分支可以是任意语句形式
                (production.right.get(6) == SymbolType.NT_STMT ||
                        production.right.get(6) == SymbolType.NT_MATCHED_STMT ||
                        production.right.get(6) == SymbolType.NT_UNMATCHED_STMT) &&
                // 确保实际的符号栈上的对象是正确的AST节点类型
                rhsSymbols.get(2) instanceof ExprNode && // 条件是 ExprNode
                rhsSymbols.get(4) instanceof StmtNode && // thenBranch 应该是 StmtNode (具体是 MatchedStmtNode 归约的结果)
                rhsSymbols.get(6) instanceof StmtNode) { // elseBranch 也是 StmtNode
            ExprNode condition = (ExprNode) rhsSymbols.get(2);  // 获取条件表达式节点
            StmtNode thenBranch = (StmtNode) rhsSymbols.get(4); // 获取 then 分支语句节点
            StmtNode elseBranch = (StmtNode) rhsSymbols.get(6); // 获取 else 分支语句节点
            return new IfStmtNode(condition, thenBranch, elseBranch, line, pos); // 创建并返回 If-Else 语句节点
        }// 产生式规则匹配: while ( bool ) stmt_form (while 循环语句)
        // stmt_form 通常是 NT_MATCHED_STMT，以避免循环体内部的悬挂else问题影响循环结构本身
        else if (production.right.size() == 5 && // 产生式右部有5个符号
                production.right.get(0) == SymbolType.T_WHILE &&      // 'while'
                production.right.get(1) == SymbolType.T_LPAREN &&     // '('
                production.right.get(2) == SymbolType.NT_BOOL &&      // bool 条件
                production.right.get(3) == SymbolType.T_RPAREN &&     // ')'
                (production.right.get(4) == SymbolType.NT_STMT ||     // 循环体语句
                        production.right.get(4) == SymbolType.NT_MATCHED_STMT) && // 通常循环体是 matched_stmt
                // 确保实际的符号栈上的对象是正确的AST节点类型
                rhsSymbols.get(2) instanceof ExprNode && // 条件是 ExprNode
                rhsSymbols.get(4) instanceof StmtNode) { // 循环体是 StmtNode
            ExprNode condition = (ExprNode) rhsSymbols.get(2); // 获取条件表达式节点
            StmtNode body = (StmtNode) rhsSymbols.get(4);      // 获取循环体语句节点
            return new WhileStmtNode(condition, body, line, pos); // 创建并返回 While 语句节点
        }
        // 产生式规则匹配: do stmt_form while ( bool ) ; (do-while 循环语句)
        // stmt_form 通常是 NT_MATCHED_STMT
        else if (production.right.size() == 7 && // 产生式右部有7个符号
                production.right.get(0) == SymbolType.T_DO &&        // 'do'
                (production.right.get(1) == SymbolType.NT_STMT ||    // 循环体语句
                        production.right.get(1) == SymbolType.NT_MATCHED_STMT) &&
                production.right.get(2) == SymbolType.T_WHILE &&     // 'while'
                production.right.get(3) == SymbolType.T_LPAREN &&    // '('
                production.right.get(4) == SymbolType.NT_BOOL &&     // bool 条件
                production.right.get(5) == SymbolType.T_RPAREN &&    // ')'
                production.right.get(6) == SymbolType.T_SEMICOLON && // ';'
                // 确保实际的符号栈上的对象是正确的AST节点类型
                rhsSymbols.get(1) instanceof StmtNode && // 循环体是 StmtNode
                rhsSymbols.get(4) instanceof ExprNode) { // 条件是 ExprNode
            StmtNode bodyNode = (StmtNode) rhsSymbols.get(1);       // 获取循环体语句节点
            ExprNode conditionNode = (ExprNode) rhsSymbols.get(4); // 获取条件表达式节点
            return new DoWhileStmtNode(bodyNode, conditionNode, line, pos); // 创建并返回 Do-While 语句节点
        }
        // 产生式规则匹配: break ; (break 语句)
        else if (production.right.size() == 2 && // 产生式右部有2个符号
                production.right.get(0) == SymbolType.T_BREAK &&      // 'break'
                production.right.get(1) == SymbolType.T_SEMICOLON) { // ';'
            return new BreakStmtNode(line, pos); // 创建并返回 Break 语句节点
        }
        // {{ 注解：如果以上所有具体结构都不匹配，说明遇到了一个未处理的语句产生式。 }}
        // {{ 这通常指示文法和 AST 创建逻辑之间可能存在不一致，或者需要为新的语句类型添加处理。 }}
        // 如果没有匹配到任何已知的语句产生式结构，则输出错误信息并抛出异常。
        // 这有助于在开发过程中发现文法定义与AST构建逻辑之间的不匹配。
        System.err.println("错误：createStmtNode 方法未能为产生式 " + production + " 创建具体的语句节点。右部符号：" + rhsSymbols);
        throw new RuntimeException("createStmtNode 中未找到与产生式 " + production + " 匹配的语句构造逻辑。");
    }

    /**
     * 从符号列表中获取行号。
     * 它会遍历符号列表，尝试从第一个 Token 或 ASTNode 中提取行号。
     * 如果列表中的符号都没有行号信息（例如，对于空产生式），
     * 它会尝试获取当前正在处理的词法单元的行号作为回退。
     *
     * @param symbols 产生式右部或相关的一组符号对象。
     * @return 提取到的行号。如果无法确定，则返回一个默认值（例如0或当前词法单元的行号）。
     */
    private int getLineNumber(List<Object> symbols) {
        for (Object sym : symbols) { //遍历符号列表
            if (sym instanceof Token) { // 如果符号是 Token 类型
                return ((Token) sym).line; // 直接返回 Token 的行号
            } else if (sym instanceof ASTNode) { // 如果符号是 ASTNode 类型
                // 假设 ASTNode 应该已经记录了行号和位置信息。
                // 确保所有 ASTNode 的子类在构造时都初始化了这些信息。
                if (((ASTNode)sym).line != 0) return ((ASTNode)sym).line; // 如果 ASTNode 的行号有效 (非0)，则返回
            }
        }
        // 回退机制：如果符号列表中没有 Token 或 ASTNode 带有行号信息（例如，对于Epsilon产生式）
        Token current = getCurrentToken(); // 获取当前词法分析器正在处理的 Token
        // 如果当前 Token 存在且行号有效 (-1 通常表示无效或未设置)，则返回其行号，否则返回 0 (或一个更合适
        return (current != null && current.line != -1) ? current.line : 0;
    }

    /**
     * 从符号列表中获取位置（列号）。
     * 它会遍历符号列表，尝试从第一个 Token 或 ASTNode 中提取位置信息。
     * 如果列表中的符号都没有位置信息，它会尝试获取当前正在处理的词法单元的位置作为回退。
     *
     * @param symbols 产生式右部或相关的一组符号对象。
     * @return 提取到的位置信息。如果无法确定，则返回一个默认值（例如0或当前词法单元的位置）。
     */
    private int getPos(List<Object> symbols) {
        for (Object sym : symbols) { // 遍历符号列表
            if (sym instanceof Token) { // 如果符号是 Token 类型
                return ((Token) sym).pos; // 直接返回 Token 的位置
            } else if (sym instanceof ASTNode) { // 如果符号是 ASTNode 类型
                if (((ASTNode)sym).pos != 0) return ((ASTNode)sym).pos; // 如果 ASTNode 的位置有效 (非0)，则返回
            }
        }
        Token current = getCurrentToken(); // 获取当前词法分析器正在处理的 Token
        // 如果当前 Token 存在且位置有效，则返回其位置，否则返回 0
        return (current != null && current.pos != -1) ? current.pos : 0;
    }

    /**
     * 获取当前正在被语法分析器处理的词法单元 (Token)。
     * 它基于 currentTokenIndex 从词法单元列表 tokens 中获取。
     * 如果已到达词法单元列表的末尾，则会构造并返回一个表示文件结束 (EOF) 的特殊 Token。
     * 这个 EOF Token 会尝试使用最后一个有效 Token 的行号，以确保错误报告或后续处理有合理的上下文。
     *
     * @return 当前的 Token；如果到达输入末尾，则返回一个 EOF Token。
     */
    private Token getCurrentToken() {
        if (currentTokenIndex < tokens.size()) { // 检查当前索引是否在词法单元列表的有效范围内
            return tokens.get(currentTokenIndex); // 返回当前索引处的 Token
        }
        // 如果 currentTokenIndex 超出范围，意味着到达了输入的末尾。
        // 创建一个文件结束符 (EOF) Token。
        // EOF Token 的行号：如果 tokens 列表为空，则默认为1；否则使用最后一个 Token 的行号。
        // EOF Token 的位置：默认为0。
        // 确保 EOF Token 至少有一个行号，以便在需要时使用。
        return new Token(TokenType.EOF, "$", tokens.isEmpty() ? 1 : tokens.get(tokens.size()-1).line, 0);
    }

    /**
     * 处理语法错误。
     * 当语法分析过程中检测到错误时调用此方法。
     * 它会构造一条包含错误位置（行号、列号）、错误标记和错误信息的错误消息，并打印到标准错误流。
     * 在调试模式下，还会打印更详细的错误信息到标准输出流。
     * 此方法还包含一个简单的逻辑，以避免在某些特定情况下（如已成功接受输入或解析栈为空时遇到预期的EOF）打印EOF错误。
     *
     * @param token 引发错误的词法单元 (Token)。
     * @param message 描述错误类型的具体消息。
     */
    private void handleSyntaxError(Token token, String message) {
        // 避免在已接受(rootNode != null)或解析失败导致栈空(stateStack.isEmpty())的情况下，
        // 再次打印关于预期EOF的错误。
        if (token.type == TokenType.EOF && (rootNode != null || stateStack.isEmpty())) {
            return; // 不报告此类EOF错误
        }

        // 构建标准错误消息
        String errorMessage = "语法错误在 行 " + token.line + ":" + token.pos +
                " (" + token.value + ") - " + message;

        if (debugMode) { // 如果开启了调试模式
            // 在调试模式下打印详细错误信息到标准输出。
            // 注意：这里的逻辑是为了避免在正常的接受序列中，如果EOF是“出错”的token但实际上有接受动作时打印错误。
            // 主循环应该在调用错误处理之前处理接受(Accept)状态。
            System.out.println("错误信息: " + errorMessage);
        }
        System.err.println(errorMessage); // 将错误信息打印到标准错误流

        // 简单的错误恢复策略：目前只是打印错误。
        // 主解析循环（未在此处显示）可能会尝试通过跳过一些 Token 来进行恢复。
    }

}