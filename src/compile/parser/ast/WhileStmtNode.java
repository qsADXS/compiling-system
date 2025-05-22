package src.compile.parser.ast;

import src.compile.parser.tac.Address.Address;
import src.compile.parser.tac.Address.LabelAddress;
import src.compile.parser.tac.Address.NameAddress;
import src.compile.parser.tac.Instruction.GotoInstruction;
import src.compile.parser.tac.Instruction.IfFalseGotoInstruction;
import src.compile.parser.tac.Instruction.LabelInstruction;
import src.compile.parser.tac.TACGenerator;

/**
 * @author sixteacher
 * @version 1.0
 * @description WhileStmtNode
 * @date 2025/5/19
 */


/**
 * 代表 'while' 循环语句。
 * 包含一个循环条件表达式 (ExprNode) 和一个循环体 (StmtNode)。
 */
public class WhileStmtNode extends StmtNode {
    public final ExprNode condition;
    public final StmtNode body;

    public WhileStmtNode(ExprNode condition, StmtNode body, int line, int pos) {
        super(line, pos);
        this.condition = condition;
        this.body = body;
    }

    @Override
    public void generateTac(TACGenerator gen) {
        LabelAddress conditionStartLabel = gen.newLabel("WHILE_CONDITION_START (evaluate while condition)");
        LabelAddress loopExitLabel = gen.newLabel("WHILE_LOOP_EXIT (while condition false or break)");

        // 1. 设置循环上下文 (只传递 break 标签)
        gen.pushLoopContext(loopExitLabel);

        // 2. 定义条件检查开始的标签
        // 此标签 (conditionStartLabel) 标记了 while 循环条件判断的入口。
        // 每次循环迭代（在循环体执行完毕后）都会无条件跳转回此标签，以重新进行条件判断。
        gen.emit(new LabelInstruction(conditionStartLabel));

        // 3. 生成条件的TAC
        this.condition.generateTac(gen);
        Address conditionPlace = this.condition.getPlace();

        // 4. 如果条件为假，跳转到循环出口
        gen.emit(new IfFalseGotoInstruction(conditionPlace, loopExitLabel));

        // 5. 生成循环体的TAC
        if (this.body != null) {
            this.body.generateTac(gen);
        }

        // 6. 无条件跳转回条件检查的开始
        gen.emit(new GotoInstruction(conditionStartLabel));

        // 7. 定义循环出口的标签
        // 此标签 (loopExitLabel) 标记了 while 循环的出口。
        // 当循环条件判断为假时，或者在循环体内遇到 break 语句时，程序会跳转到此标签。
        gen.emit(new LabelInstruction(loopExitLabel));

        // 8. 弹出循环上下文
        gen.popLoopContext();
    }

    @Override
    public String getNodeTypeString() {
        return "WhileStmt";
    }

    public ExprNode getCondition() {
        return condition;
    }

    public StmtNode getBody() {
        return body;
    }
}