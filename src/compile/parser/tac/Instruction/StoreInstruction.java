package src.compile.parser.tac.Instruction;

import src.compile.parser.tac.Address.Address;


/**
 * @author sixteacher
 * @version 1.0
 * @description StoreInstruction
 * @date 2025/5/21
 */

/**
 * 存储指令类 (StoreInstruction)。
 * 该类代表一条将特定值存储到指定内存地址的指令。
 * 这通常用于模拟或执行对内存的写入操作，比如将计算结果存入变量或数组的特定位置。
 * 构造函数通过接收目标内存地址和要存储的值来创建一个存储指令实例。
 * {@code toString} 方法返回该存储指令的字符串表示，
 * 通常格式为 {@code (store, 内存地址, 要存储的值, _)}
 * (下划线通常表示该位置的参数在此指令中不使用或为默认值)。
 */
public class StoreInstruction implements Instruction {
    private Address memoryAddress; // 目标内存地址 (例如，数组元素的计算后地址)
    private Address valueToStore;  // 要存储到该地址的值

    /**
     * 构造一个 Store 指令。
     * 表示将 valueToStore 的内容存储到 memoryAddress 指向的内存位置。
     * TAC 形式通常为: *memoryAddress = valueToStore
     *
     * @param memoryAddress  表示目标内存地址的 Address 对象。
     *                       在数组赋值 a[i] = x 中，这通常是一个临时变量，
     *                       其值为数组 a 的基地址加上偏移量 i*element_size。
     * @param valueToStore   表示要存储的值的 Address 对象。
     *                       在 a[i] = x; 中，这是 x 的地址 (可能是变量名或临时变量)。
     */
    public StoreInstruction(Address memoryAddress, Address valueToStore) {
        this.memoryAddress = memoryAddress;
        this.valueToStore = valueToStore;
    }

    public Address getMemoryAddress() {
        return memoryAddress;
    }

    public Address getValueToStore() {
        return valueToStore;
    }

    /**
     * 生成此 Store 指令的三地址码表示。
     * 格式: *memoryAddress = valueToStore
     * 例如: *t1 = t2
     */
    /*
    @Override
    public String toString() {
        return "*" + memoryAddress.toString() + " = " + valueToStore.toString();
    }
     */

    @Override
    public String toString() {
        return "(store, " + memoryAddress + ", " + valueToStore + ", _)";
    }

}