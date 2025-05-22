package src.compile.parser.tac.Address;

/**
 * @author sixteacher
 * @version 1.0
 * @description ConstantAddress
 * @date 2025/5/19
 */

/**
 * 表示一个持有泛型 T 常量值的地址。
 * 此类实现了 Address 接口。
 *
 * @param <T> 此地址将持有的常量值的值。
 */
import java.util.Objects;

public class ConstantAddress<T> implements Address {
    public final T value;

    public ConstantAddress(T value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConstantAddress<?> that = (ConstantAddress<?>) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}