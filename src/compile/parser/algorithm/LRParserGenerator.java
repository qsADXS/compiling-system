package src.compile.parser.algorithm;

import javafx.util.Pair;
import src.compile.parser.algorithm.ParseTable.Action;
import java.util.*;

/**
 * @author sixteacher
 * @version 1.0
 * @description LRParserGenerator
 * @date 2025/5/19
 */


// Pair 类是一个简单的辅助类，用于存储两个对象的组合，并正确实现了 equals 和 hashCode
// 例如:
// class Pair<K, V> {
//     public K key;
//     public V value;
//     public Pair(K key, V value) { this.key = key; this.value = value; }
//     @Override public boolean equals(Object o) { /* ... */ }
//     @Override public int hashCode() { /* ... */ }
// }

/**
 * LR(1) 分析器生成器。
 * 该类负责根据给定的文法 {@link Grammar} 构建 LR(1) 项集族（状态机）
 * 和相应的 LR(1) 分析表 ({@link ParseTable})。
 * 主要步骤包括：
 * 1. 预计算所有非终结符的 FIRST 集。
 * 2. 构建 LR(1) 项集族 (Canonical Collection of LR(1) item sets)。
 * - 计算项集的闭包 (closure)。
 * - 计算项集的 GOTO 转换。
 * 3. 根据项集族构建 LR(1) 分析表 (ACTION 表和 GOTO 表)。
 */
public class LRParserGenerator {
    /**
     * 当前分析器生成器使用的文法。
     */
    public Grammar grammar;

    /**
     * 生成的 LR(1) 项集族（状态列表）。每个 {@link LRItemSet} 代表分析器的一个状态。
     */
    public List<LRItemSet> itemSets = new ArrayList<>();

    /**
     * 用于缓存 FIRST(βa) 计算结果的 Map。
     *键是符号序列 β 和展望符 a 的组合 (Pair)，值是对应的 FIRST 集。
     *这有助于避免重复计算，提高效率。
     */
    private Map<Pair<List<SymbolType>, SymbolType>, Set<SymbolType>> firstCache = new HashMap<>();

    /**
     * 预先计算好的所有非终结符的 FIRST 集。
     * 键是非终结符，值是其对应的 FIRST 符号集合 (只包含终结符或 ε)。
     * 在构造函数中通过 {@link #precomputeAllFirstSets()} 初始化。
     */
    private Map<SymbolType, Set<SymbolType>> precomputedFirstSets;

    /**
     * 构造一个 LRParserGenerator 实例。
     * 初始化文法，预计算所有非终结符的 FIRST 集，并构建 LR(1) 项集族。
     */
    public LRParserGenerator() {
        this.grammar = new Grammar(); // 初始化文法
        this.precomputedFirstSets = precomputeAllFirstSets(); // 预计算 FIRST 集
        buildItemSets(); // 构建 LR(1) 项集族，此步骤必须在其他依赖项集的操作之前完成
    }
    /**
     * 计算给定 LR(1) 项集的闭包 (closure)。
     * 闭包操作会向项集中添加新的 LR(1) 项，直到没有更多项可以添加为止。
     * 对于项集 I 中的每个项 [A → α . B β, a]，以及 B 的每个产生式 B → γ，
     * 我们需要将项 [B → .γ, b] 添加到闭包中，其中 b ∈ FIRST(βa)。
     *
     * @param itemSet 初始的 LR(1) 项集。
     * @return 包含所有闭包项的新的 LR(1) 项集。
     */
    private LRItemSet closure(LRItemSet itemSet) {
        LRItemSet closure = new LRItemSet(); // 结果闭包项集
        // 使用 Set 来跟踪已处理或已在队列中的项，以避免重复工作和项的重复添加
        Set<LRItem> processedOrInQueue = new HashSet<>(itemSet.items);
        Queue<LRItem> queue = new LinkedList<>(itemSet.items); // 工作队列，初始化为输入项集

        // 首先将初始项集中的所有项加入到闭包结果中
        for (LRItem initialItem : itemSet.items) {
            closure.addItem(initialItem);
        }

        while (!queue.isEmpty()) {
            LRItem item = queue.poll(); // 取出一个项进行处理

            SymbolType B = item.nextSymbol(); // 获取点号 ('.') 后面的文法符号

            // 如果点号在产生式末尾，或者点号后面是终结符，则该项不会产生新的闭包项
            if (B == null || B.isTerminal()) {
                continue;
            }

            // B 是一个非终结符。我们需要计算 FIRST(βa)
            // β 是 B 后面的符号序列 (item.production.right 中从 B 之后到末尾的部分)
            List<SymbolType> beta;
            if (item.dotPos + 1 < item.production.right.size()) {
                beta = item.production.right.subList(
                        item.dotPos + 1, // B 的下一个符号的索引
                        item.production.right.size() // 产生式右部的末尾
                );
            } else {
                beta = Collections.emptyList(); // 如果 B 后面没有符号，则 β 是空序列
            }

            // 计算 FIRST(βa)，其中 a 是原项的展望符 (item.lookahead)
            Set<SymbolType> firstBetaA = computeFirst(beta, item.lookahead);

            // 对于 B 的每一个产生式 B → γ
            for (Production p : grammar.getProductionsByLeft(B)) {
                // 对于 FIRST(βa) 中的每一个终结符 b (作为新项的展望符)
                for (SymbolType bLookahead : firstBetaA) {
                    LRItem newItem = new LRItem(p, 0, bLookahead); // 创建新项 [B → .γ, b]
                    // 只有当 newItem 不在 processedOrInQueue 集合中时，才将其添加到闭包和队列中
                    // processedOrInQueue.add() 如果元素成功添加（即之前不存在），则返回 true
                    if (processedOrInQueue.add(newItem)) {
                        closure.addItem(newItem); // 添加到结果闭包项集
                        queue.add(newItem);       // 添加到工作队列以进一步处理
                    }
                }
            }
        }
        return closure;
    }

