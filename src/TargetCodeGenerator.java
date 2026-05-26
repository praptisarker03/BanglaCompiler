import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TargetCodeGenerator: Converts Bangla AST or intermediate code to Python source code
// Generates executable Python from the Bangla program
public class TargetCodeGenerator {

    // Generate Python code from intermediate instructions
    public String generatePythonCode(List<Instruction> instructions) {
        StringBuilder pyCode = new StringBuilder();
        appendPythonPrelude(pyCode);
        List<String> userVars = new ArrayList<>();

        Map<String, Integer> labelPositions = new HashMap<>();
        for (int i = 0; i < instructions.size(); i++) {
            Instruction inst = instructions.get(i);
            if ("LABEL".equals(inst.type) && inst.arg1 != null) {
                labelPositions.put(inst.arg1, i);
            }
        }

        emitInstructionRange(instructions, 0, instructions.size(), 0, pyCode, userVars, labelPositions);

        appendFinalState(pyCode, userVars);

        return pyCode.toString();
    }

    // Generate Python code directly from Abstract Syntax Tree
    public String generatePythonFromAST(List<ASTNode> nodes) {
        StringBuilder pyCode = new StringBuilder();
        appendPythonPrelude(pyCode);
        List<String> userVars = new ArrayList<>();
        generateBlock(nodes, 0, pyCode, userVars);

        appendFinalState(pyCode, userVars);

        return pyCode.toString();
    }

    // Recursively generate Python code for a block of AST nodes
    private void generateBlock(List<ASTNode> nodes, int indent, StringBuilder pyCode, List<String> userVars) {
        for (ASTNode node : nodes) {
            // Handle indentation for nested blocks
            String pad = "    ".repeat(indent);
            // Handle assignment statements
            if (node instanceof AssignNode) {
                AssignNode assign = (AssignNode) node;
                String expr = generateExpression(assign.expr);
                pyCode.append(pad).append(assign.name).append(" = ").append(expr).append("\n");
                if (!userVars.contains(assign.name)) {
                    userVars.add(assign.name);
                }
            } else if (node instanceof IfNode) {
                IfNode ifNode = (IfNode) node;
                String cond = generateExpression(ifNode.condition);
                pyCode.append(pad).append("if ").append(cond).append(":\n");
                generateBlock(ifNode.thenBody, indent + 1, pyCode, userVars);
                if (ifNode.elseBody != null) {
                    pyCode.append(pad).append("else:\n");
                    generateBlock(ifNode.elseBody, indent + 1, pyCode, userVars);
                }
            } else if (node instanceof WhileNode) {
                WhileNode whileNode = (WhileNode) node;
                String cond = generateExpression(whileNode.condition);
                pyCode.append(pad).append("while ").append(cond).append(":\n");
                generateBlock(whileNode.body, indent + 1, pyCode, userVars);
            } else if (node instanceof PrintNode) {
                PrintNode printNode = (PrintNode) node;
                String expr = generateExpression(printNode.expr);
                pyCode.append(pad).append("print(").append(expr).append(")\n");
            }
        }
    }

    // Convert AST expressions to Python code expressions
    private String generateExpression(ASTNode node) {
        // Handle numeric literals
        if (node instanceof NumberNode) {
            return String.valueOf(((NumberNode) node).value);
        }
        // Handle string literals
        if (node instanceof StringNode) {
            return "\"" + ((StringNode) node).value.replace("\"", "\\\"") + "\"";  
        }
        if (node instanceof BooleanNode) {
            return ((BooleanNode) node).value ? "True" : "False";
        }
        if (node instanceof VarNode) {
            return ((VarNode) node).name;
        }
        // Handle binary operations (arithmetic and comparisons)
        if (node instanceof BinOpNode) {
            BinOpNode binOp = (BinOpNode) node;
            String left = generateExpression(binOp.left);
            String right = generateExpression(binOp.right);
            // Use custom truncation division to match Bangla semantics
            if ("/".equals(binOp.operator)) {
                return "trunc_div(" + left + ", " + right + ")";
            }
            return "(" + left + " " + binOp.operator + " " + right + ")";
        }
        return "None";
    }

    // Add Python helper functions and header comments
    private void appendPythonPrelude(StringBuilder pyCode) {
        pyCode.append("# Auto-generated Python Target Code from Bangla Compiler\n\n");
        pyCode.append("def trunc_div(a, b):\n");  // Define truncating integer division
        pyCode.append("    if b == 0:\n");
        pyCode.append("        raise ZeroDivisionError('division by zero')\n");
        pyCode.append("    return int(a / b)\n\n");
    }

    private void appendFinalState(StringBuilder pyCode, List<String> userVars) {
        pyCode.append("\n# Print final state of variables\n");
        pyCode.append("print('--- Execution Output ---')\n");
        for (String var : userVars) {
            pyCode.append("print('").append(var).append(":', ").append(var).append(")\n");
        }
    }

