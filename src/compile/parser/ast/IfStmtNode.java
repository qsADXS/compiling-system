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
 * @description IfStmtNode
 * @date 2025/5/19
 */

/**
 * 代表条件语句 (if-then-else)。
 * 包含一个条件表达式 (ExprNode)、一个 'then' 分支 (StmtNode) 以及一个可选的 'else' 分支 (StmtNode)。
 */
public class IfStmtNode extends StmtNode {
    public final ExprNode condition;
    public final StmtNode thenBranch;
    public final StmtNode elseBranch; // Can be null

    public IfStmtNode(ExprNode condition, StmtNode thenBranch, StmtNode elseBranch, int line, int pos) {
        super(line, pos);
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    @Override
    public void generateTac(TACGenerator gen) {
        // 1. 生成条件的TAC
        this.condition.generateTac(gen);
        Address conditionPlace = this.condition.getPlace();
        // String conditionType = this.condition.getTypeString(); // 可用于类型检查

        if (this.elseBranch == null) {
            //情况 A: if (condition) thenBranch
            // L_END_IF:
            //    <condition_code> -> t1
            //    IF_FALSE t1, GOTO L_END_IF
            //    <then_branch_code>
            // L_END_IF:

            LabelAddress endIfLabel = gen.newLabel("IF_END (simple if: condition false or then_branch completed)");

            // 2. 如果条件为假，跳转到 if 语句末尾
            gen.emit(new IfFalseGotoInstruction(conditionPlace, endIfLabel));

            // 3. 生成 then 分支的TAC
            if (this.thenBranch != null) {
                this.thenBranch.generateTac(gen);
            }

            // 4. 定义 if 语句末尾的标签
            // 此标签 (endIfLabel) 标记了 if 语句的结束点。
            // 如果初始条件为假，执行流会直接跳转到此标签。
            // 如果初始条件为真，then 分支执行完毕后，执行流会自然到达此标签。
            gen.emit(new LabelInstruction(endIfLabel));

        } else {
            // 情况 B: if (condition) thenBranch else elseBranch
            // L_ELSE:
            // L_END_IF:
            //    <condition_code> -> t1
            //    IF_FALSE t1, GOTO L_ELSE
            //    <then_branch_code>
            //    GOTO L_END_IF
            // L_ELSE:
            //    <else_branch_code>
            // L_END_IF:

            LabelAddress elseLabel = gen.newLabel("ELSE_BRANCH_START (if condition false)");
            LabelAddress endIfLabel = gen.newLabel("IF_ELSE_END (after then or else branch)");

            // 2. 如果条件为假，跳转到 else 分支
            gen.emit(new IfFalseGotoInstruction(conditionPlace, elseLabel));

            // 3. 生成 then 分支的TAC
            if (this.thenBranch != null) {
                this.thenBranch.generateTac(gen);
            }
            // 4. then 分支执行完毕后，无条件跳转到 if 语句末尾
            gen.emit(new GotoInstruction(endIfLabel));

            // 5. 定义 else 分支开始的标签
            // 此标签 (elseLabel) 标记了 else 分支代码的开始。
            // 如果初始条件为假，执行流会跳转到此标签开始执行 else 分支。
            gen.emit(new LabelInstruction(elseLabel));

            // 6. 生成 else 分支的TAC
            // elseBranch 此时不为 null (因为我们在这个 if-else 块中)
            this.elseBranch.generateTac(gen);

            // 7. 定义 if 语句末尾的标签
            // 此标签 (endIfLabel) 标记了整个 if-else 语句的结束点。
            // then 分支通过一个 GOTO 指令跳转到此标签。
            // else 分支执行完毕后，执行流会自然到达此标签。
            gen.emit(new LabelInstruction(endIfLabel));
        }
    }

    @Override
    public String getNodeTypeString() {
        return "IfStmt";
    }

    public ExprNode getCondition() {
        return condition;
    }

    public StmtNode getThenBranch() {
        return thenBranch;
    }

    public StmtNode getElseBranch() {
        return elseBranch;
    }
}