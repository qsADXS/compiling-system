package src.compile.parser.ast;


import src.compile.parser.tac.Address.Address;
import src.compile.parser.tac.Address.NameAddress;
import src.compile.parser.tac.Instruction.AssignmentInstruction;
import src.compile.parser.tac.Instruction.StoreInstruction;
import src.compile.parser.tac.TACGenerator;

/**
 * @author sixteacher
 * @version 1.0
 * @description AssignStmtNode
 * @date 2025/5/19
 */

/**
 * 代表赋值语句节点 (例如 `x = y + 1;`)。
 * 包含一个左值位置 (LocNode) 和一个表达式 (ExprNode)。
 */
public class AssignStmtNode extends StmtNode {
    public final LocNode location;
    public final ExprNode expression;

    public AssignStmtNode(LocNode location, ExprNode expression, int line, int pos) {
        super(line, pos);
        this.location = location;
        this.expression = expression;
    }

    @Override
    public void generateTac(TACGenerator gen) {
        // 1. 为右侧表达式生成TAC
        expression.generateTac(gen);
        Address exprAddr = expression.getPlace();
        String exprType = expression.getTypeString(); // 获取表达式类型，用于可能的类型检查

        // 2. 为左侧位置生成TAC
        //    对于 IdNode, location.generateTac() 会设置 location.place 为变量名
        //    对于 ArrayAccessNode, location.generateTac() 会设置 location.place 为元素的地址
        location.generateTac(gen);
        Address locAddr = location.getPlace();
        String locType = location.getTypeString(); // 获取位置的类型

        // 可选：进行运行时之前的类型兼容性检查（通常在语义分析阶段做得更彻底）
        // if (!gen.getSymbolTableManager().areTypesCompatible(locType, exprType)) {
        //     System.err.println("Type mismatch in assignment at line " + line + ": Cannot assign " + exprType + " to " + locType);
        //     // 可能需要一种方式来停止编译或标记错误
        // }

        // 3. 发出赋值指令
        if (location instanceof ArrayAccessNode) {
            // 如果 location 是数组访问，locPlace 是地址，exprPlace 是值
            // 生成： STORE locPlace, exprPlace  (即 *locPlace = exprPlace)
            gen.emit(new StoreInstruction(
                    locAddr,      // 目标地址 (来自 ArrayAccessNode.place)
                    exprAddr       // 要存储的值
            ));
        } else {
            // 假设是简单变量 (例如 IdNode，或者其他 LocNode 其 place 代表直接可赋值的目标)
            // 生成： COPY locPlace, exprPlace (即 locPlace = exprPlace)
            gen.emit(new AssignmentInstruction(
                    locAddr,      // 目标变量
                    exprAddr      // 源值
            ));
        }
        // AssignStmtNode 本身没有 place 或 typeString
    }

    @Override
    public String getNodeTypeString() {
        return "AssignStmt";
    }

    public LocNode getLocation() {
        return location;
    }

    public ExprNode getExpression() {
        return expression;
    }
}