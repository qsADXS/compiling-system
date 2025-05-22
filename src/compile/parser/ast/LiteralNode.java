package src.compile.parser.ast;

import src.compile.parser.tac.Address.ConstantAddress;
import src.compile.parser.tac.TACGenerator;

/**
 * @author sixteacher
 * @version 1.0
 * @description LiteralNode
 * @date 2025/5/19
 */


/**
 * 代表字面量节点（例如数字 `123`、布尔值 `true`）。
 * 其 'place' 属性在 `createASTNode` 中被设置为一个 `ConstantAddress`，直接表示该常量值。
 * @param <T> 字面量的值的类型 (如 Integer, Double, Boolean)
 */
public class LiteralNode<T> extends ExprNode {
    public final T value;
    public final String typeName; // 例如 "int", "float", "boolean", "string"

    public LiteralNode(T value, String typeName, int line, int pos) {
        super(line, pos); // ExprNode 构造
        this.value = value;
        this.typeName = typeName;
    }

    @Override
    public void generateTac(TACGenerator gen) {
        // 对于字面量，其 "place" 就是它本身的值，用 ConstantAddress 来表示。
        // 它不直接 "emit" 指令，而是设置自己的 place 和 typeString 供父节点使用。

        // {{ 修改：ConstantAddress 构造函数只接受 value }}
        this.place = new ConstantAddress<>(this.value);
        // {{ 修改：ExprNode 的 typeString 通过其自身的字段或setter设置 }}
        // 假设 ExprNode 有 this.typeString 字段或 setTypeString 方法
        this.typeString = this.typeName; // 或者 this.setTypeString(this.typeName);

        // 示例:
        // 如果是 int i = 10;
        // LiteralNode(10, "int") -> generateTac() -> this.place = new ConstantAddress<Integer>(10);
        //                                          this.typeString = "int"; // (或通过 setTypeString)
        // 然后 AssignStmtNode 会用到这个 place:
        //   Address literalPlace = literalNode.getPlace(); // 获取 ConstantAddress
        //   ...
    }

    @Override
    public String getNodeTypeString() {
        return "Literal(" + typeName + ": " + value + ")";
    }

    public T getValue() {
        return value;
    }

    public String getTypeName() {
        return typeName;
    }
}