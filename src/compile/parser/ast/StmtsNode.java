package src.compile.parser.ast;

import src.compile.parser.tac.TACGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sixteacher
 * @version 1.0
 * @description StmtsNode
 * @date 2025/5/19
 */

/**
 * 代表一个语句序列的节点。
 * 它包含一个或多个单独的语句 (StmtNode)。
 */
public class StmtsNode extends ASTNode { // StmtsNode 直接继承 ASTNode，因为它本身不是一个语句或表达式
    private final List<StmtNode> statements;

    public StmtsNode(int line, int pos) {
        super(line, pos);
        this.statements = new ArrayList<>();
    }

    @Override
    public void generateTac(TACGenerator gen) {
        // 为语句列表生成TAC:
        // 依次遍历每个语句并调用其 generateTac 方法。
        for (StmtNode stmt : statements) {
            if (stmt != null) { // 良好的实践是检查null
                stmt.generateTac(gen);
            }
        }
    }

    public void addStatement(StmtNode stmt) {
        this.statements.add(stmt);
    }

    public List<StmtNode> getStatements() {
        return statements;
    }

    @Override
    public String getNodeTypeString() {
        return "StmtsNode";
    }
}