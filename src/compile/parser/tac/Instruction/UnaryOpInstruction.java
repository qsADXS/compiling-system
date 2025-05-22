package src.compile.parser.tac.Instruction;

import src.compile.parser.tac.Address.Address;
import src.compile.parser.tac.Instruction.Instruction;
import src.compile.parser.tac.OpCode;

/**
 * @author sixteacher
 * @version 1.0
 * @description UnaryOpInstruction
 * @date 2025/5/19
 */

/**
 * 一元操作指令类 (UnaryOpInstruction)。
 * 该类表示一个一元操作指令，它包含一个操作符和一个操作数，
 * 并将运算结果存储到指定的目标地址。
 * 这类指令通常用于执行只有一个操作数的运算，如取负 ({@code -}) 或逻辑非 ({@code !})。
 * 构造函数通过接收目标地址、操作符以及操作数来构造一个一元操作指令实例。
 * {@code toString} 方法返回该一元操作指令的字符串表示，
 * 通常格式为 {@code (操作符, 操作数, _, 目标地址)}
 * (下划线通常表示该位置的参数在此指令中不使用)。
 */
public class UnaryOpInstruction implements Instruction {
    public Address destination;
    public OpCode operator;
    public Address operand;

    public UnaryOpInstruction(Address destination, OpCode operator, Address operand) {
        this.destination = destination;
        this.operator = operator;
        this.operand = operand;
    }

    /*
    @Override
    public String toString() {
        return destination + " = " + operator + " " + operand;
    }
     */
    @Override
    public String toString() {
        return "(" + operator + ", " + operand + ", _, " + destination + ")";
    }

}