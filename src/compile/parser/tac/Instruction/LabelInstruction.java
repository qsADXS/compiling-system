package src.compile.parser.tac.Instruction;

import src.compile.parser.tac.Address.Address;
import src.compile.parser.tac.Address.LabelAddress;
import src.compile.parser.tac.Instruction.Instruction;

/**
 * @author sixteacher
 * @version 1.0
 * @description LabelInstruction
 * @date 2025/5/19
 */

/**
 * 标签指令类 (LabelInstruction)。
 * 该类用于表示一个标签指令。在汇编或中间代码生成中，
 * 标签通常用作跳转指令的目标点，是控制程序流程的重要组成部分
 * (例如，{@code goto} 语句的跳转位置)。
 * 构造函数通过接收一个 {@code LabelAddress} 对象来初始化标签指令。
 * {@code toString} 方法返回标签的名称及其可选的描述信息。
 * 如果存在描述，格式为 {@code "标签名:  # 描述"}；若无描述，则仅返回标签名。
 */
public class LabelInstruction implements Instruction {
    public LabelAddress labelAddress; // 明确类型为 LabelAddress

    // 构造函数接收 LabelAddress
    public LabelInstruction(LabelAddress labelAddress) {
        if (labelAddress == null) {
            throw new IllegalArgumentException("LabelAddress for LabelInstruction cannot be null.");
        }
        this.labelAddress = labelAddress;
    }

    @Override
    public String toString() {
        String labelName = labelAddress.getLabel(); // 获取纯标签名
        String description = labelAddress.getDescription(); // 获取描述

        if (description != null && !description.trim().isEmpty()) {
            // 如果有描述，格式为 "标签名: # 描述"
            return labelName + ": \t# " + description;
        } else {
            // 没有描述，则为 "标签名:"
            return labelName + ":";
        }
    }
}