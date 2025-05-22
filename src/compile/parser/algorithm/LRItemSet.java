package src.compile.parser.algorithm;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author sixteacher
 * @version 1.0
 * @description LRItemSet
 * @date 2025/5/19
 */

/**
 * 表示 LR 分析中的一个项集 (Item Set)，也常被称为 LR(1) 状态 (State)。
 * 在 LR 分析中，解析器通过一系列状态的转换来识别输入符号串。每个状态由一组 LR(1) 项定义，
 * 这些项指明了在当前解析点，哪些产生式可能被匹配，以及我们已经看到了产生式右部的多少部分。
 * 项集是构建 LR 分析表（如 SLR, LALR, CLR 分析表）的基础。
 */
public class LRItemSet {

    /**
     * 项集的唯一标识符 (ID)。
     * 这个 ID 通常在构建所有项集（即LR(0)或LR(1)项集的规范族 Canonical Collection）的过程中被分配。
     * 它用于在分析表中引用特定的状态，例如在 GOTO 表和 ACTION 表中。
     * 初始化为 -1 表示该项集尚未被正式添加到规范族中或其ID尚未被计算和设置。
     */
    public int id = -1;  // 初始化为无效值，由buildItemSets()分配

    /**
     * 构成该项集（状态）核心的 LR(1) 项的集合。
     * 一个项集中的所有 LR(1) 项共同描述了在解析过程中的一个特定配置。
     * 例如，如果项集包含 [A -> α . β, a]，意味着解析器当前期望看到能从 β 派生出的符号，
     * 并且如果成功匹配了 β，且下一个输入符号是 'a'，则可能进行 A -> α β 的归约。
     * 使用 Set 可以确保项集中不包含重复的 LRItem。
     */
    public Set<LRItem> items;

    /**
     * 状态转移表 (GOTO 表的一部分)。
     * 这个 Map 存储了从当前项集（状态）出发，在遇到某个文法符号 (SymbolType) 时，
     * 应该转移到的下一个项集（状态）的 ID。
     *键 (SymbolType) 是一个文法符号（可以是终结符或非终结符）。
     *值 (Integer) 是目标项集的 ID。
     * 例如，如果 transitions.get(SymbolType.T_ID) 返回 5，表示当前状态在遇到 T_ID 时，
     * 解析器将转移到 ID 为 5 的状态。
     */
    public Map<SymbolType, Integer> transitions; // 使用枚举作为键

    /**
     * 构造一个新的、空的 LRItemSet。
     * 在构建项集规范族时，通常首先创建一个空的项集，然后通过闭包 (closure) 和
     * 转移 (goto) 操作来填充和生成新的项集。
     * 这里的 transitions 也必须初始化，否则后续添加转移时会抛出 NullPointerException。
     */
    public LRItemSet() {
        this.items = new HashSet<>();
        this.transitions = new HashMap<>(); // 必须初始化！
    }

    /**
     * 向该项集中添加一个 LR(1) 项。
     * 此方法用于构建项集，例如在计算闭包 (closure) 或通过 GOTO 函数生成新项集时。
     * @param item 要添加的 LR(1) 项。
     */
    public void addItem(LRItem item) { items.add(item); }

    /**
     * 检查该项集中是否包含指定的 LR(1) 项。
     * @param item 要检查的 LR(1) 项。
     * @return 如果项集中包含该项，则返回 true；否则返回 false。
     */
    public boolean contains(LRItem item) { return items.contains(item); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LRItemSet lrItemSet = (LRItemSet) o;
        // 仅比较items，忽略id和transitions
        return Objects.equals(items, lrItemSet.items);
    }
    @Override
    public int hashCode() {
        // 仅基于items计算哈希
        return Objects.hash(items);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LRItemSet ID: ").append(id).append(" {\n");
        for (LRItem item : items) {
            sb.append("  ").append(item.toString()).append("\n");
        }
        sb.append("}\n");
        if (!transitions.isEmpty()) {
            sb.append("Transitions: {\n");
            String transitionsStr = transitions.entrySet().stream()
                    .map(entry -> "  " + entry.getKey() + " -> " + entry.getValue())
                    .collect(Collectors.joining("\n"));
            sb.append(transitionsStr).append("\n");
            sb.append("}\n");
        }
        return sb.toString();
    }
}
