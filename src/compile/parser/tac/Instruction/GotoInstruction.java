package src.compile.parser.tac.Instruction;

import src.compile.parser.tac.Address.Address;
import src.compile.parser.tac.Instruction.Instruction;

/**
 * @author sixteacher
 * @version 1.0
 * @description GotoInstruction
 * @date 2025/5/19
 */

/**
 * 表示一条无条件跳转指令。
 * 执行时，程序控制流将无条件转移到指定的标签位置。
 */
public class GotoInstruction implements Instruction {
    public Address label;

    public GotoInstruction(Address label) {
        this.label = label;
    }

    /*
    @Override
    public String toString() {
        return "goto " + label;
    }

     */

    @Override
    public String toString() {
        return "(goto, _, _, " + label + ")";
    }
}