package src.compile.parser.ast;

import src.compile.parser.tac.Address.Address;

/**
 * @author sixteacher
 * @version 1.0
 * @description ExprNode
 * @date 2025/5/19
 */

/**
 * 所有表达式节点的基类。
 * 表达式计算结果后，其值（或其地址）通常存储在 'place' 属性中，用于后续的中间代码生成。
 */
public abstract class ExprNode extends ASTNode {
    protected String typeString; // {{ 新增：存储此表达式的类型字符串 }}

    public ExprNode(int line, int pos) {
        super(line, pos);
        this.typeString = null; // 初始化类型字符串
    }

    /**
     * 获取此表达式计算出的值的类型。
     * 该值应在 generateTac 或语义分析期间设置。
     * @return 表达式的类型字符串 (例如 "int", "float", "array(int,10)", "bool")。
     */
    public String getTypeString() {
        return typeString;
    }

    /**
     * 设置此表达式的类型字符串。
     * 通常在语义分析或 generateTac 中调用。
     * @param typeString 要设置的类型字符串。
     */
    public void setTypeString(String typeString) {
        this.typeString = typeString;
    }

    /**
     * 获取此表达式在TAC中计算结果存放的"位置"（变量名或临时变量）。
     * @return 代表此表达式结果的 place 字符串。
     */
    public Address getPlace() {
        return this.place; // 'place' 继承自 ASTNode
    }

    // generateTac 是抽象的，由具体子类实现
    // public abstract void generateTac(TACGenerator gen);
}