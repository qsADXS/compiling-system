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
        // 合并所有符号
        List<String> allSymbols = new ArrayList<>(terminals);
        allSymbols.addAll(nonTerminals);
        allSymbols.remove("S'"); // 移除增广文法符号
        Collections.sort(allSymbols);

        // 打印表头
        System.out.println("LR(1) 分析表");
        System.out.print("状态\t");
        for (String symbol : allSymbols) {
            System.out.print(symbol + "\t");
        }
        System.out.println();

        // 打印表格内容
        for (int i = 0; i < states.size(); i++) {
            System.out.print(i + "\t");
            for (String symbol : allSymbols) {
                if (terminals.contains(symbol)) {
                    // 处理 ACTION 表部分
                    if (actionTable.containsKey(i) && actionTable.get(i).containsKey(symbol)) {
                        System.out.print(actionTable.get(i).get(symbol) + "\t");
                    } else {
                        System.out.print("\t");
                    }
                } else {
                    // 处理 GOTO 表部分
                    if (gotoTable.containsKey(i) && gotoTable.get(i).containsKey(symbol)) {
                        System.out.print(gotoTable.get(i).get(symbol) + "\t");
                    } else {
                        System.out.print("\t");
                    }
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

        // 打印表头
        System.out.println("\n分析过程:");
        System.out.println(
                String.format("%-50s %-50s %-50s %s",
                        "状态栈", "符号栈", "剩余输入", "动作")
        );
        printAnalysisStep(stateStack, symbolStack, input, ip, "初始化");

        while (true) {
            int currentState = stateStack.peek();
            String currentSymbol = input.get(ip);

            if (!actionTable.containsKey(currentState) ||
                    !actionTable.get(currentState).containsKey(currentSymbol)) {
                printAnalysisStep(stateStack, symbolStack, input, ip,
                        "错误: 在状态 " + currentState + " 遇到 " + currentSymbol);
                return false;
            }

            String action = actionTable.get(currentState).get(currentSymbol);

            if (action.startsWith("s")) {
                // 移进动作
                int nextState = Integer.parseInt(action.substring(1));
                stateStack.push(nextState);
                symbolStack.push(currentSymbol);
                ip++;
                printAnalysisStep(stateStack, symbolStack, input, ip, "移进: " + action);
            } else if (action.startsWith("r")) {
                // 归约动作
                int grammarIndex = Integer.parseInt(action.substring(1));
                Grammar grammar = grammars.get(grammarIndex);
                int popCount = grammar.rhs.size();
                if (grammar.rhs.get(0).equals("ε")) popCount = 0;

                // 弹出栈
                for (int i = 0; i < popCount; i++) {
                    stateStack.pop();
                    symbolStack.pop();
                }

                // 压入新符号
                symbolStack.push(grammar.lhs);

                // 查找GOTO表
                int gotoState = gotoTable.get(stateStack.peek()).get(grammar.lhs);
                stateStack.push(gotoState);
                printAnalysisStep(stateStack, symbolStack, input, ip,
                        "归约: " + action + " (" + grammar + ")");
            } else if (action.equals("acc")) {
                // 接受
                printAnalysisStep(stateStack, symbolStack, input, ip, "接受");
                System.out.println("分析成功!");
                return true;
            } else {
                printAnalysisStep(stateStack, symbolStack, input, ip,
                        "未知动作: " + action);
                return false;
            }
        }
    }

    // 打印分析步骤的辅助方法
    private void printAnalysisStep(Stack<Integer> stateStack,
                                   Stack<String> symbolStack,
                                   List<String> input,
                                   int ip,
                                   String action) {
        String stateStr = stateStack.toString();
        String symbolStr = symbolStack.toString();
        String inputStr = input.subList(ip, input.size()).toString();

        System.out.println(
                String.format("%-50s %-50s %-50s %s",
                        stateStr, symbolStr, inputStr, action)
        );
    }
}