package src.compile.parser.tac.symboltable;

import javafx.util.Pair;

import java.util.*;

/**
 * @author sixteacher
 * @version 1.0
 * @description SymbolTableManager
 * @date 2025/5/21
 */


public class SymbolTableManager {
    private Stack<Map<String, SymbolEntry>> scopeStack;
    private int currentScopeLevel;
    private Stack<Integer> currentOffsetStack; // 每个作用域的下一个可用偏移量

    private List<SymbolEntry> allAddedSymbols;

    public SymbolTableManager() {
        this.scopeStack = new Stack<>();
        this.currentOffsetStack = new Stack<>();
        this.currentScopeLevel = -1;

        this.allAddedSymbols = new ArrayList<>();

        initializeGlobalScope();
    }

    public void initializeGlobalScope() {
        enterScope(); // 进入全局作用域 (level 0)
        // 例如:
        // SymbolEntry printFunc = new SymbolEntry("print", "function(string):void", SymbolEntry.Kind.FUNCTION);
        // addSymbol(printFunc); // 预定义函数通常不分配栈偏移量
    }

    public void enterScope() {
        currentScopeLevel++;
        scopeStack.push(new HashMap<>());
        currentOffsetStack.push(0); // 每个新作用域的偏移量从0开始 (或某个基址)
        System.out.println("进入作用域，级别: " + currentScopeLevel + ". 栈大小: " + scopeStack.size());
    }

    public void leaveScope() {
        if (!scopeStack.isEmpty()) {
            scopeStack.pop();
            currentOffsetStack.pop();
            currentScopeLevel--;
            System.out.println("离开作用域，新级别: " + currentScopeLevel + ". 栈大小: " + scopeStack.size());
        } else {
            System.err.println("错误: 尝试离开作用域时，作用域栈为空。");
        }
    }

    /**
     * 向当前作用域添加一个符号。
     * 此方法会为符号设置当前作用域级别，并根据符号类型（假设是变量或参数）分配偏移量。
     * @param entry 要添加的符号条目。
     * @return 如果成功添加返回 true，如果当前作用域已存在同名符号则返回 false。
     */
    public boolean addSymbol(SymbolEntry entry) {
        if (scopeStack.isEmpty()) {
            System.err.println("错误: 无法添加符号，作用域栈为空。");
            return false; // 或者抛出异常
        }
        Map<String, SymbolEntry> currentScope = scopeStack.peek();
        if (currentScope.containsKey(entry.getName())) {
            System.err.println("错误: 符号 '" + entry.getName() + "' 已在当前作用域 (级别 " + currentScopeLevel + ") 中定义。");
            return false;
        }

        // 1. 设置符号的作用域级别
        entry.setScopeLevel(this.currentScopeLevel);

        // ... existing code ...
        // 此处为了演示，我们为所有条目计算大小并分配偏移，
        // 你需要根据你的 `SymbolEntry` 的具体含义来调整。
        int sizeOfSymbolType = calculateSize(entry.getTypeString());
        // 只有当符号实际占用空间时才分配偏移量 (例如，void 类型大小可能为0)
        if (sizeOfSymbolType > 0) {
            // 并且该符号类型是需要在栈上分配空间的 (例如变量、参数)
            // 你可能需要一个更明确的方式来判断，比如 entry.isStackAllocated()
            int offset = allocateOffset(sizeOfSymbolType);
            entry.setOffset(offset); // 假设 SymbolEntry 有 setOffset 方法
            System.out.println("为符号 '" + entry.getName() + "' (类型: " + entry.getTypeString() + ") 分配偏移量: " + offset + ", 大小: " + sizeOfSymbolType + " 在作用域 " + this.currentScopeLevel);
        }


        currentScope.put(entry.getName(), entry);
        System.out.println("添加符号: " + entry.getName() + " 到作用域级别 " + currentScopeLevel);
        this.allAddedSymbols.add(entry); // Added: Record symbol for final printing
        return true;
    }

