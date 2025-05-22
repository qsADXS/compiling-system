package src.compile.parser.tac;

import src.compile.parser.tac.Address.Address;
import src.compile.parser.tac.Address.LabelAddress;
import src.compile.parser.tac.Address.NameAddress;
import src.compile.parser.tac.Instruction.CommentInstruction;
import src.compile.parser.tac.Instruction.Instruction;
import src.compile.parser.tac.symboltable.SymbolTableManager;

import java.util.List;
import java.util.Stack;

/**
 * @author sixteacher
 * @version 1.0
 * @description TACGenerator
 * @date 2025/5/19
 */


public class TACGenerator {
    private int tempCount = 0;
    private int labelCount = 0;
    private List<Instruction> instructionsList;

    // {{ MODIFIED: 移除了 continueLabelStack 因为不支持 'continue' }}
    private Stack<LabelAddress> breakLabelStack = new Stack<>();
    // private Stack<LabelAddress> continueLabelStack = new Stack<>(); // 已移除

    private SymbolTableManager symbolTableManager;

    public TACGenerator(List<Instruction> instructionsList, SymbolTableManager manager) {
        this.instructionsList = instructionsList;
        this.symbolTableManager = manager;
    }

    public TACGenerator(List<Instruction> instructionsList) {
        this(instructionsList, new SymbolTableManager());
    }

    public void setInstructionList(List<Instruction> list) {
        this.instructionsList = list;
    }

    public NameAddress newTemp() {
        return new NameAddress("t" + tempCount++);
    }

    // 原有的 newLabel 方法，创建不带描述的标签
    public LabelAddress newLabel() {
        return new LabelAddress("L" + labelCount++); // 调用 LabelAddress(String label)
    }

    // 新增的 newLabel 方法，创建带描述的标签
    public LabelAddress newLabel(String description) {
        return new LabelAddress("L" + labelCount++, description); // 调用 LabelAddress(String label, String description)
    }
    public void emit(Instruction instruction) {
        if (this.instructionsList == null) {
            throw new IllegalStateException("Instruction list not set in TACGenerator");
        }
        this.instructionsList.add(instruction);
    }

    public void emitErrorComment(String errorMessage) {
        // 为错误注释添加一个明确的前缀，使其易于识别
        String formattedErrorMessage = "ERROR: " + errorMessage;
        emit(new CommentInstruction(formattedErrorMessage));
    }


    /**
     * 将新循环的结束标签（用于 break）推入其栈中。
     * 在 AST 处理中进入循环结构时调用。
     * @param breakLabel 循环结束后的 LabelAddress 标签（用于 break）。
     */
    // {{ MODIFIED: 参数列表更改为仅包含 breakLabel }}
    public void pushLoopContext(LabelAddress breakLabel) {
        if (breakLabel == null) {
            throw new IllegalArgumentException("Loop break label cannot be null.");
        }
        // this.continueLabelStack.push(continueLabel); // 已移除
        this.breakLabelStack.push(breakLabel);
    }

    /**
     * 从栈中弹出当前循环的 break 标签。
     * 在 AST 处理中退出循环结构时调用。
     */
    // {{ MODIFIED: 仅弹出 breakLabelStack }}
    public void popLoopContext() {
        // if (!continueLabelStack.isEmpty()) { // 已移除
        //     this.continueLabelStack.pop();
        // } else {
        //     System.err.println("Warning: Popped from empty continueLabelStack.");
        // }
        if (!breakLabelStack.isEmpty()) {
            this.breakLabelStack.pop();
        } else {
            System.err.println("Warning: Popped from empty breakLabelStack.");
        }
    }

    /**
     * 获取当前循环上下文中 'break' 语句的目标标签（即循环的出口标签）。
     * @return 用于 break 跳转的 LabelAddress。
     * @throws IllegalStateException 如果当前不在循环结构内。
     */
    public LabelAddress getLoopExitLabel() {
        if (breakLabelStack.isEmpty()) {
            throw new IllegalStateException("No enclosing loop context for break statement (getLoopExitLabel called).");
        }
        return breakLabelStack.peek();
    }




    public SymbolTableManager getSymbolTableManager() {
        return symbolTableManager;
    }
    // ... existing code ...
    public void setSymbolTableManager(SymbolTableManager symbolTableManager) {
        this.symbolTableManager = symbolTableManager;
    }

    public void emitComment(String commentText) {
        // 创建一个新的 CommentInstruction 对象
        CommentInstruction commentInstruction = new CommentInstruction(commentText);
        // 使用通用的 emit 方法将其添加到指令列表
        emit(commentInstruction);
    }



    // {{ REMOVED: The empty getLoopExitLabel() method is no longer needed as getCurrentBreakLabel was adapted }}
}
