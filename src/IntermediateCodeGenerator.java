import java.util.ArrayList;
import java.util.List;

public class IntermediateCodeGenerator {
    private final List<Instruction> instructions = new ArrayList<>();
    private int tempCount = 1;
    private int labelCount = 1;

    private String generateTemp() {
        return "t" + (tempCount++);
    }

    private String generateLabel() {
        return "L" + (labelCount++);
    }

    public List<Instruction> generate(List<ASTNode> nodes) {
        for (ASTNode node : nodes) {
            generateStatement(node);
        }
        return instructions;
    }

    private void generateStatement(ASTNode node) {
        if (node instanceof AssignNode) {
            AssignNode assign = (AssignNode) node;
            String exprCode = generateExpr(assign.expr);
            instructions.add(new Instruction(assign.name, exprCode, null, null));
        } else if (node instanceof IfNode) {
            generateIf((IfNode) node);
        } else if (node instanceof WhileNode) {
            generateWhile((WhileNode) node);
        } else if (node instanceof PrintNode) {
            PrintNode printNode = (PrintNode) node;
            String exprCode = generateExpr(printNode.expr);
            instructions.add(new Instruction("PRINT", exprCode, null));
        }
    }

    private void generateIf(IfNode node) {
        String condCode = generateExpr(node.condition);

        if (node.elseBody != null) {
            String elseLabel = generateLabel();
            String endLabel = generateLabel();
            instructions.add(new Instruction("IF_FALSE_GOTO", condCode, elseLabel));
            for (ASTNode stmt : node.thenBody) {
                generateStatement(stmt);
            }
            instructions.add(new Instruction("GOTO", endLabel, null));
            instructions.add(new Instruction("LABEL", elseLabel, null));
            for (ASTNode stmt : node.elseBody) {
                generateStatement(stmt);
            }
            instructions.add(new Instruction("LABEL", endLabel, null));
        } else {
            String endLabel = generateLabel();
            instructions.add(new Instruction("IF_FALSE_GOTO", condCode, endLabel));
            for (ASTNode stmt : node.thenBody) {
                generateStatement(stmt);
            }
            instructions.add(new Instruction("LABEL", endLabel, null));
        }
    }

    private void generateWhile(WhileNode node) {
        String startLabel = generateLabel();
        String endLabel = generateLabel();

        instructions.add(new Instruction("LABEL", startLabel, null));
        String condCode = generateExpr(node.condition);
        instructions.add(new Instruction("IF_FALSE_GOTO", condCode, endLabel));

        for (ASTNode stmt : node.body) {
            generateStatement(stmt);
        }

        instructions.add(new Instruction("GOTO", startLabel, null));
        instructions.add(new Instruction("LABEL", endLabel, null));
    }

    private String generateExpr(ASTNode node) {
        if (node instanceof NumberNode) {
            return SemanticAnalyzer.convertEnglishToBangla(((NumberNode) node).value);
        }

        if (node instanceof StringNode) {
            return "\"" + ((StringNode) node).value.replace("\"", "\\\"") + "\"";
        }
        
        if (node instanceof BooleanNode) {
            return ((BooleanNode) node).value ? "সত্য" : "মিথ্যা";
        }

        if (node instanceof VarNode) {
            return ((VarNode) node).name;
        }

        if (node instanceof BinOpNode) {
            BinOpNode binOp = (BinOpNode) node;
            String leftCode = generateExpr(binOp.left);
            String rightCode = generateExpr(binOp.right);
            String temp = generateTemp();
            instructions.add(new Instruction(temp, leftCode, binOp.operator, rightCode));
            return temp;
        }

        return null;
    }
}
