package src.compile.parser.ast;

import src.compile.parser.tac.Address.Address;
import src.compile.parser.tac.Address.NameAddress;
import src.compile.parser.tac.Instruction.BinaryOpInstruction;
import src.compile.parser.tac.OpCode;
import src.compile.parser.tac.TACGenerator;

/**
 * @author sixteacher
 * @version 1.0
 * @description BinaryExprNode
 * @date 2025/5/19
 */

/**
 * 代表二元运算表达式（例如 `a + b`, `x > y`）。
 * 包含左右两个操作数 (ExprNode) 和一个操作符。
 * 其 'place' 属性通常是一个临时变量，用于存储运算结果的地址。
 */
public class BinaryExprNode extends ExprNode {
    public final ExprNode left;
    public final ExprNode right;
    public final String operator; // 例如 "+", "-", "*", "/", "==", "<", "&&", "||"

    public BinaryExprNode(ExprNode left, ExprNode right, String operator, int line, int pos) {
        super(line, pos);
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    @Override
    public void generateTac(TACGenerator gen) {
        // 1. 为左右操作数生成TAC
        left.generateTac(gen);
        right.generateTac(gen);

        Address leftAddr = left.getPlace();
        Address rightAddr = right.getPlace();
        String leftType = left.getTypeString();
        String rightType = right.getTypeString();

        // 2. 创建一个新的临时变量来存储结果
        Address resultTemp = gen.newTemp();
        this.place = resultTemp; // 设置此表达式节点的 place

        // 3. 根据操作符字符串获取OpCode
        OpCode op = mapOperatorToOpCode(this.operator);
        if (op == null) {
            System.err.println("Syntax Error or Unimplemented operator: Unknown binary operator '" + this.operator + "' at line " + line + ", pos " + pos);
            // 可以设置一个错误标记或默认行为
            this.setTypeString("error_type_operator");
            // 也可以生成一个表示错误的特殊TAC，或者不生成
            gen.emitComment("Error: Unknown operator " + this.operator);
            return;
        }

        // 4. 发出二元运算指令
        gen.emit(new BinaryOpInstruction(
                resultTemp,
                leftAddr,  // 直接使用 Address 对象
                op,
                rightAddr  // 直接使用 Address 对象
        ));

        // 5. 设置此表达式的结果类型
        // 这是一个简化的类型推断，理想情况下应由语义分析阶段完成
        // 此处假设类型已在 left/right 节点上被正确设置
        String resultType = determineResultType(this.operator, leftType, rightType);
        this.setTypeString(resultType);
    }

    private OpCode mapOperatorToOpCode(String opStr) {
        switch (opStr) {
            // 算术运算符
            case "+": return OpCode.ADD;
            case "-": return OpCode.SUB;
            case "*": return OpCode.MUL;
            case "/": return OpCode.DIV;
            case "%": return OpCode.MOD; // 假设有 MOD 操作码
            // 比较运算符
            case "==": return OpCode.EQ;
            case "!=": return OpCode.NE;
            case "<":  return OpCode.LT;
            case "<=": return OpCode.LE;
            case ">":  return OpCode.GT;
            case ">=": return OpCode.GE;
            // 逻辑运算符
            case "&&": return OpCode.AND; // 逻辑与
            case "||": return OpCode.OR;  // 逻辑或
            // 其他可能的二元运算符 (位运算等)
            // case "&": return OpCode.BIT_AND;
            // case "|": return OpCode.BIT_OR;
            // case "^": return OpCode.BIT_XOR;
            default:
                return null; // 未知或未实现的运算符
        }
    }

    private String determineResultType(String opStr, String typeLeft, String typeRight) {
        // 简化版类型推断：
        // 规则1: 如果是比较运算符或逻辑运算符, 结果为 "bool"
        if (isComparisonOrLogical(opStr)) {
            return "bool"; // 或者你定义的布尔类型字符串
        }

        // 规则2: 算术运算 (假设只有 int 和 float)
        // (确保你的类型字符串是规范的，例如 "int", "float")
        if (typeLeft == null || typeRight == null) return "error_type_operand_null";
        if (typeLeft.equals("error") || typeRight.equals("error")) return "error_type_operand_error";


        if (typeLeft.equals("float") || typeRight.equals("float")) {
            // 如果任一操作数是 float，结果是 float (类型提升)
            // (这里需要检查另一个操作数是否可以参与浮点运算)
            if (typeLeft.equals("int") || typeLeft.equals("float") || typeRight.equals("int") || typeRight.equals("float")) {
                return "float";
            } else {
                // 例如 float + string 是不允许的
                System.err.println("Type Error: Incompatible types for operator '" + opStr + "': " + typeLeft + ", " + typeRight + " at line " + line);
                return "error_type_incompatible";
            }
        } else if (typeLeft.equals("int") && typeRight.equals("int")) {
            // 如果两个操作数都是 int，结果是 int
            return "int";
        } else {
            // 其他情况 (例如 int + bool) 是类型错误
            System.err.println("Type Error: Incompatible types for operator '" + opStr + "': " + typeLeft + ", " + typeRight + " at line " + line);
            return "error_type_incompatible";
        }
    }

    private boolean isComparisonOrLogical(String opStr) {
        switch (opStr) {
            case "==": case "!=":
            case "<":  case "<=":
            case ">":  case ">=":
            case "&&": case "||":
                return true;
            default:
                return false;
        }
    }


    @Override
    public String getNodeTypeString() {
        return "BinaryExpr(" + operator + ")";
    }

    public ExprNode getLeft() {
        return left;
    }

    public ExprNode getRight() {
        return right;
    }

    public String getOperator() {
        return operator;
    }
}