import java.util.*;
import java.io.PrintWriter;

public class SemanticAnalyzer {
    private final SymbolTable symbolTable;
    private final List<String> printOutput = new ArrayList<>();

    public SemanticAnalyzer(SymbolTable st) {
        this.symbolTable = st;
    }

    public void analyze(List<ASTNode> nodes) {
        for (ASTNode node : nodes) {
            evaluate(node);
        }
    }

    public List<String> getPrintOutput() {
        return printOutput;
    }

    private Object evaluate(ASTNode node) {
        if (node instanceof NumberNode) {
            return ((NumberNode) node).value;
        }

        if (node instanceof StringNode) {
            return ((StringNode) node).value;
        }

        if (node instanceof BooleanNode) {
            return ((BooleanNode) node).value;
        }

        if (node instanceof VarNode) {
            String name = ((VarNode) node).name;
            if (!symbolTable.contains(name)) {
                throw new RuntimeException("Semantic Error: Variable '" + name + "' used before declaration.");
            }
            return symbolTable.get(name);
        }

        if (node instanceof BinOpNode) {
            BinOpNode binOp = (BinOpNode) node;
            Object leftObj = evaluate(binOp.left);
            Object rightObj = evaluate(binOp.right);

            if (binOp.operator.equals("+")) {
                if (leftObj instanceof String || rightObj instanceof String) {
                    return String.valueOf(leftObj) + String.valueOf(rightObj);
                }
            }

            if (binOp.operator.equals("==")) {
                return leftObj.equals(rightObj);
            }
            if (binOp.operator.equals("!=")) {
                return !leftObj.equals(rightObj);
            }

            if (!(leftObj instanceof Integer) || !(rightObj instanceof Integer)) {
                throw new RuntimeException("Semantic Error: Mathematical operations are only allowed on numbers.");
            }

            int left = (Integer) leftObj;
            int right = (Integer) rightObj;

            switch (binOp.operator) {
                case "+": return left + right;
                case "-": return left - right;
                case "*": return left * right;
                case "/": 
                    if (right == 0) throw new RuntimeException("Semantic Error: Division by zero.");
                    return left / right;
                case ">": return left > right;
                case "<": return left < right;
                case ">=": return left >= right;
                case "<=": return left <= right;
            }
        }

        if (node instanceof AssignNode) {
            AssignNode assign = (AssignNode) node;
            Object value = evaluate(assign.expr);
            symbolTable.set(assign.name, value);
            return value;
        }

        if (node instanceof IfNode) {
            IfNode ifNode = (IfNode) node;
            Object condVal = evaluate(ifNode.condition);
            boolean cond = toBool(condVal);
            if (cond) {
                for (ASTNode stmt : ifNode.thenBody) {
                    evaluate(stmt);
                }
            } else if (ifNode.elseBody != null) {
                for (ASTNode stmt : ifNode.elseBody) {
                    evaluate(stmt);
                }
            }
            return null;
        }

        if (node instanceof WhileNode) {
            WhileNode whileNode = (WhileNode) node;
            while (toBool(evaluate(whileNode.condition))) {
                for (ASTNode stmt : whileNode.body) {
                    evaluate(stmt);
                }
            }
            return null;
        }

        if (node instanceof PrintNode) {
            PrintNode printNode = (PrintNode) node;
            Object value = evaluate(printNode.expr);
            String output;
            if (value instanceof Boolean) {
                output = (Boolean) value ? "সত্য" : "মিথ্যা";
            } else if (value instanceof String) {
                output = (String) value;
            } else {
                output = convertEnglishToBangla((Integer) value);
            }
            printOutput.add(output);
            return null;
        }

        return 0;
    }

    private boolean toBool(Object val) {
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof Integer) return (Integer) val != 0;
        return false;
    }

    public static String convertEnglishToBangla(int number) {
        String engNum = String.valueOf(number);
        StringBuilder sb = new StringBuilder();
        for (char c : engNum.toCharArray()) {
            if (c == '-') sb.append('-'); // Handle negative results
            else sb.append((char) (c - '0' + '০'));
        }
        return sb.toString();
    }
    public static int convertBanglaToEnglish(String bNum) {
        StringBuilder sb = new StringBuilder();
        for (char c : bNum.toCharArray()) sb.append((char) (c - '০' + '0'));
        return Integer.parseInt(sb.toString());
    }
}