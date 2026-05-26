import java.util.*;

// Optimizer: Performs intermediate code optimization
// - Constant Folding: Evaluates constant expressions at compile time
// - Constant Propagation: Replaces variables with their constant values
// - Dead Code Elimination: Removes unused temporary variables
public class Optimizer {
    private final List<String> optimizationLog = new ArrayList<>();

    // Main optimization method: applies various optimization techniques
    public List<Instruction> optimize(List<Instruction> original) {
        // Copy instructions while preserving control flow statements
        List<Instruction> code = new ArrayList<>();
        for (Instruction inst : original) {
            if (inst.type.equals("LABEL") || inst.type.equals("GOTO") || 
                inst.type.equals("IF_FALSE_GOTO") || inst.type.equals("PRINT")) {
                code.add(new Instruction(inst.type, inst.arg1, inst.arg2));
            } else {
                code.add(new Instruction(inst.result, inst.arg1, inst.operator, inst.arg2));
            }
        }

        // Check if code contains control flow (loops, conditionals)
        boolean hasControlFlow = false;
        for (Instruction inst : code) {
            if (inst.type.equals("LABEL") || inst.type.equals("GOTO") || 
                inst.type.equals("IF_FALSE_GOTO") || inst.type.equals("WHILE")) {
                hasControlFlow = true;
                break;
            }
        }

        // Apply constant folding and propagation only for code without control flow
        if (!hasControlFlow) {
            Map<String, String> constants = new LinkedHashMap<>();
            for (int i = 0; i < code.size(); i++) {
                Instruction inst = code.get(i);

                // Skip control flow instructions
                if (isControlFlow(inst)) continue;


                // Constant Propagation: Replace variables with their constant values
                if (inst.arg1 != null && constants.containsKey(inst.arg1)) {
                    String oldArg = inst.arg1;
                    inst.arg1 = constants.get(inst.arg1);
                    optimizationLog.add("Constant Propagation: replaced '" + oldArg + "' with '" + inst.arg1 + "' in instruction " + (i + 1));
                }
                if (inst.arg2 != null && constants.containsKey(inst.arg2)) {
                    String oldArg = inst.arg2;
                    inst.arg2 = constants.get(inst.arg2);
                    optimizationLog.add("Constant Propagation: replaced '" + oldArg + "' with '" + inst.arg2 + "' in instruction " + (i + 1));
                }
                // Constant Folding: Evaluate constant expressions at compile time
                if (inst.operator != null && inst.arg1 != null && inst.arg2 != null) {
                    Integer left = tryParseBangla(inst.arg1);
                    Integer right = tryParseBangla(inst.arg2);
                    if (left != null && right != null) {
                        Integer result = fold(left, inst.operator, right);
                        if (result != null) {
                            String banglaResult = SemanticAnalyzer.convertEnglishToBangla(result);
                            optimizationLog.add("Constant Folding: computed " + inst.arg1 + " " + inst.operator + " " + inst.arg2 + " = " + banglaResult + " in instruction " + (i + 1));
                            inst.arg1 = banglaResult;
                            inst.operator = null;
                            inst.arg2 = null;
                        }
                    }
                    if (inst.operator != null && inst.operator.equals("==")) {
                        if (inst.arg1.equals(inst.arg2)) {
                            optimizationLog.add("Constant Folding: " + inst.arg1 + " == " + inst.arg2 + " => সত্য in instruction " + (i + 1));
                            inst.arg1 = "সত্য";
                            inst.operator = null;
                            inst.arg2 = null;
                        } else if (left != null && right != null) {
                            String boolResult = left.equals(right) ? "সত্য" : "মিথ্যা";
                            optimizationLog.add("Constant Folding: " + SemanticAnalyzer.convertEnglishToBangla(left) + " == " + SemanticAnalyzer.convertEnglishToBangla(right) + " => " + boolResult + " in instruction " + (i + 1));
                            inst.arg1 = boolResult;
                            inst.operator = null;
                            inst.arg2 = null;
                        }
                    }
                }
                if (inst.operator == null && inst.arg2 == null && inst.arg1 != null) {
                    constants.put(inst.result, inst.arg1);
                }
            }
        }

        // Dead Code Elimination: Remove unused temporary variables
        Set<String> usedVars = new HashSet<>();
        for (Instruction inst : code) {
            if (inst.arg1 != null) usedVars.add(inst.arg1);
            if (inst.arg2 != null) usedVars.add(inst.arg2);
        }
        List<Instruction> optimized = new ArrayList<>();
        for (Instruction inst : code) {
            if (!isControlFlow(inst) && isTemp(inst.result) && !usedVars.contains(inst.result)) {
                optimizationLog.add("Dead Code Elimination: removed unused temporary '" + inst.result + "'");
                continue;
            }
            optimized.add(inst);
        }

        return optimized;
    }

    private boolean isControlFlow(Instruction inst) {
        return inst.type.equals("LABEL") || inst.type.equals("GOTO") || 
               inst.type.equals("IF_FALSE_GOTO") || inst.type.equals("PRINT");
    }

    // Convert Bangla numerals to Java integers for arithmetic operations
    private Integer tryParseBangla(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            StringBuilder sb = new StringBuilder();
            for (char c : s.toCharArray()) {
                if (c == '-') {
                    sb.append('-');
                } else if (c >= '০' && c <= '৯') {
                    sb.append((char) (c - '०' + '0'));
                } else {
                    return null; 
                }
            }
            return Integer.parseInt(sb.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Perform arithmetic operations for constant folding
    private Integer fold(int left, String operator, int right) {
        switch (operator) {
            case "+": return left + right;
            case "-": return left - right;
            case "*": return left * right;
            case "/": return right != 0 ? left / right : null;
            default: return null;
        }
    }

    // Check if a variable is a temporary (named t0, t1, t2, etc.)
    private boolean isTemp(String name) {
        return name != null && name.matches("t\\d+");
    }
    public List<String> getOptimizationLog() {
        return optimizationLog;
    }
}
