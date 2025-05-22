package src.compile.parser.ast;

import src.compile.parser.tac.TACGenerator;

/**
 * @author sixteacher
 * @version 1.0
 * @description StmtNode
 * @date 2025/5/19
 */

/**
 * 所有具体语句节点（如赋值、条件、循环等）的基类。
 * 定义了语句节点的通用接口或属性。
 */
public abstract class StmtNode extends ASTNode {
    public StmtNode(int line, int pos) {
        super(line, pos);
    }

}