    /**
     * 查找一个符号。从当前作用域开始，逐级向外层作用域查找。
     * @param name 要查找的符号名称。
     * @return 如果找到则返回 SymbolEntry，否则返回 null。
     */
    public SymbolEntry lookupSymbol(String name) {
        for (int i = scopeStack.size() - 1; i >= 0; i--) {
            Map<String, SymbolEntry> scope = scopeStack.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        System.err.println("错误: 符号 '" + name + "' 在任何可访问的作用域中都未找到。");
        return null;
    }

    public int getCurrentScopeLevel() {
        return currentScopeLevel;
    }

    public int calculateSize(String typeString) {
        if (typeString == null || typeString.trim().isEmpty()) return 0; // 空类型或无效类型不占空间
        if (typeString.equalsIgnoreCase("void")) return 0; // void 类型不占空间

        if (typeString.equals("int") || typeString.equals("bool")) return 4;
        if (typeString.equals("float")) return 8;
        if (typeString.startsWith("array")) {
            try {
                String content = typeString.substring("array(".length(), typeString.lastIndexOf(')'));
                Pair<String, String> parsed = parseElementAndSize(content);
                String elementType = parsed.getKey();
                String sizeStr = parsed.getValue();

                int count = Integer.parseInt(sizeStr);
                return calculateSize(elementType) * count;
            } catch (Exception e) {
                System.err.println("警告: 解析数组类型时出错，无法计算大小: " + typeString);
                return 4;
            }
        }
        // 对于未识别的类型，可以返回一个默认大小或错误指示
        System.err.println("警告: 未知类型 '" + typeString + "'，使用默认大小 4。");
        return 4;
    }

    public int allocateOffset(int sizeInBytes) {
        if (currentOffsetStack.isEmpty()) {
            throw new IllegalStateException("偏移量栈为空，无法分配偏移量。");
        }
        if (sizeInBytes <= 0) { // 不为大小为0或负的类型分配偏移量
            return -1; //或根据你的约定返回一个特殊值
        }
        int currentBaseOffset = currentOffsetStack.pop();
        int allocatedOffset = currentBaseOffset;
        currentOffsetStack.push(currentBaseOffset + sizeInBytes);
        return allocatedOffset;
    }

    /**
     * 一个辅助方法，用于创建 SymbolEntry 并将其添加到当前作用域。
     * 注意：此方法中的 'scopeLevel' 参数主要用于信息目的或与外部源同步，
     * 符号实际添加到的作用域由 SymbolTableManager 的当前状态 (this.currentScopeLevel) 决定。
     * 'size' 参数如果提供且大于0，则直接使用；否则，会根据 typeNameString 计算。
     *
     * @param identifierName 符号名称
     * @param typeNameString 符号的类型字符串
     * @param kind           符号的种类 (例如 VARIABLE, PARAMETER, FUNCTION)
     * @param preCalculatedSize 预计算的大小 (如果 <= 0, 则会调用 calculateSize)
     * @return 创建并添加的 SymbolEntry，如果添加失败则返回 null。
     */
    public SymbolEntry addSymbol(String identifierName, String typeNameString, SymbolEntry.Kind kind, int preCalculatedSize) {
        // 确保 SymbolEntry 有一个构造函数类似 SymbolEntry(String name, String type, SymbolEntry.Kind kind)
        // 并且有 setOffset 和 setScopeLevel 方法。
        SymbolEntry entry = new SymbolEntry(identifierName, typeNameString, kind); // 假设的 SymbolEntry 构造

        // 大小计算逻辑
        int actualSize;
        if (preCalculatedSize > 0) {
            actualSize = preCalculatedSize;
        } else {
            actualSize = calculateSize(typeNameString);
        }
        // entry.setSize(actualSize); // 如果 SymbolEntry 需要存储其大小

        if (addSymbol(entry)) { // addSymbol(SymbolEntry) 会处理作用域级别和偏移量
            return entry;
        } else {
            return null; // 添加失败 (例如，重定义)
        }
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

    /**
     * Prints all symbols that have ever been added to the symbol table,
     * grouped by scope.
     */
    public void printAllSymbolsEverAdded() {
        if (allAddedSymbols.isEmpty()) {
            System.out.println("符号表中没有任何符号被记录。");
            return;
        }

        // Group symbols by scope level using a TreeMap to keep scopes sorted
        Map<Integer, List<SymbolEntry>> symbolsByScope = new TreeMap<>();
        for (SymbolEntry entry : allAddedSymbols) {
            // Assuming SymbolEntry has getScopeLevel()
            symbolsByScope.computeIfAbsent(entry.getScopeLevel(), k -> new ArrayList<>()).add(entry);
        }

        System.out.println("\n--- 全局符号表状态 ---");

        for (Map.Entry<Integer, List<SymbolEntry>> scopeEntry : symbolsByScope.entrySet()) {
            int scopeLevel = scopeEntry.getKey();
            List<SymbolEntry> symbolsInScope = scopeEntry.getValue();

            System.out.println("-------------------------------------");
            System.out.println("Scope: " + scopeLevel);

            for (SymbolEntry symbol : symbolsInScope) {
                // Assuming SymbolEntry has getName(), getTypeString(), getOffset()
                String typeString = symbol.getTypeString();

                // Using "Variable:" as per example. You might want to vary this based on symbol.getKind()
                System.out.print("Variable: " + symbol.getName() + ", Type: " + typeString);

                // Assuming getOffset() returns the calculated offset, or a value like -1 if not applicable.
                // The offset here is the relative offset calculated by SymbolTableManager.
                if (symbol.getOffset() >= 0) {
                    System.out.print(", Address: " + String.format("0x%X", symbol.getOffset()));
                }
                System.out.println();

                if (typeString != null && typeString.startsWith("array")) {
                    try {
                        // Extract content within "array(...)"
                        String content = typeString.substring("array(".length(), typeString.lastIndexOf(')'));
                        // Use the existing private helper method to parse element type and count
                        Pair<String, String> parsedArrayInfo = this.parseElementAndSize(content);
                        String elementType = parsedArrayInfo.getKey();
                        String countStr = parsedArrayInfo.getValue();

                        int elementCount = Integer.parseInt(countStr);
                        int elementSize = calculateSize(elementType); // calculateSize is public

                        System.out.println("Array Size: " + elementCount + ", Element Size: " + elementSize);
                    } catch (Exception e) {
                        // It's good practice to log the error or print a more informative message
                        System.err.println("警告: 在为符号 '" + symbol.getName() + "' 解析数组详细信息时出错: " + typeString + " (" + e.getMessage() + ")");
                    }
                }
            }
        }
        System.out.println("-------------------------------------");
        System.out.println("--- 符号表状态打印完毕 ---");
    }

}