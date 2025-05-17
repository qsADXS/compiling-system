package src.lr1parser;

import java.util.List;
import java.util.Objects;

public class Grammar {
    public String lhs; // 左部
    public List<String> rhs; // 右部

    public Grammar(String lhs, List<String> rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public String toString() {
        return lhs + " -> " + String.join(" ", rhs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Grammar grammar = (Grammar) o;
        return lhs.equals(grammar.lhs) && rhs.equals(grammar.rhs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lhs, rhs);
    }
}