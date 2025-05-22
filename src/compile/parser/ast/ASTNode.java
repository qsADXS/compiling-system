package src.compile.parser.ast;

import src.compile.parser.tac.Address.Address;
import src.compile.parser.tac.TACGenerator;


/**
 * 抽象语法树 (AST) 中所有节点的基类。
 * 它通常包含节点的源代码位置（行号、列号）以及可能的中间代码生成相关信息（如 'place' 属性）。
 * 所有具体的AST节点（如 ProgramNode, ExprNode, StmtNode 等）都应继承自此类。
 */
public abstract class ASTNode {
    public final int line;
    public final int pos;
    public Address place; // Used for TAC generation (temp variable, variable name, or literal string)

    public abstract void generateTac(TACGenerator gen);

    public ASTNode(int line, int pos) {
        this.line = line;
        this.pos = pos;
        this.place = null; // Initialize place
    }

    /**
     * Returns a string representation of the node's type.
     * Useful for debugging and simple AST printing.
     * @return String representing the node type.
     */
    public abstract String getNodeTypeString();




    // Consider adding an accept method for the Visitor pattern later
    // public abstract <T> T accept(ASTVisitor<T> visitor);

    //public abstract void generateAndEmitTac(TACGenerator gen);

    @Override
    public String toString() {
        return String.format("%s (L:%d, P:%d, Place:%s)", getNodeTypeString(), line, pos, place != null ? place : "N/A");
    }
}