    /**
     * 计算从项集 I 接收文法符号 X 后的 GOTO 目标项集。
     * GOTO(I, X) 是对 I 中所有形如 [A → α . X β, a] 的项，
     * 得到项集 J = { [A → α X . β, a] }，然后计算 J 的闭包。
     *
     * @param I 源 LR(1) 项集。
     * @param X 转移的文法符号（终结符或非终结符）。
     * @return GOTO(I, X) 目标 LR(1) 项集（已计算闭包）。
     */
    private LRItemSet goTo(LRItemSet I, SymbolType X) {
        LRItemSet J = new LRItemSet(); // 核心项集
        for (LRItem item : I.items) {
            // 检查点号后是否是符号 X
            if (item.nextSymbol() != null && item.nextSymbol().equals(X)) {
                // 如果是，将点号右移一位，创建新项，并保持原展望符
                J.addItem(new LRItem(
                        item.production,
                        item.dotPos + 1,
                        item.lookahead
                ));
            }
        }
        return closure(J); // 返回核心项集的闭包
    }

    /**
     * 构建 LR(1) 项集族（规范 LR(1) 项集集合）。
     * 这是构建 LR(1) 分析器的核心步骤，它生成了分析器的所有状态。
     * 1. 从增广文法的起始产生式 S' → S (这里是 NT_AUGMENTED_START → NT_PROGRAM) 创建初始项集 I0。
     * 初始项为 [S' → .S, EOF]。然后计算 I0 的闭包。
     * 2. 使用一个工作列表，迭代处理所有已生成的项集。
     * 3. 对于每个项集 I 和每个文法符号 X，计算 GOTO(I, X)。
     * 4. 如果 GOTO(I, X) 非空且尚未存在于项集族中，则将其添加到项集族中和工作列表。
     * 5. 同时记录从状态 I 在符号 X 上的转移到 GOTO(I, X) 对应的状态。
     */
    public void buildItemSets() {
        // 初始化：创建初始项集 I0
        LRItemSet initSet = new LRItemSet();
        // 假设 grammar.productions.get(0) 是增广产生式 S' → program
        // 初始项为 [S' → .program, EOF]
        initSet.addItem(new LRItem(
                grammar.productions.get(0),  // 获取增广文法的起始产生式
                0,                           // 点号在最开始
                SymbolType.T_EOF             // 初始展望符为文件结束符
        ));
        initSet = closure(initSet); // 计算初始项集的闭包
        initSet.id = 0;             // 设置 I0 的 ID
        itemSets.add(initSet);      // 将 I0 添加到项集族中

        // 迭代构建项集族，直到没有新的项集产生
        // 使用一个索引 i 来遍历 itemSets 列表，因为列表在循环过程中可能会增长
        for (int i = 0; i < itemSets.size(); i++) {
            LRItemSet currentSet = itemSets.get(i); // 当前处理的项集

            // 对所有文法符号（终结符和非终结符）计算 GOTO 转换
            for (SymbolType symbol : getAllSymbols()) {
                LRItemSet nextSetCore = goTo(currentSet, symbol); // 计算 GOTO(currentSet, symbol)

                // 如果 GOTO 结果非空（即存在有效的转移）
                if (!nextSetCore.items.isEmpty()) {
                    LRItemSet finalNextSet = nextSetCore; // GOTO 结果
                    // 检查这个新生成的项集是否已经存在于项集族中
                    // LRItemSet 的 equals 方法仅比较核心项
                    Optional<LRItemSet> existing = itemSets.stream()
                            .filter(s -> s.equals(finalNextSet)) // 使用 equals 比较项集内容
                            .findFirst();

                    LRItemSet targetSet;
                    if (existing.isPresent()) {
                        targetSet = existing.get();  // 如果已存在，则复用现有的项集
                    } else {
                        // 如果是新的项集
                        finalNextSet.id = itemSets.size(); // 分配新的 ID
                        itemSets.add(finalNextSet);        // 将新项集添加到项集族中
                        targetSet = finalNextSet;
                    }
                    // 在当前项集 currentSet 中记录到目标项集 targetSet 的转移
                    currentSet.transitions.put(symbol, targetSet.id);
                }
            }
        }
    }

