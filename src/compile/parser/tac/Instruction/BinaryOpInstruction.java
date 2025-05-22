package src.compile.parser.tac.Instruction;

import src.compile.parser.tac.Address.Address;
import src.compile.parser.tac.Instruction.Instruction;
import src.compile.parser.tac.OpCode;

/**
 * @author sixteacher
 * @version 1.0
 * @description BinaryOpInstruction
 * @date 2025/5/19
 */

/**
 * 二元操作指令类 (BinaryOpInstruction)。
 * 该类表示一个二元运算操作指令，它包含两个操作数以及一个操作符。
 * 其主要目的是执行像加法、减法这样的二元运算，并将运算结果存储到指定的目标地址中。
 * 构造函数通过接收目标地址、两个操作数以及操作符来创建一个新的二元操作指令实例。
 * {@code toString} 方法返回该指令的字符串表现形式，通常格式为 {@code (操作符, 操作数1, 操作数2, 目标地址)}。
 */
public class BinaryOpInstruction implements Instruction {
    public Address destination;
    public Address operand1;

    public OpCode operator;
    public Address operand2;

    public BinaryOpInstruction(Address destination, Address operand1, OpCode operator, Address operand2) {
        this.destination = destination;
        this.operand1 = operand1;
        this.operator = operator;
        this.operand2 = operand2;
    }

    /*
    @Override
    public String toString() {
        return destination + " = " + operand1 + " " + operator + " " + operand2;
    }

     */

    @Override
    public String toString() {
        return "(" + operator + ", " + operand1 + ", " + operand2 + ", " + destination + ")";
    }
}
