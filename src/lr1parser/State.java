package src.lr1parser;

import java.util.Set;
import java.util.Objects;

public class State {
    public int id;
    public Set<Item> items;

    public State(int id, Set<Item> items) {
        this.id = id;
        this.items = items;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        State state = (State) o;
        return items.equals(state.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(items);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("State ").append(id).append(":\n");
        for (Item item : items) {
            sb.append(item.toString()).append("\n");
        }
        return sb.toString();
    }
}