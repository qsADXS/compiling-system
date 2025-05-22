package src.compile.parser.tac.Address;

import java.util.Objects;

/**
 * @author sixteacher
 * @version 1.0
 * @description NameAddress
 * @date 2025/5/19
 */


/**
 * 表示由名称字符串标识的地址。
 * 此类实现了 Address 接口。
 */
public class NameAddress implements Address {
    public final String name;

    public NameAddress(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NameAddress that = (NameAddress) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}