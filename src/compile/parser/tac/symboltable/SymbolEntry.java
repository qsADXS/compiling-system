package src.compile.parser.tac.symboltable;

import src.compile.parser.tac.Address.Address;
import src.compile.parser.tac.Address.NameAddress;

/**
 * @author sixteacher
 * @version 1.0
 * @description SymbolEntry
 * @date 2025/5/21
 */

public class SymbolEntry {
    private String name;
    private String type; // 简单起见，用字符串表示类型
    private int scopeLevel;
    private Address address;
    private int offset;
    private Kind kind;

    public enum Kind {
        VARIABLE,
        PARAMETER,
        FUNCTION,
        CONSTANT,
        TYPE_DEFINITION
    }

    // {{ NEW_CONSTRUCTOR }}
    /**
     * 推荐的构造函数，与 SymbolTableManager 配合使用。
     * scopeLevel 和 offset 将由 SymbolTableManager 在添加时设置。
     */
    public SymbolEntry(String name, String type, Kind kind) {
        this.name = name;
        this.type = type;
        this.kind = kind;
        this.scopeLevel = -1; // 初始默认值，会被 SymbolTableManager覆盖
        this.address = new NameAddress(name); // 对变量/参数来说是合理的默认值
        this.offset = -1;     // 初始默认值，会被 SymbolTableManager覆盖
    }

    // 保留现有构造函数，如果它们在其他地方被使用
    public SymbolEntry(String name, String type, int scopeLevel, Kind kind) {
        this.name = name;
        this.type = type;
        this.scopeLevel = scopeLevel; // 直接设置
        this.kind = kind;
        this.address = new NameAddress(name);
        this.offset = 0; // 或 -1，根据你的约定
    }

    public SymbolEntry(String name, String type, int scopeLevel, Kind kind, Address address) {
        this.name = name;
        this.type = type;
        this.scopeLevel = scopeLevel; // 直接设置
        this.kind = kind;
        this.address = address;
        this.offset = 0; // 或 -1
    }


    public String getName() {
        return name;
    }

    public String getTypeString() {
        return type;
    }

    public int getScopeLevel() {
        return scopeLevel;
    }

    // {{ ADDED_METHOD }}
    /**
     * 设置符号的作用域级别。
     * 通常由 SymbolTableManager 在将符号添加到特定作用域时调用。
     */
    public void setScopeLevel(int scopeLevel) {
        this.scopeLevel = scopeLevel;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public Kind getKind() {
        return kind;
    }

    @Override
    public String toString() {
        return "SymbolEntry{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", scopeLevel=" + scopeLevel +
                ", address=" + (address != null ? address.toString() : "null") +
                ", offset=" + offset +
                ", kind=" + kind +
                '}';
    }
}