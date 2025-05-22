package src.compile.parser.algorithm;

import com.sun.xml.internal.bind.v2.model.core.ID;

import java.util.*;

/**
 * @author sixteacher
 * @version 1.0
 * @description ParseTable
 * @date 2025/5/19
 */


/**
 * 分析表由两部分组成：ACTION 表和 GOTO 表。
 * ACTION 表指示了在给定状态和面临一个终结符时，分析器应执行的操作（移入、规约、接受或报错）。
 * GOTO 表指示了在给定状态和一个非终结符（通常是规约结果）出现时，分析器应转移到的下一个状态。
 * 此表由 {@link LRParserGenerator} 在构建 LR(0), SLR, LR(1) 或 LALR 分析器时生成。
 */
public class ParseTable {
    /**
     * ACTION 表。
     * 键是状态 ID (Integer)。
     * 值是一个 Map，其中：
     * 键是终结符 ({@link SymbolType})。
     * 值是对应的分析动作 ({@link Action})。
     * 例如, actionTable.get(state).get(terminal) 返回在 state 状态遇到 terminal 符号时应执行的动作。
     */
    private Map<Integer, Map<SymbolType, Action>> actionTable;

    /**
     * GOTO 表。
     * 键是状态 ID (Integer)。
     * 值是一个 Map，其中：
     * 键是非终结符 ({@link SymbolType})。
     * 值是目标状态的 ID (Integer)。
     * 例如, gotoTable.get(state).get(nonTerminal) 返回在 state 状态归约出 nonTerminal 后应转移到的状态。
     */
    private Map<Integer, Map<SymbolType, Integer>> gotoTable;

    /**
     * 文法产生式的列表。
     * 用于在规约 (Reduce) 操作时通过产生式 ID 查找具体的产生式。
     * {@link Action#stateOrProduction} 在规约动作中存储的就是此列表中的索引。
     */
    private List<Production> productions;

    /**
     * 构造一个新的分析表。
     *
     * @param productions 文法产生式的列表，用于规约操作。
     */
    public ParseTable(List<Production>  productions) {
        this.actionTable = new HashMap<>();
        this.gotoTable = new HashMap<>();
        this.productions = productions;
    }

    public Map<Integer, Map<SymbolType, Action>> getActionTable() {
        return actionTable;
    }

    public Map<Integer, Map<SymbolType, Integer>> getGotoTable() {
        return gotoTable;
    }

