package src.compile.parser.tac;

public enum OpCode {
    // --- 算术运算 ---
    ADD("+"),
    SUB("-"),
    MUL("*"),
    DIV("/"),
    MOD("%"),
    NEG("-"), // 一元取负

    // --- 逻辑运算 ---
    AND("&&"),
    OR("||"),
    NOT("!"), // 一元逻辑非

    // --- 位运算 (如果需要) ---
    BIT_AND("&"),
    BIT_OR("|"),
    BIT_XOR("^"),
    BIT_NOT("~"), // 一元按位取反
    LSHIFT("<<"),
    RSHIFT(">>"),

    // --- 数据移动/赋值 ---
    COPY("="), // 用于赋值指令的符号
    LOAD("*"),  // 用于表示从内存加载的符号 (例如 t1 = *addr)
    STORE("*"), // 用于表示存储到内存的符号 (例如 *addr = t1) - 注意这里的符号用法

    // --- 比较运算 ---
    EQ("=="),
    NE("!="),
    LT("<"),
    LE("<="),
    GT(">"),
    GE(">="),

    // --- 控制流 (无条件跳转) ---
    GOTO("goto"),

    // --- 控制流 (条件跳转) ---
    IF_TRUE_GOTO("if True_goto"), // 或者 "if_goto" 如果条件通常是隐含的
    IF_FALSE_GOTO("if False_goto"), // 或者 "ifFalse_goto"

    // --- 函数调用 ---
    PARAM("param"),
    CALL("call"),
    RETURN("return"),

    // --- 标签和注释 ---
    LABEL(":"), // 标签通常以冒号结尾，但 OpCode 本身可能只是 LABEL
    COMMENT("#"), // 注释符号，例如 '#' 或 '//'

    // --- 其他/特殊操作 ---
    NO_OP("nop"),
    MALLOC("malloc"),
    FREE("free"),

    // --- 类型转换 (如果需要显式指令) ---
    INT_TO_FLOAT("(float)"),
    FLOAT_TO_INT("(int)"),

    // --- 数组/指针特定的地址计算 ---
    ADDR_ADD("+"), // 地址加法，符号与普通加法相同
    LOAD_INDEXED("[]"), // 表示数组访问的符号 (高级)
    STORE_INDEXED("[]="); // 表示数组存储的符号 (高级)

    // ---- 枚举字段 ----
    private final String symbol; // 用于存储操作符的符号表示

    // ---- 构造函数 ----
    /**
     * 枚举的私有构造函数。
     * @param symbol 该操作码对应的符号表示。
     */
    OpCode(String symbol) {
        this.symbol = symbol;
    }

    // ---- 公共方法 ----
    /**
     * 获取操作码的符号表示。
     * @return 操作符的字符串符号。
     */
    public String getSymbol() {
        return this.symbol;
    }

    /**
     * （可选）覆盖 toString() 方法，使其默认返回符号。
     * 如果不覆盖，OpCode.ADD.toString() 将返回 "ADD"。
     * 覆盖后，OpCode.ADD.toString() 将返回 "+"。
     * 根据你的使用场景决定是否需要这样做。
     */
    @Override
    public String toString() {
        return this.symbol;
    }
}