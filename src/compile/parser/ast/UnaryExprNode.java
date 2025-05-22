package src.compile.parser.ast;

import src.compile.parser.tac.Address.Address;
import src.compile.parser.tac.Address.NameAddress;
import src.compile.parser.tac.Instruction.UnaryOpInstruction;
import src.compile.parser.tac.OpCode;
import src.compile.parser.tac.TACGenerator;

/**
 * @author sixteacher
 * @version 1.0
 * @description UnaryExprNode
 * @date 2025/5/19
 */

/**
 * 代表一元运算表达式（例如 `-x`, `!flag`）。
 * 包含一个操作数 (ExprNode) 和一个操作符。
 * 其 'place' 属性存储运算结果的地址。
 */
public class UnaryExprNode extends ExprNode {
    public final ExprNode operand;
    public final String operator; // 例如 "-", "!"

    public UnaryExprNode(ExprNode operand, String operator, int line, int pos) {
        super(line, pos);
        this.operand = operand;
        this.operator = operator;
    }

    @Override
    public void generateTac(TACGenerator gen) {
        // 1. 为操作数生成TAC
        this.operand.generateTac(gen);
        Address operandPlace = this.operand.getPlace();
        String operandType = this.operand.getTypeString();

        // 2. 创建一个新的临时变量来存储结果
        NameAddress resultPlace = gen.newTemp();
        this.place = resultPlace; // 设置此节点的结果存放位置

        // 3. 根据操作符发出相应的TAC指令并设置结果类型
        switch (this.operator) {
            case "-": // 数值取反
                // 假设有一个 UnaryOpInstruction(op, dest, src)
                // 或者更具体的 NegateInstruction(dest, src)
                gen.emit(new UnaryOpInstruction(resultPlace, OpCode.NEG, operandPlace));
                // this.setTypeString(operandType); // 通常取反不改变数值类型 (int -> int, float -> float)
                if ("int".equals(operandType) || "float".equals(operandType)) { // 简化的类型检查
                    this.setTypeString(operandType);
                } else {
                    // 类型错误处理
                    System.err.println("Semantic Error: Cannot apply numeric negate '-' to type " + operandType + " at line " + line + ", pos " + pos);
                    this.setTypeString("error_type");
                }
                break;
            case "!": // 逻辑非
                // 假设有一个 UnaryOpInstruction(op, dest, src)
                // 或者更具体的 NotInstruction(dest, src)
                gen.emit(new UnaryOpInstruction(resultPlace, OpCode.NOT,  operandPlace));
                // this.setTypeString("boolean"); // 逻辑非的结果总是布尔型
                if ("boolean".equals(operandType)) { // 简化的类型检查
                    this.setTypeString("boolean");
                } else {
                    // 类型错误处理
                    System.err.println("Semantic Error: Cannot apply logical not '!' to type " + operandType + " at line " + line + ", pos " + pos);
                    this.setTypeString("error_type");
                }
                break;
            // case "~": // 按位取反 (如果支持)
            //     gen.emit(new UnaryOpInstruction(UnaryOpInstruction.Operator.BITWISE_NOT, resultPlace, operandPlace));
            //     if ("int".equals(operandType)) {
            //         this.setTypeString("int");
            //     } else {
            //         System.err.println("Semantic Error: Cannot apply bitwise not '~' to type " + operandType + " at line " + line + ", pos " + pos);
            //         this.setTypeString("error_type");
            //     }
            //     break;
            default:
                System.err.println("Error: Unknown unary operator '" + this.operator + "' at line " + line + ", pos " + pos);
                this.setTypeString("error_type");
                // 可能需要将 this.place 设置为操作数本身或一个错误指示符
                this.place = operandPlace; // 或者一个特殊的错误 Address
                break;
        }
    }

    @Override
    public String getNodeTypeString() {
        return "UnaryExpr(" + operator + ")";
    }

    public ExprNode getOperand() {
        return operand;
    }

    public String getOperator() {
        return operator;
    }
}