    private void emitInstructionRange(
        List<Instruction> instructions,
        int start,
        int end,
        int indent,
        StringBuilder pyCode,
        List<String> userVars,
        Map<String, Integer> labelPositions
    ) {
        int i = start;
        while (i < end) {
            Instruction inst = instructions.get(i);
            String pad = "    ".repeat(indent);

            if ("LABEL".equals(inst.type) || "GOTO".equals(inst.type)) {
                i++;
                continue;
            }

            if ("IF_FALSE_GOTO".equals(inst.type)) {
                int falseLabelIndex = labelPositions.getOrDefault(inst.arg2, -1);
                String condition = mapValue(inst.arg1);
                if (i - 1 >= start) {
                    Instruction prev = instructions.get(i - 1);
                    if (!isControlFlow(prev) && inst.arg1 != null && inst.arg1.equals(prev.result)) {
                        condition = renderInstructionExpression(prev);
                    }
                }

                if (falseLabelIndex > i && falseLabelIndex < end) {
                    int beforeFalseLabel = falseLabelIndex - 1;

                    if (beforeFalseLabel >= i + 1 && "GOTO".equals(instructions.get(beforeFalseLabel).type)) {
                        Instruction gotoInst = instructions.get(beforeFalseLabel);
                        Integer loopStartIndex = labelPositions.get(gotoInst.arg1);
                        if (loopStartIndex != null && loopStartIndex < i && loopStartIndex >= start) {
                            pyCode.append(pad).append("while ").append(condition).append(":\n");
                            emitInstructionRange(
                                instructions,
                                i + 1,
                                beforeFalseLabel,
                                indent + 1,
                                pyCode,
                                userVars,
                                labelPositions
                            );
                            i = falseLabelIndex + 1;
                            continue;
                        }

                        Integer endLabelIndex = labelPositions.get(gotoInst.arg1);
                        if (endLabelIndex != null && endLabelIndex > falseLabelIndex && endLabelIndex <= end) {
                            pyCode.append(pad).append("if ").append(condition).append(":\n");
                            emitInstructionRange(
                                instructions,
                                i + 1,
                                beforeFalseLabel,
                                indent + 1,
                                pyCode,
                                userVars,
                                labelPositions
                            );
                            pyCode.append(pad).append("else:\n");
                            emitInstructionRange(
                                instructions,
                                falseLabelIndex + 1,
                                endLabelIndex,
                                indent + 1,
                                pyCode,
                                userVars,
                                labelPositions
                            );
                            i = endLabelIndex + 1;
                            continue;
                        }
                    }

                    pyCode.append(pad).append("if ").append(condition).append(":\n");
                    emitInstructionRange(
                        instructions,
                        i + 1,
                        falseLabelIndex,
                        indent + 1,
                        pyCode,
                        userVars,
                        labelPositions
                    );
                    i = falseLabelIndex + 1;
                    continue;
                }

                pyCode.append(pad).append("if ").append(condition).append(":\n");
                i++;
                continue;
            }

            if (!isControlFlow(inst)
                && i + 1 < end
                && "IF_FALSE_GOTO".equals(instructions.get(i + 1).type)
                && inst.result != null
                && inst.result.equals(instructions.get(i + 1).arg1)) {
                i++;
                continue;
            }

            if ("PRINT".equals(inst.type)) {
                pyCode.append(pad).append("print(").append(mapValue(inst.arg1)).append(")\n");
                i++;
                continue;
            }

            String expr = renderInstructionExpression(inst);
            pyCode.append(pad).append(inst.result).append(" = ").append(expr).append("\n");
            if (inst.result != null && !inst.result.matches("t\\d+") && !userVars.contains(inst.result)) {
                userVars.add(inst.result);
            }
            i++;
        }
    }

    private boolean isControlFlow(Instruction inst) {
        return "LABEL".equals(inst.type)
            || "GOTO".equals(inst.type)
            || "IF_FALSE_GOTO".equals(inst.type)
            || "PRINT".equals(inst.type);
    }

    private String renderInstructionExpression(Instruction inst) {
        String left = mapValue(inst.arg1);
        if (inst.operator == null) {
            return left;
        }

        String right = mapValue(inst.arg2);
        if ("/".equals(inst.operator)) {
            return "trunc_div(" + left + ", " + right + ")";
        }

        return left + " " + inst.operator + " " + right;
    }

    
    private String mapValue(String val) {
        if (val == null) return null;

        
        if (val.equals("সত্য")) return "True";
        if (val.equals("মিথ্যা")) return "False";

        if (isBanglaNumber(val)) {
            return String.valueOf(SemanticAnalyzer.convertBanglaToEnglish(val));
        }

        if (val.startsWith("\"") && val.endsWith("\"")) {
            return val;
        }

        return val;
    }

    private boolean isBanglaNumber(String val) {
        if (val == null || val.isEmpty()) return false;
        for (int i = 0; i < val.length(); i++) {
            char c = val.charAt(i);
            if (i == 0 && c == '-') continue; 
            if (c < '০' || c > '৯') return false;
        }
        return true;
    }
}
