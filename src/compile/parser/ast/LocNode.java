package src.compile.parser.ast;

import src.compile.parser.tac.Address.Address;
import src.compile.parser.tac.Address.NameAddress;
import src.compile.parser.tac.TACGenerator;
import src.compile.parser.tac.symboltable.SymbolEntry;
import src.compile.parser.tac.symboltable.SymbolTableManager;

/**
 * @author sixteacher
 * @version 1.0
 * @description LocNode
 * @date 2025/5/19
 */

/**
 * 代表一个左值（L-value）位置，例如变量标识符或数组成员访问。它也是一种表达式 (ExprNode)。
 * 其 'place' 属性：
 * - 对于简单变量ID (如 `x`)，在 `createASTNode` 中被设置为 `NameAddress("x")`。
 * - 对于数组成员访问 (如 `a[i]`)，其 `place` 在 `ArrayAccessNode.generateTac()` 中被设置为计算得到的元素实际地址（通常是一个临时变量）。
 * 当作为右值使用时 (即读取其值)，`generateTac` 通常会额外生成一条指令将 `loc.place` 的内容加载到新的临时变量中 (但这更多是使用 `LocNode` 的上下文决定的)。
 * 当作为左值使用时 (如赋值语句的左侧)，`loc.place` 直接用作目标地址。
 */
public class LocNode extends ExprNode {
    public final String idName;

    public LocNode(String idName, int line, int pos) {
        super(line, pos);
        this.idName = idName;
    }

    protected LocNode(int line, int pos) {
        super(line, pos);
        this.idName = null;
    }

    @Override
    public void generateTac(TACGenerator gen) {
        if (this.idName != null) {
            SymbolTableManager stm = gen.getSymbolTableManager();
            SymbolEntry entry = stm.lookupSymbol(this.idName); // 假设方法名

            if (entry != null) {

                String tacVariableName = entry.getName() + "_scope" + entry.getScopeLevel();
                // 假设 entry.getAddress() 返回 Address 类型
                this.place = new NameAddress(tacVariableName); // 使用这个唯一的名字
                this.setTypeString(entry.getTypeString()); // setTypeString 是 ExprNode 的方法
            } else {
                System.err.println("Semantic Error: Undefined symbol '" + this.idName + "' at line " + line + ", pos " + pos);
                this.place = new NameAddress("UNDEFINED_VAR_" + this.idName); // NameAddress 是 Address
                this.setTypeString("error_type");
            }
        }
        // else: 子类 (如 ArrayAccessNode) 负责设置 place 和 typeString
    }

    @Override
    public String getNodeTypeString() {
        return idName != null ? "LocNode(ID: " + idName + ")" : "LocNode(Complex)";
    }

    public String getIdName() {
        return idName;
    }

    public String getBaseAccessString() {
        // {{ 修改：获取 place 的字符串表示形式进行操作 }}
        Address currentAddress = getPlace(); // 从 ExprNode (最终从 ASTNode) 获取 Address
        String currentPlaceString = null;
        if (currentAddress != null) {
            currentPlaceString = currentAddress.toString();
        }

        if (currentPlaceString != null && currentPlaceString.contains("[")) {
            return currentPlaceString.substring(0, currentPlaceString.indexOf("["));
        }
        // 如果没有找到 "[", 或者 currentPlaceString 为 null,
        // 则返回 currentPlaceString (如果非null)，否则返回 idName (如果非null)，最后是备用字符串。
        return currentPlaceString != null ? currentPlaceString : (idName != null ? idName : "unknown_loc_base");
    }

    // getPlace() 和 getTypeString() 从 ExprNode 继承
    // setTypeString(String) 从 ExprNode 继承
}