    /**
    *向 ACTION 表中添加一个条目。
    *
    * @param state  当前状态的 ID。
    * @param symbol 遇到的终结符 (或 EOF)。
    * @param action 要执行的动作 (移入, 规约, 接受)。
    * @throws IllegalArgumentException 如果尝试为非终结符 (非 EOF) 添加动作。
    */
    public void addAction(int state, SymbolType symbol, Action action) {
        // ACTION 表的键必须是终结符或文件结束符 (T_EOF)
        if (!symbol.isTerminal()) {
            // 允许为 T_EOF 添加动作，它通常被视为一个特殊的终结符
            if (symbol != SymbolType.T_EOF) {
                throw new IllegalArgumentException("Action表只能使用终结符或EOF, 尝试添加: " + symbol + " for state " + state);
            }
        }

        // 获取或创建当前状态的动作映射
        Map<SymbolType, Action> stateActions = actionTable.computeIfAbsent(state, k -> new HashMap<>());
        Action existingAction = stateActions.get(symbol); // 获取已存在的动作 (如果有)

        if (existingAction != null) {
            // 检测到冲突：在同一个状态下，对于同一个输入符号，已经存在一个动作
            // 移入/规约冲突 (Shift/Reduce Conflict)
            if (existingAction.type == Action.Type.SHIFT && action.type == Action.Type.REDUCE) {
                // 已存在的是移入，新的是规约
                System.err.println("警告: 在状态 " + state + " 遇到符号 " + symbol + " 时发生移入/规约冲突。" +
                        " 已有动作: " + existingAction + ", 新动作: " + action + "。默认保留移入 (SHIFT) 动作。");
                // 默认解决：优先移入。如果新动作是规约，则保留已有的移入动作。
                // (当前代码逻辑: 如果 existingAction 是 SHIFT，新 action 是 REDUCE，则不更新，保留 existingAction)
                // 如果需要根据优先级或其他规则来决定，例如根据产生式的优先级。
            } else if (existingAction.type == Action.Type.REDUCE && action.type == Action.Type.SHIFT) {
                // 已存在的是规约，新的是移入
                System.err.println("警告: 在状态 " + state + " 遇到符号 " + symbol + " 时发生规约/移入冲突。" +
                        " 已有动作: " + existingAction + ", 新动作: " + action + "。默认选择移入 (SHIFT) 动作。");
                // 默认解决：优先移入。新动作是 SHIFT，所以更新为新动作。
                stateActions.put(symbol, action);
            } else if (existingAction.type == Action.Type.REDUCE && action.type == Action.Type.REDUCE) {
                // 规约/规约冲突 (Reduce/Reduce Conflict)
                // 这通常是文法二义性的表现，需要更仔细地处理。
                System.err.println("错误: 在状态 " + state + " 遇到符号 " + symbol + " 时发生规约/规约冲突。" +
                        " 已有动作: " + existingAction + ", 新动作: " + action + "。");
                // 示例解决方案：选择产生式ID（编号）较小的规约动作。
                // 假设 Action.stateOrProduction 在规约时存储的是产生式ID。
                if (action.stateOrProduction < existingAction.stateOrProduction) {
                    System.err.println("  解决规约/规约冲突：选择产生式ID较小的动作: " + action.stateOrProduction);
                    stateActions.put(symbol, action);
                } else {
                    System.err.println("  解决规约/规约冲突：保留产生式ID较小的已有动作: " + existingAction.stateOrProduction);
                    // 保留 existingAction (即不更新)
                }
                // 或者，更严格的做法是抛出异常，强制用户修改文法来消除二义性。
                // throw new IllegalStateException("Reduce/Reduce conflict at state " + state + " on symbol " + symbol +
                //                                 ". Existing: " + existingAction + ", New: " + action);
            } else if (!existingAction.equals(action)) {
                // 其他类型的冲突，或者同一类型的动作但目标不同 (例如，SHIFT 到不同的状态)。
                // 对于确定性 LR 分析器 (如 SLR, LR(1), LALR)，这通常不应该发生，
                // 如果项集构造和分析表生成算法正确，对于一个状态和一个符号，动作应该是唯一的。
                // (除非是 LALR 分析器中可能出现的与 LR(1) 不同的规约/规约冲突，但这通常在项集合并时处理)
                System.err.println("错误: 在状态 " + state + " 遇到符号 " + symbol + " 时发生动作冲突。" +
                        " 已有动作: " + existingAction + ", 新动作: " + action + "。这可能表示文法或分析器生成逻辑存在问题。");
                // 根据具体策略决定是否覆盖或抛出异常。
                // stateActions.put(symbol, action); // 例如，总是用新的动作覆盖旧的
            }
            // 如果 existingAction.equals(action)，说明是重复添加相同的动作，无需操作。
        } else {
            // 没有冲突，直接添加新的动作。
            stateActions.put(symbol, action);
        }
    }

    /**
     * 向 GOTO 表中添加一个条目。
     *
     * @param state       当前状态的 ID。
     * @param symbol      遇到的非终结符。
     * @param targetState 成功归约并遇到该非终结符后要转移到的目标状态 ID。
     * @throws IllegalArgumentException 如果尝试为终结符添加 GOTO。
     * @throws IllegalStateException    如果发生 GOTO 冲突 (理论上不应发生)。
     */
    public void addGoto(int state, SymbolType symbol, int targetState) {
        // GOTO 表的键必须是非终结符
        if (symbol.isTerminal()) {
            // T_EOF 通常不作为 GOTO 表的键
            throw new IllegalArgumentException("Goto表只能使用非终结符, 尝试添加: " + symbol + " for state " + state);
        }
        // 获取或创建当前状态的 GOTO 映射
        Map<SymbolType, Integer> stateGotos = gotoTable.computeIfAbsent(state, k -> new HashMap<>());
        Integer existingTarget = stateGotos.get(symbol); // 获取已存在的 GOTO 目标

        // GOTO 表理论上不应该有冲突。对于一个给定的状态和一个非终结符，GOTO 目标是确定的。
        // 如果 existingTarget 不为 null 且不等于 targetState，说明存在冲突。
        if (existingTarget != null && existingTarget != targetState) {
            throw new IllegalStateException("GOTO 冲突: 在状态 " + state + " 遇到非终结符 " + symbol +
                    " 时发生冲突。已有目标状态: " + existingTarget + ", 新目标状态: " + targetState +
                    "。这很可能指示 LR 项集构造或 GOTO 函数实现中存在问题。");
        }
        stateGotos.put(symbol, targetState);
    }


