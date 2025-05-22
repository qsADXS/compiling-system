package src.compile.parser.algorithm;

/**
 * @author sixteacher
 * @version 1.0
 * @description LRItem
 * @date 2025/5/19
 */

/**
 * 表示 LR(1) 分析过程中的一个项 (Item)。
 * LR(1) 项由三部分组成：一个文法产生式、一个点在产生式右部的位置以及一个展望符。
 * 例如，对于产生式 A -> X Y，一个可能的 LR(1) 项是 [A -> X . Y, z]，
 * 其中点表示我们已经匹配了 X，期望接下来匹配 Y，而 z 是展望符。
 * 这些项用于构建 LR(1) 分析表的状态机中的状态。
 */
public class LRItem {
    /**
     * 项关联的文法产生式。
     */
    public final Production production;

    /**
     * 点在产生式右部的位置。
     * 范围从 0 (点在最左边) 到 production.right.size() (点在最右边)。
     */
    public final int dotPos;

    /**
     * 展望符 (lookahead symbol)，它是一个终结符。
     * 对于 LR(1) 项，展望符用于决定在归约时是否应用此产生式。
     * 对于 LR(0) 项，此字段可能未使用或设置为特殊值。
     */
    public final SymbolType lookahead;

    public LRItem(Production production, int i, SymbolType symbolType) {
        this.production = production;
        this.dotPos = i;
        this.lookahead = symbolType;
    }


    /**
     * 获取点右边的符号。
     * 如果点在产生式的末尾，则返回 null。
     *
     * @return 点右边的符号，如果点在末尾则返回 null。
     */
    public SymbolType nextSymbol() {
        if (dotPos >= production.right.size()) return null;
        return production.right.get(dotPos);
    }


    @Override
    public String toString() {
        // 可选：提供一个有意义的字符串表示，方便调试
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(production.left).append(" -> ");
        for (int i = 0; i < production.right.size(); i++) {
            if (i == dotPos) {
                sb.append(".");
            }
            sb.append(production.right.get(i)).append(" ");
        }
        if (dotPos == production.right.size()) {
            sb.append(".");
        }
        sb.append(", ").append(lookahead).append("]");
        return sb.toString();
    }


    /**
     * 检查此项是否为一个完成项 (complete item)，即点是否在产生式右部的最末端。
     * 完成项表示一个潜在的归约操作。
     * 例如：[A -> α ., z]
     *
     * @return 如果点在产生式右部的末尾，则返回 true；否则返回 false。
     */
    public boolean isComplete() {
        // 情况1：点号在产生式右部的末尾。
        // 这覆盖了:
        //   - X -> α. (此时 dotPos == production.right.size())
        //   - X -> .  (如果 production.right 为空, 此时 dotPos == 0, production.right.size() == 0)
        if (dotPos >= production.right.size()) {
            return true;
        }

        // 情况2：产生式是 X -> ε 的形式, 其中 ε 被表示为 production.right 中的单个特殊符号
        // (例如 SymbolType.T_EPSILON), 且点号在最前面: X -> .ε
        // 在这种情况下, dotPos 为 0, production.right.size() 为 1,
        // 且 production.right.get(0) 就是那个 ε 符号。
        // 这样的项也应该被视为完成项，用于规约。
        if (production.right.size() == 1 &&
                dotPos == 0 && // 点号在最前面
                production.right.get(0) == SymbolType.T_EPSILON) { // 且唯一的符号是 ε
            return true;
        }

        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LRItem lrItem = (LRItem) o;
        return dotPos == lrItem.dotPos &&
                production.equals(lrItem.production) && // Assuming Production has a proper equals method
                lookahead == lrItem.lookahead; // Assuming SymbolType is an enum or has proper equals
    }

    @Override
    public int hashCode() {
        // Assuming Production has a proper hashCode method and SymbolType is an enum or has proper hashCode
        int result = production.hashCode();
        result = 31 * result + dotPos;
        result = 31 * result + lookahead.hashCode();
        return result;
    }
}
