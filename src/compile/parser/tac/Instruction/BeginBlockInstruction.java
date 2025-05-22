package src.compile.parser.tac.Instruction;

/**
 * @author sixteacher
 * @version 1.0
 * @description BeginBlockInstruction
 * @date 2025/5/21
 */

/**
 * 表示一个代码块开始的指令。
 * 它通常用于标记一段具有特定作用域或逻辑功能的代码的起始位置。
 */
public class BeginBlockInstruction implements Instruction {
    private int sourceLine; // 记录源代码行号，便于追溯

    public BeginBlockInstruction(int sourceLine) {
        this.sourceLine = sourceLine;
    }

    public int getSourceLine() {
        return sourceLine;
    }

    @Override
    public String toString() {
        return "BEGIN_BLOCK (Line: " + sourceLine + ")";
    }

}