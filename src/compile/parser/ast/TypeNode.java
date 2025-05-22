package src.compile.parser.ast;

import src.compile.parser.tac.TACGenerator;

/**
 * @author sixteacher
 * @version 1.0
 * @description TypeNode
 * @date 2025/5/19
 */

/**
 * 代表一个数据类型。
 * 可以是基本类型（如 int, float）或更复杂的类型（如数组类型 ArrayTypeNode）。
 * 其 'place' 属性在 `createASTNode` 中被设置为一个 `NameAddress`，存储类型名称本身作为一种标识。
 */
public class TypeNode extends ASTNode {
    public final String typeName; // 对于基本类型如 "int", "float", 或数组的基类型

    // 基本类型构造函数
    public TypeNode(String typeName, int line, int pos) {
        super(line, pos);
        this.typeName = typeName;
    }

    // 对于像 ArrayTypeNode 这样的子类，它们不直接在这里设置 typeName
    protected TypeNode(int line, int pos) {
        super(line, pos);
        this.typeName = null; // 将由具体的类型表示来设置
    }

    @Override
    public void generateTac(TACGenerator gen) {
        // 为 TypeNode 生成TAC:
        // 通常，TypeNode 本身不直接生成可执行的TAC指令。
        // 它的信息在语义分析（类型检查）期间以及被其他节点
        // (如 DeclNode 或 CastExprNode) 用来确定类型或大小。

        // 如果需要，可以将类型信息字符串存储在 place 中，供TAC的其他阶段使用。
        // 例如，可以将其表示为一个 NameAddress。
        // if (this.typeName != null) {
        //     this.place = new NameAddress("TYPEINFO_" + this.typeName);
        // }
        // 然而，对于大多数编译器，类型节点在TAC生成阶段是无操作的。
        // 它主要在构建AST和语义分析时起作用。
    }


    @Override
    public String getNodeTypeString() {
        return "Type(" + (typeName != null ? typeName : "Complex") + ")";
    }

    public String getTypeName() {
        return typeName;
    }
}