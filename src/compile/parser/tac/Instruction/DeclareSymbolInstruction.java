package src.compile.parser.tac.Instruction;

/**
 * @author sixteacher
 * @version 1.0
 * @description DeclareSymbolInstruction
 * @date 2025/5/21
 */

/**
 * 表示一条声明符号的指令。
 * 用于在特定作用域内声明一个具有名称和类型的符号（例如变量）。
 */
public class DeclareSymbolInstruction implements Instruction {
    private String symbolName;
    private String symbolType; // 或者直接引用一个 Type 对象/Symbol 对象
    private int sourceLine;
    // 可以考虑直接存储 Symbol 对象，如果它包含了所有需要的信息
    // private Symbol symbol;

    public DeclareSymbolInstruction(String symbolName, String symbolType, int sourceLine) {
        this.symbolName = symbolName;
        this.symbolType = symbolType;
        this.sourceLine = sourceLine;
    }

    // public DeclareSymbolInstruction(Symbol symbol, int sourceLine) {
    //     this.symbol = symbol;
    //     this.sourceLine = sourceLine;
    // }


    public String getSymbolName() {
        return symbolName;
        // return symbol != null ? symbol.getName() : null;
    }

    public String getSymbolType() {
        return symbolType;
        // return symbol != null ? symbol.getType().toString() : null;
    }

    public int getSourceLine() {
        return sourceLine;
    }

    @Override
    public String toString() {
        // return "DECLARE " + getSymbolName() + " : " + getSymbolType() + " (Line: " + sourceLine + ")";
        return String.format("DECLARE %s : %s (Line: %d)", symbolName, symbolType, sourceLine);
    }
}