    /**
     * 构建 LR(1) 分析表 (Parse Table)。
     * 分析表包含 ACTION 表和 GOTO 表。
     * - ACTION[state, terminal] 指示在状态 state 遇到终结符 terminal 时应执行的操作
     * (SHIFT, REDUCE, ACCEPT, ERROR)。
     * - GOTO[state, nonTerminal] 指示在状态 state 归约出非终结符 nonTerminal 后应转移到的状态。
     *
     * @return 构建完成的 {@link ParseTable}。
     */
    public ParseTable buildParseTable() {
        ParseTable table = new ParseTable(grammar.productions); // 初始化分析表

        // 遍历所有已生成的 LR(1) 项集（状态）
        for (LRItemSet itemSet : itemSets) {
            int currentStateId = itemSet.id; // 当前状态的 ID

            // 填充 ACTION 表的 SHIFT 操作 和 GOTO 表
            // 基于项集的 GOTO 转换
            for (Map.Entry<SymbolType, Integer> entry : itemSet.transitions.entrySet()) {
                SymbolType symbol = entry.getKey();    // 转移符号
                int nextStateId = entry.getValue(); // 目标状态的 ID

                if (symbol.isTerminal()) {
                    // 如果转移符号是终结符，则为 SHIFT 操作
                    table.addAction(currentStateId, symbol, Action.shift(nextStateId));
                } else {
                    // 如果转移符号是非终结符，则为 GOTO 操作
                    table.addGoto(currentStateId, symbol, nextStateId);
                }
            }

            // 填充 ACTION 表的 REDUCE 操作 和 ACCEPT 操作
            // 基于项集中的完成项 (dot at the end)
            for (LRItem item : itemSet.items) {
                if (item.isComplete()) { // 检查项是否为完成项，例如 [A → α ., a]
                    // 特殊情况：处理增广文法的接受项 S' → S .
                    // 假设 grammar.productions.get(0) 是增广产生式 S' → program
                    if (item.production.equals(grammar.productions.get(0))) {
                        // 如果是 [S' → program ., EOF]，则为 ACCEPT 操作
                        // 仅当展望符是 EOF 时接受
                        if (item.lookahead == SymbolType.T_EOF) {
                            table.addAction(currentStateId, SymbolType.T_EOF, Action.accept());
                        }
                    } else {
                        // 一般的归约项：[A → α ., a]
                        // 使用项的展望符 a 来确定归约操作
                        table.addAction(currentStateId, item.lookahead, Action.reduce(item.production.id));
                    }
                }
            }
        }
        return table;
    }
    /**
     * 计算符号序列 β 和展望符 a 组合的 FIRST(βa) 集合。
     * FIRST(βa) = FIRST(β) if ε ∉ FIRST(β)
     * FIRST(βa) = (FIRST(β) - {ε}) ∪ {a} if ε ∈ FIRST(β)
     * 如果 β 为空，则 FIRST(βa) = {a} (如果 a 不是 null)，或 {ε} (如果 a 是 null，通常表示原始展望符)。
     * 此方法使用 {@link #precomputedFirstSets} 来获取单个非终结符的 FIRST 集。
     *
     * @param symbols   符号序列 β (一个 {@code List<SymbolType>})。
     * @param lookahead 展望符 a (一个 {@code SymbolType})。
     * @return FIRST(βa) 符号集合 (只包含终结符或 ε)。
     */
    private Set<SymbolType> computeFirst(List<SymbolType> symbols, SymbolType lookahead) {
        // 使用 Pair 作为缓存的键
        Pair<List<SymbolType>, SymbolType> key = new Pair<>(symbols, lookahead);
        if (firstCache.containsKey(key)) {
            return firstCache.get(key); // 如果已缓存，直接返回结果
        }

        Set<SymbolType> first = new HashSet<>(); // 初始化结果集合

        // 如果符号序列 β 为空
        if (symbols.isEmpty()) {
            // FIRST(εa) = {a} (或 {ε} 如果 lookahead 为空，代表原始上下文的ε)
            first.add(lookahead != null ? lookahead : SymbolType.T_EPSILON);
            firstCache.put(key, first);
            return first;
        }

        boolean allPreviousDerivedEpsilon = true; // 标记 β 中当前符号之前的所有符号是否都能推导出 ε
        for (SymbolType currentSymbol : symbols) { // 遍历 β 中的每个符号 Y_i
            Set<SymbolType> firstOfCurrentSymbol = new HashSet<>(); // 当前符号 Y_i 的 FIRST 集

            if (currentSymbol.isTerminal()) {
                // 如果 Y_i 是终结符 t，则 FIRST(Y_i) = {t}
                firstOfCurrentSymbol.add(currentSymbol);
            } else { // currentSymbol 是非终结符 N
                // 使用预先计算好的 FIRST(N) 集合
                if (this.precomputedFirstSets.containsKey(currentSymbol)) {
                    firstOfCurrentSymbol.addAll(this.precomputedFirstSets.get(currentSymbol));
                } else {
                    // 理论上不应发生，因为所有非终结符的 FIRST 集都应该已预计算
                    System.err.println("错误: 非终结符 " + currentSymbol + " 的 FIRST 集未被预计算!");
                    // 可以选择抛出异常或返回空集，但这指示预计算步骤中存在问题
                }
            }

            boolean currentSymbolDerivesEpsilon = false; // 标记当前符号 Y_i 是否能推导出 ε
            // 将 FIRST(Y_i) 中非 ε 的符号加入到结果 first 集合中
            for (SymbolType s_prime : firstOfCurrentSymbol) {
                if (!s_prime.equals(SymbolType.T_EPSILON)) {
                    first.add(s_prime);
                } else {
                    currentSymbolDerivesEpsilon = true; // Y_i 可以推导出 ε
                }
            }

            // 如果当前符号 Y_i 不能推导出 ε，则 FIRST(βa) 不受 β 中 Y_i 之后符号的影响
            if (!currentSymbolDerivesEpsilon) {
                allPreviousDerivedEpsilon = false; // β 整体不能仅推导出 ε (到目前为止)
                break; // 停止处理序列中的后续符号
            }
            // 如果 Y_i 能推导出 ε，则继续处理 β 中的下一个符号 Y_{i+1}
        }

        // 如果 β 中的所有符号 (Y1 Y2 ... Yk) 都能推导出 ε
        if (allPreviousDerivedEpsilon) {
            // 则将展望符 a (或 ε) 加入到 FIRST(βa) 集合中
            first.add(lookahead != null ? lookahead : SymbolType.T_EPSILON);
        }

        firstCache.put(key, first); // 缓存计算结果
        return first;
    }




