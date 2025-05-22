package src.compile.parser.tac.Instruction;

/**
 * 表示任何可执行指令的接口。
 * 所有具体的指令类（如赋值、跳转、声明等）都应实现此接口。
 * 该接口强制实现 toString() 方法，以便以统一的方式获取指令的文本表示。
 */
public interface Instruction {
    String toString();
}
