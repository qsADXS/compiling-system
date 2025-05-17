package src.lr1parser;

import java.util.Arrays;
import java.util.List;

public class TestCases {
    public static void main(String[] args) {
        // 测试用例1: 简单算术表达式
        testArithmeticExpression();

        // 测试用例2: 简单if语句
        testIfStatement();

        // 测试用例3: 简单声明语句
        testDeclaration();
    }

    private static void testArithmeticExpression() {
        System.out.println("=== 测试用例1: 简单算术表达式 ===");

        List<Grammar> grammars = Arrays.asList(
                new Grammar("E", Arrays.asList("E", "+", "T")),
                new Grammar("E", Arrays.asList("T")),
                new Grammar("T", Arrays.asList("T", "*", "F")),
                new Grammar("T", Arrays.asList("F")),
                new Grammar("F", Arrays.asList("(", "E", ")")),
                new Grammar("F", Arrays.asList("id"))
        );

        LR1Parser parser = new LR1Parser(grammars, "E");
        parser.printTables();

        List<String> input = Arrays.asList("id", "*", "id", "+", "id");
        System.out.println("\n测试输入: " + input);
        parser.parse(input);
    }

    private static void testIfStatement() {
        System.out.println("\n=== 测试用例2: 简单if语句 ===");

        List<Grammar> grammars = Arrays.asList(
                new Grammar("S", Arrays.asList("if", "E", "then", "S", "else", "S")),
                new Grammar("S", Arrays.asList("if", "E", "then", "S")),
                new Grammar("S", Arrays.asList("other")),
                new Grammar("E", Arrays.asList("id"))
        );

        LR1Parser parser = new LR1Parser(grammars, "S");
        parser.printTables();

        List<String> input = Arrays.asList("if", "id", "then", "if", "id", "then", "other", "else", "other");
        System.out.println("\n测试输入: " + input);
        parser.parse(input);
    }

    private static void testDeclaration() {
        System.out.println("\n=== 测试用例3: 简单声明语句 ===");

        List<Grammar> grammars = Arrays.asList(
                new Grammar("D", Arrays.asList("T", "id", ";", "D")),
                new Grammar("D", Arrays.asList("ε")),
                new Grammar("T", Arrays.asList("int")),
                new Grammar("T", Arrays.asList("float"))
        );

        LR1Parser parser = new LR1Parser(grammars, "D");
        parser.printTables();

        List<String> input = Arrays.asList("int", "id", ";", "float", "id", ";");
        System.out.println("\n测试输入: " + input);
        parser.parse(input);
    }
}