    /**
     * 预计算文法中所有非终结符的 FIRST 集。
     * FIRST(N) 是从非终结符 N 可能推导出的所有产生式开头终结符的集合，如果 N 能推导出 ε，则 ε 也包含在内。
     * 该方法使用迭代算法，重复计算直到 FIRST 集不再发生变化。
     *
     * @return 一个 Map，键是非终结符，值是其对应的 FIRST 符号集合。
     */
    public Map<SymbolType, Set<SymbolType>> precomputeAllFirstSets() {
        Map<SymbolType, Set<SymbolType>> firstSets = new HashMap<>();
        // 初始化所有非终结符的 FIRST 集为空集
        for (SymbolType nonTerminal : grammar.nonTerminals) {
            firstSets.put(nonTerminal, new HashSet<>());
        }

        boolean changed; // 标记在一轮迭代中是否有 FIRST 集发生变化
        do {
            changed = false;
            // 对每个非终结符 N 进行处理
            for (SymbolType nonTerminal : grammar.nonTerminals) {
                Set<SymbolType> currentNtFirstSet = firstSets.get(nonTerminal); // 获取 N 当前的 FIRST 集
                int oldSize = currentNtFirstSet.size(); // 记录处理前的大小，用于检测变化

                // 遍历 N 的所有产生式 N → Y1 Y2 ... Yk
                for (Production p : grammar.getProductionsByLeft(nonTerminal)) {
                    List<SymbolType> rhs = p.right; // 产生式右部

                    // 情况 1: N → ε (产生式右部为空或直接为 ε)
                    if (rhs.isEmpty() || (rhs.size() == 1 && rhs.get(0).equals(SymbolType.T_EPSILON))) {
                        currentNtFirstSet.add(SymbolType.T_EPSILON);
                    } else {
                        // 情况 2: N → Y1 Y2 ... Yk
                        boolean allPreviousDerivedEpsilon = true; // 标记 Y1...Y_{i-1} 是否都能推导出 ε
                        for (SymbolType symbolInRhs : rhs) { // 遍历 Y1, Y2, ...
                            if (symbolInRhs.isTerminal()) {
                                // 如果 Y_i 是终结符 t，则将 t 加入 FIRST(N)
                                currentNtFirstSet.add(symbolInRhs);
                                allPreviousDerivedEpsilon = false; // Y_i 不能是 ε (因为它是终结符)
                                break; // 此产生式的 FIRST 集贡献已确定，处理下一个产生式
                            } else { // symbolInRhs 是非终结符 Y_i
                                Set<SymbolType> firstOfSymbolInRhs = firstSets.get(symbolInRhs); // 获取 FIRST(Y_i)
                                // 将 FIRST(Y_i) 中除了 ε 之外的所有符号加入 FIRST(N)
                                for (SymbolType s : firstOfSymbolInRhs) {
                                    if (!s.equals(SymbolType.T_EPSILON)) {
                                        currentNtFirstSet.add(s);
                                    }
                                }
                                // 如果 FIRST(Y_i) 不包含 ε，则 Y_i 不能推导出 ε
                                if (!firstOfSymbolInRhs.contains(SymbolType.T_EPSILON)) {
                                    allPreviousDerivedEpsilon = false;
                                    break; // 此产生式的 FIRST 集贡献已确定
                                }
                                // 否则 (FIRST(Y_i) 包含 ε)，继续处理下一个 Y_{i+1}
                            }
                        }
                        // 如果产生式右部的所有符号 Y1...Yk 都能推导出 ε
                        if (allPreviousDerivedEpsilon) {
                            currentNtFirstSet.add(SymbolType.T_EPSILON); // 则将 ε 加入 FIRST(N)
                        }
                    }
                }
                // 如果 N 的 FIRST 集大小发生变化，则标记 changed 为 true
                if (currentNtFirstSet.size() > oldSize) {
                    changed = true;
                }
            }
        } while (changed); // 重复迭代直到没有 FIRST 集发生变化

        return firstSets;
    }


    /**
     * 获取文法中所有符号的集合（包括终结符和非终结符）。
     *
     * @return 包含所有文法符号的 Set。
     */
    private Set<SymbolType> getAllSymbols() {
        Set<SymbolType> symbols = new HashSet<>();
        symbols.addAll(grammar.terminals);
        symbols.addAll(grammar.nonTerminals);
        // 注意：T_EOF 和 T_EPSILON 通常需要特殊处理
        // 或者在特定逻辑中单独处理，如 buildItemSets 和 buildParseTable。
        // getAllSymbols 主要用于 GOTO 计算时遍历所有可能的转移符号。
        // 从 grammar.terminals 中移除 T_EPSILON 如果它不应该作为 GOTO 的符号
        symbols.remove(SymbolType.T_EPSILON); // ε 通常不作为 GOTO 的转移符号
        return symbols;
    }
}
