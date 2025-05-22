package src.compile.parser.ast;

import src.compile.parser.tac.Address.Address;
import src.compile.parser.tac.Address.ConstantAddress;
import src.compile.parser.tac.Address.LabelAddress;
import src.compile.parser.tac.Instruction.*;
import src.compile.parser.tac.TACGenerator;

/**
 * @author sixteacher
 * @version 1.0
 * @description ShortCircuitExprNode
 * @date 2025/5/21
 */

/**
 * 代表支持短路求值的逻辑运算表达式（如 `&&`, `||`）。
 * 它特殊处理条件跳转的生成，而不是简单计算一个布尔值存入place。
 * 其 'place' 属性最终会存储整个表达式的布尔结果 (通常是0或1)。
 */
public class ShortCircuitExprNode extends ExprNode {
    public ExprNode left;
    public ExprNode right;
    public String operator; // "&&" or "||"

    public ShortCircuitExprNode(int line, int pos, ExprNode left, String operator, ExprNode right) {
        super(line, pos);
        this.left = left;
        this.right = right;
        this.operator = operator;
        this.typeString = "bool"; // 默认为布尔类型
    }

    @Override
    public void generateTac(TACGenerator gen) {
        // 生成左侧表达式的 TAC
        left.generateTac(gen);
        Address leftAddr = left.getPlace();

        // 申请结果临时变量
        Address result = gen.newTemp();
        this.place = result;

        // 创建跳转标签
        LabelAddress labelTrue = gen.newLabel();  // 主要用于 OR
        LabelAddress labelFalse = gen.newLabel(); // 主要用于 AND
        LabelAddress labelEnd = gen.newLabel();

        if ("&&".equals(operator)) {
            // if !left goto false (labelFalse)
            gen.emit(new IfFalseGotoInstruction(leftAddr, labelFalse));
            // 生成右侧
            right.generateTac(gen);
            Address rightAddr = right.getPlace();
            // if !right goto false (labelFalse)
            gen.emit(new IfFalseGotoInstruction(rightAddr, labelFalse));

            // 如果执行到这里，说明 left 和 right 都为 true
            gen.emit(new AssignmentInstruction(result, new ConstantAddress<>(true))); // result = true
            gen.emit(new GotoInstruction(labelEnd)); // 然后跳转到结束

            // false: (如果 left 或 right 为 false，则跳转到这里)
            gen.emit(new LabelInstruction(labelFalse));
            gen.emit(new AssignmentInstruction(result, new ConstantAddress<>(false))); // result = false
            // 如果执行了 false 分支，会自然流向下面的 labelEnd

        } else if ("||".equals(operator)) {
            // if left goto true (labelTrue)
            gen.emit(new IfTrueGotoInstruction(leftAddr, labelTrue));
            // 生成右侧
            right.generateTac(gen);
            Address rightAddr = right.getPlace();
            // if right goto true (labelTrue)
            gen.emit(new IfTrueGotoInstruction(rightAddr, labelTrue));

            // 如果执行到这里，说明 left 和 right 都为 false
            gen.emit(new AssignmentInstruction(result, new ConstantAddress<>(false))); // result = false
            gen.emit(new GotoInstruction(labelEnd)); // 跳转到结束

            // true: (如果 left 或 right 为 true，则跳转到这里)
            gen.emit(new LabelInstruction(labelTrue));
            gen.emit(new AssignmentInstruction(result, new ConstantAddress<>(true))); // result = true
            // 如果执行了 true 分支，会自然流向下面的 labelEnd

        } else {
            throw new RuntimeException("Invalid short-circuit operator: " + operator);
        }

        // end: (所有路径最终汇合到这里)
        gen.emit(new LabelInstruction(labelEnd));
    }

    @Override
    public String getNodeTypeString() {
        return "ShortCircuitExpr(" + operator + ")";
    }
}