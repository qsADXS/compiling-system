package src.compile.parser.ast;

import javafx.util.Pair;
import src.compile.parser.tac.Address.Address;
import src.compile.parser.tac.Address.ConstantAddress;
import src.compile.parser.tac.Address.NameAddress;
import src.compile.parser.tac.Instruction.BinaryOpInstruction;
import src.compile.parser.tac.OpCode;
import src.compile.parser.tac.TACGenerator;
import src.compile.parser.tac.symboltable.SymbolEntry;
import src.compile.parser.tac.symboltable.SymbolTableManager;

/**
 * @author sixteacher
 * @version 1.0
 * @description ArrayAccessNode
 * @date 2025/5/19
 */

/**
 * 代表数组元素访问的节点 (例如 `arr[index]`)。它继承自 LocNode。
 * 其 'place' 属性在 `generateTac()` 中会被设置为计算得到的数组元素的实际地址 (通常是一个新的临时变量)。
 */
public class ArrayAccessNode extends LocNode { // 继承自 LocNode
    public final LocNode base;         // 数组的基址 (可以是 IdNode 或另一个 ArrayAccessNode)
    public final ExprNode index;      // 索引表达式

    // 这两个字段可以保留，用于调试或特定情况，但核心信息在 base.getPlace() 和 index.getPlace()
    public String basePlaceCache;    // TAC: 基址的 place (缓存)
    public String indexPlaceCache;   // TAC: 索引值的 place (缓存)

    // 'place' 继承自 ASTNode
    // 'typeString' 将通过 ExprNode 的 setTypeString/getTypeString 访问

    public ArrayAccessNode(LocNode base, ExprNode index, int line, int pos) {
        super(line, pos); // 调用 LocNode 的 protected 构造函数 LocNode(line, pos)，idName 为 null
        this.base = base;
        this.index = index;
    }

    @Override
    public void generateTac(TACGenerator gen) {
        base.generateTac(gen);
        index.generateTac(gen);

        this.basePlaceCache = base.getPlace().toString();
        this.indexPlaceCache = index.getPlace().toString();

        SymbolTableManager stm = gen.getSymbolTableManager();

        String baseType = base.getTypeString();

        if (baseType == null || !baseType.startsWith("array(")) {
            System.err.println("Semantic Error (ArrayAccessNode): Base '" + base.getPlace() +
                    "' is not of array type. Found type: " + baseType +
                    " at line " + this.line + ", pos " + this.pos);
            this.place = new NameAddress("ERROR_ARRAY_ADDR_TYPE");
            this.setTypeString("error");
            return;
        }

        // 解析 elementType 和当前维度大小（支持嵌套数组）
        String innerContent = baseType.substring("array(".length(), baseType.lastIndexOf(')'));
        Pair<String, String> typeAndSize = parseElementAndSize(innerContent);
        String elementType = typeAndSize.getKey();
        String sizeStr = typeAndSize.getValue();

        // 设置下一级的类型（递归）
        this.setTypeString(elementType);

        int size;
        try {
            size = Integer.parseInt(sizeStr);
        } catch (NumberFormatException e) {
            System.err.println("Semantic Error (ArrayAccessNode): Invalid array size: '" + sizeStr +
                    "' for base type: " + baseType + " at line " + this.line + ", pos " + this.pos);
            this.place = new NameAddress("ERROR_ARRAY_SIZE");
            this.setTypeString("error");
            return;
        }

        int elementSize = stm.calculateSize(elementType);

        // 当前层的偏移量 = index * 每个元素大小
        Address offset = gen.newTemp();
        gen.emit(new BinaryOpInstruction(
                offset,
                new NameAddress(indexPlaceCache),
                OpCode.MUL,
                new ConstantAddress<>(elementSize)
        ));

        // 加到基址上
        Address elementAddress = gen.newTemp();
        gen.emit(new BinaryOpInstruction(
                elementAddress,
                new NameAddress(basePlaceCache),
                OpCode.ADD,
                offset
        ));

        // 保存地址结果
        this.place = elementAddress;
    }
    @Override
    public String getNodeTypeString() {
        return "ArrayAccess";
    }

    public LocNode getBase() {
        return base;
    }

    public ExprNode getIndex() {
        return index;
    }

    // 这些辅助 getter 方法可以使用缓存的 place 值
    public String getIndexAccessString() {
        return indexPlaceCache != null ? indexPlaceCache : (index.getPlace() != null ? index.getPlace().toString() : "idx_unknown");
    }

    @Override
    public String getBaseAccessString() { // LocNode 也定义了此方法，这里覆盖它
        return basePlaceCache != null ? basePlaceCache : (base.getPlace() != null ? base.getPlace().toString() : "base_unknown");
    }

    // 新增方法：递归提取最内层元素类型
    private Pair<String, String> parseElementAndSize(String innerContent) {
        int depth = 0;
        int splitPos = -1;
        for (int i = 0; i < innerContent.length(); i++) {
            char c = innerContent.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == ',' && depth == 0) {
                splitPos = i;
                break;
            }
        }
        if (splitPos == -1) return new Pair<>(innerContent.trim(), "1"); // no size info
        String elementType = innerContent.substring(0, splitPos).trim();
        String sizeStr = innerContent.substring(splitPos + 1).trim();
        return new Pair<>(elementType, sizeStr);
    }
}