    /**
     * 从 ACTION 表中获取指定状态和符号对应的动作。
     *
     * @param state  当前状态的 ID。
     * @param symbol 遇到的终结符。
     * @return 对应的 {@link Action}，如果不存在则返回 null (表示错误或分析结束)。
     */
    public Action getAction(int state, SymbolType symbol) {
        Map<SymbolType, Action> stateActions = actionTable.get(state);
        if (stateActions != null) {
            return stateActions.get(symbol); // 返回动作，如果符号没有对应动作则返回null
        }
        return null; // 该状态不存在或没有定义任何动作
    }
    /**
     * 根据产生式的索引（ID）获取产生式对象。
     *
     * @param index 产生式的索引 (通常是其在文法产生式列表中的位置)。
     * @return 对应的 {@link Production} 对象，如果索引无效则返回 null。
     */
    public Production getProduction(int index) {
        if (index >= 0 && index < productions.size()) {
            return productions.get(index);
        }
        // 索引越界，无法找到产生式
        System.err.println("错误: 尝试获取无效的产生式索引: " + index);
        return null;
    }

    /**
     * 从 GOTO 表中获取指定状态和非终结符对应的目标状态。
     *
     * @param state      当前状态的 ID。
     * @param symbolType 遇到的非终结符。
     * @return 目标状态的 ID，如果不存在转移则返回 -1 (表示错误或分析无法继续)。
     */
    public int getGoto(int state, SymbolType symbolType) { // 参数名 SymbolType 建议小写开头 symbolType
        Map<SymbolType, Integer> stateGotos = gotoTable.get(state);
        if (stateGotos != null) {
            Integer targetState = stateGotos.get(symbolType); // 获取目标状态
            return targetState != null ? targetState : -1; // 如果找到则返回，否则返回-1
        }
        return -1; // 该状态不存在或没有为该非终结符定义 GOTO 转移
    }
    /**
     * 打印整个分析表 (ACTION 表和 GOTO 表) 到控制台。
     * 主要用于调试。
     */
    public void printParseTable() {
        if (actionTable == null || gotoTable == null) {
            System.out.println("分析表尚未初始化!");
            return;
        }
        System.out.println("========== ACTION 表 ==========");
        System.out.printf("%-6s", "状态"); // State 列头

        // 收集所有在 ACTION 表中出现过的终结符作为列标题
        Set<SymbolType> terminals = new TreeSet<>(Comparator.comparing(Enum::name)); // 使用 TreeSet 保证顺序
        for (Map<SymbolType, Action> actions : actionTable.values()) {
            terminals.addAll(actions.keySet());
        }

        // 打印 ACTION 表的列标题 (终结符)
        for (SymbolType terminal : terminals) {
            System.out.printf("%-8s", terminal.name()); // 假设 SymbolType 是枚举，使用 name()
        }
        System.out.println();

        // 打印每个状态的 ACTION 表条目
        // 为了保证状态顺序打印，可以先收集所有状态ID并排序
        List<Integer> sortedActionStates = new ArrayList<>(actionTable.keySet());
        Collections.sort(sortedActionStates);

        for (int state : sortedActionStates) {
            System.out.printf("%-6d", state); // 打印状态ID
            Map<SymbolType, Action> actions = actionTable.get(state);
            for (SymbolType terminal : terminals) { // 按照收集到的终结符顺序打印
                Action action = actions.get(terminal);
                System.out.printf("%-8s", action != null ? action.toString() : " "); // 打印动作或空格
            }
            System.out.println();
        }

        System.out.println("\n========== GOTO 表 ==========");
        System.out.printf("%-6s", "状态"); // State 列头

        // 收集所有在 GOTO 表中出现过的非终结符作为列标题
        Set<SymbolType> nonTerminals = new TreeSet<>(Comparator.comparing(Enum::name)); // 使用 TreeSet 保证顺序
        for (Map<SymbolType, Integer> gotos : gotoTable.values()) {
            nonTerminals.addAll(gotos.keySet());
        }

        // 打印 GOTO 表的列标题 (非终结符)
        for (SymbolType nonTerminal : nonTerminals) {
            System.out.printf("%-8s", nonTerminal.name()); // 假设 SymbolType 是枚举，使用 name()
        }
        System.out.println();

        // 打印每个状态的 GOTO 表条目
        // 为了保证状态顺序打印，可以先收集所有状态ID并排序
        List<Integer> sortedGotoStates = new ArrayList<>(gotoTable.keySet());
        Collections.sort(sortedGotoStates);

        for (int state : sortedGotoStates) {
            System.out.printf("%-6d", state); // 打印状态ID
            Map<SymbolType, Integer> gotos = gotoTable.get(state);
            for (SymbolType nonTerminal : nonTerminals) { // 按照收集到的非终结符顺序打印
                Integer target = gotos.get(nonTerminal);
                System.out.printf("%-8s", target != null ? target.toString() : " "); // 打印目标状态或空格
            }
            System.out.println();
        }
    }


