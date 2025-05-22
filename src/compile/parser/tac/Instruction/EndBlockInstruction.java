package src.compile.parser.tac.Instruction;

/**
 * @author sixteacher
 * @version 1.0
 * @description EndBlockInstruction
 * @date 2025/5/21
 */

/**
 * 表示一个代码块结束的指令。
 * 它标记了由 BeginBlockInstruction 开始的代码段的结束。
 */
public class EndBlockInstruction implements Instruction {
    private int sourceLine;

    public EndBlockInstruction(int sourceLine) {
        this.sourceLine = sourceLine;
    }

    public int getSourceLine() {
        return sourceLine;
    }

    @Override
    public String toString() {
        return "END_BLOCK (Line: " + sourceLine + ")";
    }

}