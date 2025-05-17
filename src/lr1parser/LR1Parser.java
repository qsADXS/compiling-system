package src.lr1parser;

import java.util.*;

public class LR1Parser {
    private List<Grammar> grammars;
    private Set<String> terminals;
    private Set<String> nonTerminals;
    private String startSymbol;
    private List<State> states;
    private Map<Integer, Map<String, String>> actionTable;
    private Map<Integer, Map<String, Integer>> gotoTable;

    public LR1Parser(List<Grammar> grammars, String startSymbol) {
        this.grammars = new ArrayList<>(grammars);
        this.startSymbol = startSymbol;
        this.terminals = new HashSet<>();
        this.nonTerminals = new HashSet<>();
        this.states = new ArrayList<>();
        this.actionTable = new HashMap<>();
        this.gotoTable = new HashMap<>();

        computeSymbolSets();
        buildLR1Table();
    }

    private void computeSymbolSets() {
        // 计算终结符和非终结符
        for (Grammar g : grammars) {
            nonTerminals.add(g.lhs);
            for (String s : g.rhs) {
                if (!s.equals("ε")) {
                    if (s.startsWith("'") && s.endsWith("'")) {
                        terminals.add(s);
                    } else if (!grammars.stream().anyMatch(gr -> gr.lhs.equals(s))) {
                        terminals.add(s);
                    }
                }
            }
        }
        terminals.add("$"); // 结束符号
    }

    private Set<Item> closure(Set<Item> items) {
        Set<Item> closureSet = new HashSet<>(items);
        boolean changed;
        do {
            changed = false;
            Set<Item> newItems = new HashSet<>();

            for (Item item : closureSet) {
                String symbol = item.getSymbolAfterDot();
                if (symbol != null && nonTerminals.contains(symbol)) {
                    List<String> beta = item.grammar.rhs.subList(item.dotPos + 1, item.grammar.rhs.size());
                    Set<String> firstBeta = new HashSet<>(first(beta));
                    if (beta.isEmpty() || firstBeta.contains("ε")) {
                        firstBeta.add(item.lookahead);
                    }
                    firstBeta.remove("ε");

                    for (Grammar g : grammars) {
                        if (g.lhs.equals(symbol)) {
                            for (String lookahead : firstBeta) {
                                Item newItem = new Item(g, 0, lookahead);
                                if (!closureSet.contains(newItem) && !newItems.contains(newItem)) {
                                    newItems.add(newItem);
                                    changed = true;
                                }
                            }
                        }
                    }
                }
            }
            closureSet.addAll(newItems);
        } while (changed);
        return closureSet;
    }

    private Set<Item> goTo(Set<Item> items, String symbol) {
        Set<Item> newItems = new HashSet<>();
        for (Item item : items) {
            String symbolAfterDot = item.getSymbolAfterDot();
            if (symbol.equals(symbolAfterDot)) {
                newItems.add(new Item(item.grammar, item.dotPos + 1, item.lookahead));
            }
        }
        return closure(newItems);
    }

    private Set<String> first(List<String> symbols) {
        Set<String> firstSet = new HashSet<>();
        if (symbols.isEmpty()) {
            firstSet.add("ε");
            return firstSet;
        }

        String firstSymbol = symbols.get(0);
        if (terminals.contains(firstSymbol)) {
            firstSet.add(firstSymbol);
        } else if (nonTerminals.contains(firstSymbol)) {
            for (Grammar g : grammars) {
                if (g.lhs.equals(firstSymbol)) {
                    firstSet.addAll(first(g.rhs));
                }
            }
        }

        if (firstSet.contains("ε") && symbols.size() > 1) {
            firstSet.remove("ε");
            firstSet.addAll(first(symbols.subList(1, symbols.size())));
        }

        return firstSet;
    }

