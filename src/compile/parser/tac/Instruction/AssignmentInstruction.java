package src.compile.parser.tac.Instruction;

import src.compile.parser.tac.Address.Address;
import src.compile.parser.tac.Instruction.Instruction;

/**
 * @author sixteacher
 * @version 1.0
 * @description AssignmentInstruction
 * @date 2025/5/19
 */


/**
 * 表示一条赋值指令。
 * 这条指令将源地址 (source) 的值赋给目标地址 (destination)。
 */
public class AssignmentInstruction implements Instruction {
    public Address destination;
    public Address source;

    public AssignmentInstruction(Address destination, Address source) {
        this.destination = destination;
        this.source = source;
    }

    /*
    @Override
    public String toString() {
        return destination + " = " + source;
    }
     */

    @Override
    public String toString() {
        return "(=, " + source + ", _, " + destination + ")";
    }
}