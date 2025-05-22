package src.compile.parser.ast;

import src.compile.parser.tac.TACGenerator;

/**
 * @author sixteacher
 * @version 1.0
 * @description GenericNode
 * @date 2025/5/19
 */

/**
 * 一个通用的 AST 节点，当没有为特定产生式左部符号定义专门的 AST 节点类型时，用作回退。
 * 它通常只存储非终结符的名称。
 */
public class GenericNode extends ASTNode {
    private final String description;

    public GenericNode(String description, int line, int pos) {
        super(line, pos);
        this.description = description;
    }

    @Override
    public void generateTac(TACGenerator gen) {
        // 通常，GenericNode 不会生成可执行的TAC。
        // 它可能是一个解析错误、未识别的结构或用于AST构建过程中的临时节点。
        // 在TAC生成阶段，可以记录一个警告或错误。
        System.err.println("Warning: Attempting to generate TAC for GenericNode: '" +
                description + "' at line " + this.line + ", pos " + this.pos +
                ". No TAC will be generated for this node.");
        // 或者，如果你有注释指令:
        // gen.emitComment("GenericNode encountered: " + description);
    }

    @Override
    public String getNodeTypeString() {
        return "GenericNode(" + description + ")";
    }

    public String getDescription() {
        return description;
    }
}