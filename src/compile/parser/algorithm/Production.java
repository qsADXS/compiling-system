package src.compile.parser.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author sixteacher
 * @version 1.0
 * @description Production
 * @date 2025/5/19
 */


/**
 * 表示文法中的一个产生式 (Production Rule)。
 * 一个产生式定义了如何从一个非终结符（左部）推导出一个符号序列（右部）。
 * 例如：E -> E + T
 * 其中 E 是左部 (left)，E + T 是右部 (right)。
 * 每个产生式通常会有一个唯一的 ID，用于在分析表或解析过程中引用它。
 */
public class Production {
    /**
     * 产生式的左部 (Left-Hand Side, LHS)。
     * 必须是一个非终结符 (Non-Terminal Symbol)。
     */
    public final SymbolType left;

    /**
     * 产生式的右部 (Right-Hand Side, RHS)。
     * 是一个文法符号 ({@link SymbolType}) 的列表，可以是终结符、非终结符或空串 (ε)。
     * 该列表是不可修改的，以保证产生式的不可变性。
     */
    public final List<SymbolType> right;

    /**
     * 产生式的唯一标识符 (ID)。
     * 这个 ID 在文法中唯一标识一个产生式，常用于分析表中的规约动作。
     */
    public final int id;

    /**
     * 构造一个新的产生式。
     *
     * @param left  产生式的左部非终结符。
     * @param right 产生式的右部符号序列。为了表示 ε-产生式 (epsilon production)，
     * 可以将 right 传递为空列表，或者包含单个 {@link SymbolType#T_EPSILON} 的列表。
     * @param id    产生式的唯一 ID。
     */
    public Production(SymbolType left, List<SymbolType> right, int id) {
        this.left = left;
        // 使用 Collections.unmodifiableList 确保右部列表在创建后不被修改，增强不可变性。
        this.right = Collections.unmodifiableList(new ArrayList<>(right)); // 防御性拷贝并设为不可修改
        this.id = id;
    }

    /**
     * 检查此产生式是否为空产生式 (ε-production)。
     * 空产生式是指右部为空，或者右部只包含一个表示空串的特殊符号 (如 SymbolType.T_EPSILON)。
     * 例如：A -> ε
     *
     * @return 如果是空产生式，则返回 true；否则返回 false。
     */
    public boolean isEpsilon() {
        // 一个 ε-产生式也可以通过右部包含一个特殊的 SymbolType.T_EPSILON 来表示
        return right.isEmpty() || (right.size() == 1 && (right.get(0) == SymbolType.T_EPSILON));
    }

    /**
     * 比较两个产生式对象是否相等。
     * 两个产生式被认为是相等的，当且仅当它们的左部和右部完全相同。
     * 产生式的 ID 通常是在左部和右部相同时保持一致的，但此处的比较主要基于产生式的定义本身。
     *
     * @param o 要比较的对象。
     * @return 如果两个产生式的左部和右部相同，则返回 true，否则返回 false。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // 如果是同一个对象引用，则相等
        if (o == null || getClass() != o.getClass()) return false; // 如果对象为null或类型不同，则不相等
        Production that = (Production) o;
        // 主要地，一个产生式由其左部和右部定义。
        // 如果左部和右部相同，ID 也应该是一致的，但比较左部和右部是更根本的。
        return Objects.equals(left, that.left) &&
                Objects.equals(right, that.right);
    }

    /**
     * 返回产生式对象的哈希码。
     * 哈希码的计算基于产生式的核心组成部分：左部 (left) 和右部 (right)。
     * 这与 {@link #equals(Object)} 方法的逻辑保持一致：如果两个产生式的 left 和 right 相同，
     * 它们的哈希码也应该相同。
     *
     * @return 该产生式基于其左部和右部的哈希码。
     */
    @Override
    public int hashCode() {
        // 基于核心组件计算哈希：左部和右部。
        return Objects.hash(left, right);
    }

    /**
     * 返回此产生式的字符串表示形式，主要用于调试和日志记录。
     * 格式通常为 "ID: Left -> RightSymbol1 RightSymbol2 ..."
     * 对于空产生式，右部通常表示为 "ε"。
     *
     * @return 产生式的字符串表示。
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(id).append(": ").append(left).append(" ->"); // "ID: Left ->"
        if (isEpsilon()) { // 使用 isEpsilon() 方法来判断是否打印 ε
            sb.append(" ε"); // 特殊处理空产生式
        } else {
            for (SymbolType s : right) {
                sb.append(" ").append(s); // " Symbol"
            }
        }
        return sb.toString();
    }
}