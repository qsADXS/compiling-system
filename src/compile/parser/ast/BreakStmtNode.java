package src.compile.parser.ast;

import src.compile.parser.tac.Address.Address;
import src.compile.parser.tac.Address.LabelAddress;
import src.compile.parser.tac.Address.NameAddress;
import src.compile.parser.tac.Instruction.GotoInstruction;
import src.compile.parser.tac.OpCode;
import src.compile.parser.tac.TACGenerator;

/**
 * @author sixteacher
 * @version 1.0
 * @description BreakStmtNode
 * @date 2025/5/19
 */

/**
 * 代表 'break' 语句节点，用于跳出循环或 switch 语句。
 */
public class BreakStmtNode extends StmtNode {
    public BreakStmtNode(int line, int pos) {
        super(line, pos);
    }

    @Override
    public void generateTac(TACGenerator gen) {
        // 1. 从 TACGenerator 获取当前循环的出口标签
        //    TACGenerator 需要维护一个循环出口标签的栈。
        //    例如: Label breakTarget = gen.getCurrentLoopExitLabel();
        LabelAddress breakTarget = gen.getLoopExitLabel(); // 假设有这个方法

        if (breakTarget == null) {
            System.err.println("Semantic Error: 'break' statement not within a loop or switch at line " + line + ", pos " + pos);
            // 这种错误通常在语义分析阶段捕获，但这里也做个检查
            return;
        }

        // 将 Label 对象转换为 GotoInstruction 期望的 Address 类型
        // 这里假设标签的名称可以用来创建一个 NameAddress
        // 使用你的 GotoInstruction
        gen.emit(new GotoInstruction(breakTarget));
    }

    @Override
    public String getNodeTypeString() {
        return "BreakStmt";
    }
}