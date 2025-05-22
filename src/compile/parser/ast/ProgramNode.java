package src.compile.parser.ast;

import src.compile.parser.tac.TACGenerator;

/**
 * @author sixteacher
 * @version 1.0
 * @description ProgramNode
 * @date 2025/5/19
 */

/**
 * 代表整个程序的 AST 根节点。
 * 通常包含一个程序块 (BlockNode)作为其主要内容。
 */
public class ProgramNode extends ASTNode {

    public final BlockNode block; // 假设一个程序是一个单一的块

    public ProgramNode(BlockNode block, int line, int pos) {
        super(line, pos);
        this.block = block;
    }

    @Override
    public void generateTac(TACGenerator gen) {
        // 为整个程序生成TAC:
        // 1. 可能涉及设置全局上下文或入口点 (例如 'main' 函数标签)。
        // 2. 然后，为程序的主块生成TAC。

        // 示例: 为程序开始或 main 函数发出一个标签
        // 你可以根据你的目标架构或语言规范决定是否需要一个特定的入口标签
        // gen.emit(new LabelInstruction(gen.newLabel("main_entry"))); // 或类似

        if (block != null) {
            block.generateTac(gen);
        }

        // 示例: 发出一个程序结束指令或停止指令
        // 这条指令告诉解释器/机器停止执行
        // gen.emit(new HaltInstruction()); // 假设你有这样的指令
    }

    @Override
    public String getNodeTypeString() {
        return "ProgramNode";
    }

    public BlockNode getBlock() {
        return block;
    }
}