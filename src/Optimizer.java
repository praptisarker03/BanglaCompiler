import java.util.*;

public class Optimizer {
    private final List<String> optimizationLog = new ArrayList<>();

    public List<Instruction> optimize(List<Instruction> original) {

        List<Instruction> code = new ArrayList<>();
        for (Instruction inst : original) {
            if (inst.type.equals("LABEL") || inst.type.equals("GOTO") || 
                inst.type.equals("IF_FALSE_GOTO") || inst.type.equals("PRINT")) {
                code.add(new Instruction(inst.type, inst.arg1, inst.arg2));
            } else {
                code.add(new Instruction(inst.result, inst.arg1, inst.operator, inst.arg2));
            }
        }

        boolean hasControlFlow = false;
        for (Instruction inst : code) {
            if (inst.type.equals("LABEL") || inst.type.equals("GOTO") || 
                inst.type.equals("IF_FALSE_GOTO") || inst.type.equals("WHILE")) {
                hasControlFlow = true;
                break;
            }
        }

        if (!hasControlFlow) {
            Map<String, String> constants = new LinkedHashMap<>();
            for (int i = 0; i < code.size(); i++) {
                Instruction inst = code.get(i);


                if (isControlFlow(inst)) continue;


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

    private Integer tryParseBangla(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            StringBuilder sb = new StringBuilder();
            for (char c : s.toCharArray()) {
                if (c == '-') {
                    sb.append('-');
                } else if (c >= '০' && c <= '৯') {
                    sb.append((char) (c - '০' + '0'));
                } else {
                    return null; 
                }
            }
            return Integer.parseInt(sb.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer fold(int left, String operator, int right) {
        switch (operator) {
            case "+": return left + right;
            case "-": return left - right;
            case "*": return left * right;
            case "/": return right != 0 ? left / right : null;
            default: return null;
        }
    }

    private boolean isTemp(String name) {
        return name != null && name.matches("t\\d+");
    }
    public List<String> getOptimizationLog() {
        return optimizationLog;
    }
}
