package src.compile.parser.ast;

import src.compile.parser.tac.Instruction.DeclareSymbolInstruction;
import src.compile.parser.tac.TACGenerator;
import src.compile.parser.tac.symboltable.SymbolEntry;
import src.compile.parser.tac.symboltable.SymbolTableManager;

/**
 * @author sixteacher
 * @version 1.0
 * @description DeclNode
 * @date 2025/5/19
 */

/**
 * 代表一个单独的变量声明。
 * 它包含变量的类型 (TypeNode) 和标识符名称。
 */
public class DeclNode extends ASTNode {
    public final TypeNode type;
    public final String identifierName;
    // 如果支持初始化: public final ExprNode initializer;

    public DeclNode(TypeNode type, String identifierName, /* ExprNode initializer, */ int line, int pos) {
        super(line, pos);
        this.type = type;
        this.identifierName = identifierName;
        // this.initializer = initializer;
    }

    @Override
    public void generateTac(TACGenerator gen) {
        SymbolTableManager stm = gen.getSymbolTableManager();

        // 1. 获取类型名称和大小
        String typeNameString = type.getTypeName();
        int size = stm.calculateSize(typeNameString);

        if (size <= 0 && !typeNameString.equalsIgnoreCase("void")) { // void 类型大小为0是正常的，不应报错
            System.err.println("语义错误: 标识符 '" + identifierName + "' 的类型 '" + typeNameString +
                    "' 未知或无效 (大小 <= 0) 在行 " + line + ", 位置 " + pos);
            // 标记错误，可能后续不再处理此变量
            return;
        }

        // 2. 将变量添加到符号表
        //    我们假设 DeclNode 主要用于声明局部变量。
        //    如果 DeclNode 也用于声明函数参数，那么 Kind 可能需要根据上下文调整。
        // {{ EDIT_1_MODIFICATION }}
        // 旧的调用方式:
        // SymbolEntry entry = stm.addSymbol(identifierName, typeNameString, size, stm.getCurrentScopeLevel());
        // 新的调用方式:
        SymbolEntry entry = stm.addSymbol(identifierName, typeNameString, SymbolEntry.Kind.VARIABLE, size);

        if (entry == null) {
            // 添加失败，通常意味着重复定义。
            // stm.addSymbol 内部应该已经报告了具体错误信息。
            System.err.println("错误: 添加变量 '" + identifierName + "' 到符号表失败 (可能重复定义) 在行 " + line + ", 位置 " + pos);
            return;
        }
        // 此时，entry 已经被 SymbolTableManager 设置了正确的作用域级别和偏移量（如果适用）

        // 对于简单的局部变量声明 (没有初始化)，通常不直接生成可执行的 TAC 指令。
        // 栈空间分配在函数入口处理。

        // 3. 如果有初始化表达式 (当前类结构没有，但为未来考虑):
        // if (initializer != null) {
        //     initializer.generateTac(gen);
        //     String initValuePlace = initializer.getPlace();
        //     // entry.getAddress() 应该返回变量的 NameAddress 或类似的
        //     gen.emit(new CopyInstruction(entry.getAddress(), new NameAddress(initValuePlace)));
        // }

        // DeclNode 本身通常不设置 this.place，因为它不是一个表达式。
        gen.emit(new DeclareSymbolInstruction(this.identifierName, typeNameString, this.line));
    }

    @Override
    public String getNodeTypeString() {
        return "DeclNode(" + identifierName + " : " + (type != null ? type.getTypeName() : "null_type") + ")";
    }

    public TypeNode getType() {
        return type;
    }

    public String getIdentifierName() {
        return identifierName;
    }
}