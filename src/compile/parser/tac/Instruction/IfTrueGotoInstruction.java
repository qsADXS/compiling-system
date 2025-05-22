package src.compile.parser.tac.Instruction;

import src.compile.parser.tac.Address.Address;
import src.compile.parser.tac.Instruction.Instruction;

/**
 * @author sixteacher
 * @version 1.0
 * @description IfTrueGotoInstruction
 * @date 2025/5/19
 */

/**
 * 表示一条条件为真时跳转的指令。
 * 如果指定的条件 (condition) 计算结果为真，则程序控制流将跳转到指定的标签 (label) 位置。
 */
public class IfTrueGotoInstruction implements Instruction {
    public Address condition;
    public Address label;

    public IfTrueGotoInstruction(Address condition, Address label) {
        this.condition = condition;
        this.label = label;
    }
    /*
    @Override
    public String toString() {
        return "if True " + condition + " goto " + label;
    }

     */


    @Override
    public String toString() {
        return "(iftrue, " + condition + ", _, " + label + ")";
    }
}