    private void buildLR1Table() {
        // 初始项目
        Grammar augmentedGrammar = new Grammar("S'", Arrays.asList(startSymbol));
        grammars.add(0, augmentedGrammar);
        Item initialItem = new Item(augmentedGrammar, 0, "$");
        Set<Item> initialClosure = closure(new HashSet<>(Collections.singletonList(initialItem)));
        State initialState = new State(0, initialClosure);
        states.add(initialState);

        // 构建规范LR(1)项集族
        Queue<State> queue = new LinkedList<>();
        queue.add(initialState);

        while (!queue.isEmpty()) {
            State currentState = queue.poll();
            Set<String> symbols = new HashSet<>();
            symbols.addAll(terminals);
            symbols.addAll(nonTerminals);

            for (String symbol : symbols) {
                Set<Item> newItems = goTo(currentState.items, symbol);
                if (!newItems.isEmpty()) {
                    State newState = new State(states.size(), newItems);

                    int existingStateIndex = states.indexOf(newState);
                    if (existingStateIndex == -1) {
                        states.add(newState);
                        queue.add(newState);
                    } else {
                        newState = states.get(existingStateIndex);
                    }

                    // 更新分析表
                    if (terminals.contains(symbol)) {
                        actionTable.computeIfAbsent(currentState.id, k -> new HashMap<>())
                                .put(symbol, "s" + newState.id);
                    } else if (nonTerminals.contains(symbol)) {
                        gotoTable.computeIfAbsent(currentState.id, k -> new HashMap<>())
                                .put(symbol, newState.id);
                    }
                }
            }
        }

        // 添加归约和接受动作
        for (State state : states) {
            for (Item item : state.items) {
                if (item.getSymbolAfterDot() == null) {
                    if (item.grammar.lhs.equals("S'")) {
                        actionTable.computeIfAbsent(state.id, k -> new HashMap<>())
                                .put("$", "acc");
                    } else {
                        int grammarIndex = grammars.indexOf(item.grammar);
                        if (grammarIndex != -1) {
                            actionTable.computeIfAbsent(state.id, k -> new HashMap<>())
                                    .put(item.lookahead, "r" + grammarIndex);
                        }
                    }
                }
            }
        }
    }

    public void printTables() {
        System.out.println("ACTION 表:");
        System.out.print("状态\t");
        List<String> sortedTerminals = new ArrayList<>(terminals);
        Collections.sort(sortedTerminals);
        for (String t : sortedTerminals) {
            System.out.print(t + "\t");
        }
        System.out.println();

        for (int i = 0; i < states.size(); i++) {
            System.out.print(i + "\t");
            for (String t : sortedTerminals) {
                if (actionTable.containsKey(i) && actionTable.get(i).containsKey(t)) {
                    System.out.print(actionTable.get(i).get(t) + "\t");
                } else {
                    System.out.print("\t");
                }
            }
            System.out.println();
        }

        System.out.println("\nGOTO 表:");
        System.out.print("状态\t");
        List<String> sortedNonTerminals = new ArrayList<>(nonTerminals);
        Collections.sort(sortedNonTerminals);
        sortedNonTerminals.remove("S'");
        for (String nt : sortedNonTerminals) {
            System.out.print(nt + "\t");
        }
        System.out.println();

        for (int i = 0; i < states.size(); i++) {
            System.out.print(i + "\t");
            for (String nt : sortedNonTerminals) {
                if (gotoTable.containsKey(i) && gotoTable.get(i).containsKey(nt)) {
                    System.out.print(gotoTable.get(i).get(nt) + "\t");
                } else {
                    System.out.print("\t");
                }
            }
            System.out.println();
        }
    }

    public boolean parse(List<String> input) {
        input = new ArrayList<>(input);
        input.add("$"); // 添加结束符号
        Stack<Integer> stateStack = new Stack<>();
        Stack<String> symbolStack = new Stack<>();
        stateStack.push(0);
        int ip = 0;

        System.out.println("\n分析过程:");
        System.out.println("栈\t\t\t输入\t\t动作");

        while (true) {
            int currentState = stateStack.peek();
            String currentSymbol = input.get(ip);

            System.out.print(stateStack + " " + symbolStack + "\t" +
                    input.subList(ip, input.size()) + "\t");

            if (!actionTable.containsKey(currentState) ||
                    !actionTable.get(currentState).containsKey(currentSymbol)) {
                System.out.println("错误: 在状态 " + currentState + " 遇到 " + currentSymbol);
                return false;
            }

            String action = actionTable.get(currentState).get(currentSymbol);
            System.out.println(action);

            if (action.startsWith("s")) {
                // 移进动作
                int nextState = Integer.parseInt(action.substring(1));
                stateStack.push(nextState);
                symbolStack.push(currentSymbol);
                ip++;
            } else if (action.startsWith("r")) {
                // 归约动作
                int grammarIndex = Integer.parseInt(action.substring(1));
                Grammar grammar = grammars.get(grammarIndex);
                int popCount = grammar.rhs.size();
                if (grammar.rhs.get(0).equals("ε")) popCount = 0;

                for (int i = 0; i < popCount; i++) {
                    stateStack.pop();
                    symbolStack.pop();
                }

                symbolStack.push(grammar.lhs);
                int gotoState = gotoTable.get(stateStack.peek()).get(grammar.lhs);
                stateStack.push(gotoState);
            } else if (action.equals("acc")) {
                System.out.println("分析成功!");
                return true;
            } else {
                System.out.println("未知动作: " + action);
                return false;
            }
        }
    }
}