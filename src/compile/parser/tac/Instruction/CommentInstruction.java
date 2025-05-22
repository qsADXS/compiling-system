package src.compile.parser.tac.Instruction;

/**
 * @author sixteacher
 * @version 1.0
 * @description insr
 * @date 2025/5/21
 */

/**
 * 表示一条注释指令。
 * 注释用于在生成的代码中提供解释性文本，不会被执行。
 */
public class CommentInstruction implements Instruction { // 或者 extends BaseTACInstruction
    private String comment;

    public CommentInstruction(String comment) {
        if (comment == null) {
            this.comment = "";
        } else {
            // 确保注释不会包含换行符，如果需要，可以进一步处理或限制
            this.comment = comment.replace("\n", " ").replace("\r", "");
        }
    }

    public String getComment() {
        return comment;
    }

    @Override
    public String toString() {
        // 通常，注释在三地址代码中以特定字符开头，如 '#' 或 '//'
        // 你选择的格式 "# " 能够很好地区分，并且与你之前的例子一致
        return "# " + comment;
    }

    // 如果 TACInstruction 接口需要其他方法 (如 getReadOperands, getWrittenOperand)，
    // 对于 CommentInstruction，它们通常返回 null 或空集合。
    // 例如:
    // @Override
    // public List<Operand> getReadOperands() { return Collections.emptyList(); }
    // @Override
    // public Operand getWrittenOperand() { return null; }
}