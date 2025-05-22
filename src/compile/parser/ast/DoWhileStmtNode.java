package src.compile.parser.ast;

import src.compile.lexer.LexicalAnalyzer;
import src.compile.parser.tac.Address.Address;
import src.compile.parser.tac.Address.LabelAddress;
import src.compile.parser.tac.Address.NameAddress;
import src.compile.parser.tac.Instruction.IfTrueGotoInstruction;
import src.compile.parser.tac.Instruction.LabelInstruction;
import src.compile.parser.tac.TACGenerator;

/**
 * @author sixteacher
 * @version 1.0
 * @description DoWhileStmtNode
 * @date 2025/5/19
 */

/**
 * 代表 'do-while' 循环语句。
 * 包含一个循环体 (StmtNode) 和一个循环条件表达式 (ExprNode)。
 */
public class DoWhileStmtNode extends StmtNode {
    public final StmtNode body;
    public final ExprNode condition;

    public DoWhileStmtNode(StmtNode body, ExprNode condition, int line, int pos) {
        super(line, pos);
        this.body = body;
        this.condition = condition;
    }

    @Override
    public void generateTac(TACGenerator gen) {
        LabelAddress bodyStartLabel = gen.newLabel("DO_WHILE_BODY_START (start of body / loop back if cond true)");
        LabelAddress loopExitLabel = gen.newLabel("DO_WHILE_LOOP_EXIT (condition false or break)");

        // 1. 设置循环上下文，用于 break
        gen.pushLoopContext(loopExitLabel);

        // 2. 定义循环体开始的标签
        // 此标签 (bodyStartLabel) 标记了 do-while 循环体的开始。
        // 循环体至少执行一次。在每次循环体执行完毕并进行条件判断后，如果条件为真，程序会跳转回此标签。
        gen.emit(new LabelInstruction(bodyStartLabel));

        // 3. 生成循环体的TAC
        if (this.body != null) {
            this.body.generateTac(gen);
        }
        // 在循环体之后，自然会流向条件评估

        // 5. 生成条件的TAC
        this.condition.generateTac(gen);
        Address conditionPlace = this.condition.getPlace();

        // 6. 如果条件为真 (非0)，则跳转回循环体开始
        gen.emit(new IfTrueGotoInstruction(conditionPlace, bodyStartLabel));


        // 7. 定义循环出口标签
        // 此标签 (loopExitLabel) 标记了 do-while 循环的出口。
        // 当循环条件判断为假时（即不再跳转回 bodyStartLabel），或者在循环体内遇到 break 语句时，程序会执行到此标签。
        gen.emit(new LabelInstruction(loopExitLabel));

        // 8. 弹出循环上下文
        gen.popLoopContext();
    }

    @Override
    public String getNodeTypeString() {
        return "DoWhileStmt";
    }

    public StmtNode getBody() {
        return body;
    }

    public ExprNode getCondition() {
        return condition;
    }
}