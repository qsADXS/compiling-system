package src.compile.parser.tac.Address;

/**
 * @author sixteacher
 * @version 1.0
 * @description LabelAddress
 * @date 2025/5/21
 */

/**
 * 表示由标签字符串标识的地址。
 * （可选）可以为标签关联一个描述。
 * 此类实现了 Address 接口。
 */
public class LabelAddress implements Address {
    private final String label;
    private final String description; // 新增：标签的描述信息

    // 原构造函数，用于无描述的情况
    public LabelAddress(String label) {
        this(label, null); // 调用下面的构造函数，描述传入null
    }

    // 新增/修改构造函数：接受标签名和描述
    public LabelAddress(String label, String description) {
        if (label == null || label.trim().isEmpty()) {
            throw new IllegalArgumentException("Label name cannot be null or empty.");
        }
        this.label = label;
        this.description = description; // 存储描述信息
    }

    public String getLabel() { // 此方法仍返回纯标签名
        return label;
    }

    // 新增：获取描述的方法
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        // toString 保持只返回标签名，因为 LabelAddress 也可能作为操作数使用
        // 例如，在 (goto, L1, _, _) 中，L1 就是这里的 toString() 结果
        return label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LabelAddress that = (LabelAddress) o;
        // 注意：equals 和 hashCode 通常只基于核心标识（即label名）
        // 描述信息不影响标签的唯一性
        return label.equals(that.label);
    }

    @Override
    public int hashCode() {
        return label.hashCode(); // 同上，只基于label名
    }
}