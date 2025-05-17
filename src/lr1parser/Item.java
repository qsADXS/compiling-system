package src.lr1parser;

import java.util.Objects;

public class Item {
    public Grammar grammar;
    public int dotPos; // 点的位置
    public String lookahead; // 展望符

    public Item(Grammar grammar, int dotPos, String lookahead) {
        this.grammar = grammar;
        this.dotPos = dotPos;
        this.lookahead = lookahead;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return dotPos == item.dotPos &&
                grammar.equals(item.grammar) &&
                lookahead.equals(item.lookahead);
    }

    @Override
    public int hashCode() {
        return Objects.hash(grammar, dotPos, lookahead);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(grammar.lhs).append(" -> ");

        for (int i = 0; i < grammar.rhs.size(); i++) {
            if (i == dotPos) sb.append("•");
            sb.append(grammar.rhs.get(i)).append(" ");
        }
        if (dotPos == grammar.rhs.size()) sb.append("•");

        sb.append(", ").append(lookahead);
        return sb.toString();
    }

    public String getSymbolAfterDot() {
        if (dotPos < grammar.rhs.size()) {
            return grammar.rhs.get(dotPos);
        }
        return null;
    }
}