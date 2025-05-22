package src.compile.parser.ast;

import src.compile.parser.tac.TACGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sixteacher
 * @version 1.0
 * @description DeclsNode
 * @date 2025/5/19
 */

/**
 * 代表一系列声明的节点。
 * 它包含一个或多个单独的声明 (DeclNode)。
 */
public class DeclsNode extends ASTNode {
    private final List<DeclNode> declarations;

    public DeclsNode(int line, int pos) {
        super(line, pos);
        this.declarations = new ArrayList<>();
    }

    // 如果解析器一次性构建列表，可以添加一个接收列表的构造函数
    public DeclsNode(List<DeclNode> declarations, int line, int pos) {
        super(line, pos);
        this.declarations = new ArrayList<>(declarations); // 创建副本或直接使用
    }

    @Override
    public void generateTac(TACGenerator gen) {
        // 遍历每个声明节点并调用其 generateTac 方法
        for (DeclNode decl : declarations) {
            if (decl != null) { // 安全检查
                decl.generateTac(gen);
            }
        }
    }

    public void addDeclaration(DeclNode decl) {
        if (decl != null) {
            this.declarations.add(decl);
        }
    }

    public List<DeclNode> getDeclarations() {
        return declarations;
    }

    @Override
    public String getNodeTypeString() {
        return "DeclsNode (Count: " + declarations.size() + ")";
    }
}