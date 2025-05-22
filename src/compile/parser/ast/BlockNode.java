package src.compile.parser.ast;

import src.compile.parser.tac.Instruction.BeginBlockInstruction;
import src.compile.parser.tac.Instruction.EndBlockInstruction;
import src.compile.parser.tac.TACGenerator;

/**
 * @author sixteacher
 * @version 1.0
 * @description BlockNode
 * @date 2025/5/19
 */

/**
 * 代表一个程序块（通常由花括号 `{}` 包围）。
 * 它可以包含声明 (DeclsNode) 和语句列表 (StmtsNode)。
 * BlockNode 本身也可以被视为一种语句 (StmtNode)，例如在函数体或复合语句中。
 */
public class BlockNode extends StmtNode {
    private DeclsNode declarations; // 可以为 null
    private StmtsNode statements;   // 可以为 null

    // 构造函数
    public BlockNode(DeclsNode declarations, StmtsNode statements, int line, int pos) {
        super(line, pos);
        this.declarations = declarations;
        this.statements = statements;
    }

    // 如果你的解析器可能只创建空的BlockNode，然后通过setter设置
    public BlockNode(int line, int pos) {
        super(line, pos);
        this.declarations = null;
        this.statements = null;
    }


    @Override
    public void generateTac(TACGenerator gen) {
        // 1. 进入新的作用域
        //    确保 TACGenerator 有一个 SymbolTableManager 的实例
        if (gen.getSymbolTableManager() != null) {
            gen.getSymbolTableManager().enterScope();
        } else {
            // 这是一个潜在的问题，如果符号表管理器是必须的
            System.err.println("Warning: SymbolTableManager is null in TACGenerator during BlockNode processing at line " + line + ".");
        }

        // {{ 修改1：激活并格式化块开始标记 }}
        // 在TAC中明确标记块的开始，可以包含源文件行号以便追溯
        gen.emit(new BeginBlockInstruction(this.line));

        // 2. 为声明部分生成TAC (如果存在)
        //    声明通常会向当前作用域（即刚进入的作用域）添加符号

        if (declarations != null) {
            declarations.generateTac(gen);
        }

        // 3. 为语句部分生成TAC (如果存在)
        if (statements != null) {
            statements.generateTac(gen);
        }

        // {{ 修改2：激活并格式化块结束标记 }}
        // 在TAC中明确标记块的结束

        gen.emit(new EndBlockInstruction(this.line));
        // 4. 退出作用域

        if (gen.getSymbolTableManager() != null) {
            gen.getSymbolTableManager().leaveScope();
        }
        // BlockNode 本身不直接产生计算结果的 'place' 或代表类型的 'typeString'，
        // 它的主要作用是组织代码结构和管理作用域。
    }


    @Override
    public String getNodeTypeString() {
        return "BlockNode";
    }

    public DeclsNode getDeclarations() {
        return declarations;
    }

    public void setDeclarations(DeclsNode declarations) {
        this.declarations = declarations;
    }

    public StmtsNode getStatements() {
        return statements;
    }

    public void setStatements(StmtsNode statements) {
        this.statements = statements;
    }
}