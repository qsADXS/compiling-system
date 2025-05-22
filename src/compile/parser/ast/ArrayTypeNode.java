package src.compile.parser.ast;

import src.compile.parser.tac.TACGenerator;

/**
 * @author sixteacher
 * @version 1.0
 * @description ArrayTypeNode
 * @date 2025/5/19
 */

/**
 * 代表数组类型。
 * 它继承自 TypeNode，并额外存储了数组的基类型和维度大小。
 * 其 'place' 属性在 `createASTNode` 中被设置为一个 `NameAddress`，存储数组类型的描述字符串。
 */
public class ArrayTypeNode extends TypeNode {
    public final TypeNode baseType; // 数组的元素类型节点
    public final int size;         // 数组的大小 (对于固定大小数组)

    public ArrayTypeNode(TypeNode baseType, int size, int line, int pos) {
        super(line, pos); // 调用 TypeNode 构造函数
        this.baseType = baseType;
        this.size = size;
        // TypeNode 的构造函数或此类自身可能会设置继承的 'place' 或 'typeString'
        // 例如: this.typeString = getTypeName();
        // 但对于TypeNode，'place' 通常不适用。
    }

    @Override
    public void generateTac(TACGenerator gen) {
        // 类型节点通常不直接生成 TAC 指令。
        // 它们的信息在语义分析期间用于构建符号表条目，
        // 或在代码生成期间由其他节点 (如 DeclNode) 使用。
        if (baseType != null) {
            baseType.generateTac(gen); // 以防 baseType (也是一个 TypeNode) 有任何相关逻辑
        }
        // 此节点本身不发出指令。
    }

    @Override
    public String getNodeTypeString() {
        return "ArrayType(Size: " + size + ")";
    }

    public TypeNode getBaseType() {
        return baseType;
    }

    public int getSize() {
        return size;
    }

    /**
     * 返回此数组类型的规范字符串表示形式。
     * 例如："array(int, 10)" 或 "array(float, 20)"
     * @return 类型名称字符串。
     */
    @Override
    public String getTypeName() { // 假设 TypeNode 定义了 getTypeName()
        // 确保 baseType.getTypeName() 返回的是元素类型的名称，如 "int"
        return "array(" + baseType.getTypeName() + ", " + size + ")";
    }
}