    /**
     * 表示分析器在 ACTION 表中的一个动作。
     * 动作可以是移入 (SHIFT)、规约 (REDUCE) 或接受 (ACCEPT)。
     */
    public static class Action {
        /**
         * 定义分析动作的类型。
         */
        public enum Type {
            /**
             * 移入 (Shift): 将当前输入符号和目标状态压入栈。
             */
            SHIFT,
            /**
             * 规约 (Reduce): 使用一个文法产生式进行规约。
             */
            REDUCE,
            /**
             * 接受 (Accept): 输入串成功解析。
             */
            ACCEPT
        }

        /**
         * 动作的类型 (移入, 规约, 接受)。
         */
        public final Type type;
        /**
         * 动作的目标值。
         * - 对于 SHIFT 动作: 表示要转移到的目标状态的 ID。
         * - 对于 REDUCE 动作: 表示要用于规约的产生式的 ID (在 {@link ParseTable#productions} 列表中的索引)。
         * - 对于 ACCEPT 动作: 此值通常未使用 (可以设为 -1 或其他标记值)。
         */
        public final int stateOrProduction;

        /**
         * 私有构造函数，用于创建 Action 对象。
         * 使用静态工厂方法 {@link #shift(int)}, {@link #reduce(int)}, {@link #accept()} 来创建实例。
         *
         * @param type              动作类型。
         * @param value             动作的目标值 (状态 ID 或产生式 ID)。
         */
        private Action(Type type, int value) {
            this.type = type;
            this.stateOrProduction = value;
        }

        /**
         * 创建一个移入 (SHIFT) 动作。
         *
         * @param state 要转移到的目标状态 ID。
         * @return 代表移入操作的 Action 对象。
         */
        public static Action shift(int state) {
            return new Action(Type.SHIFT, state);
        }

        /**
         * 创建一个规约 (REDUCE) 动作。
         *
         * @param productionId 用于规约的产生式的 ID。
         * @return 代表规约操作的 Action 对象。
         */
        public static Action reduce(int productionId) { // 参数名 productionId 更清晰
            return new Action(Type.REDUCE, productionId);
        }

        /**
         * 创建一个接受 (ACCEPT) 动作。
         *
         * @return 代表接受操作的 Action 对象。
         */
        public static Action accept() {
            // 接受动作通常不需要特定的 stateOrProduction 值，-1 是一个常见的标记
            return new Action(Type.ACCEPT, -1);
        }

        @Override
        public String toString() {
            switch (type) {
                case SHIFT: return "s" + stateOrProduction;
                case REDUCE: return "r" + stateOrProduction;
                case ACCEPT: return "acc";
                default: return "?";
            }
        }
    }
}
