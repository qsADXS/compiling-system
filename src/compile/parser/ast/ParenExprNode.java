package src.compile.parser.ast;

import src.compile.parser.tac.TACGenerator;

/**
 * @author sixteacher
 * @version 1.0
 * @description ParenExprNode
 * @date 2025/5/19
 */

/**
 * ParenExprNode 类代表程序中的括号表达式 (Parenthesized Expression)。
 * 当遇到用括号括起来的表达式时，会被创建出来作为一个抽象语法树
 */
public class ParenExprNode extends ExprNode {

    public final ExprNode expression;

    public ParenExprNode(ExprNode expression, int line, int pos) {
        super(line, pos);
        this.expression = expression;
    }

    @Override
    public void generateTac(TACGenerator gen) {
        // 1. 为内部表达式生成TAC
        this.expression.generateTac(gen);

        // 2. ParenExprNode 的 place 和 typeString 与其内部表达式相同
        this.place = this.expression.getPlace(); // getPlace() 返回 Address
        // 假设 ExprNode 有 setTypeString 或可以直接访问/设置 typeString
        this.setTypeString(this.expression.getTypeString());
    }

    @Override
    public String getNodeTypeString() {
        return "ParenExpr";
    }

    public ExprNode getExpression() {
        return expression;
    }
}