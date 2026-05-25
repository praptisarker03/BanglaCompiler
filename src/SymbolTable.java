import java.util.*;
import java.io.PrintWriter;

public class SymbolTable {
    private final Map<String, Object> table = new LinkedHashMap<>();

    public void set(String name, Object val) { 
        table.put(name, val); 
    }

    public Object get(String name) { 
        return table.get(name); 
    }

    public boolean contains(String name) { 
        return table.containsKey(name); 
    }

    public void displayToFile(PrintWriter out) {
        out.println("Variable | Value (Bangla)");
        out.println("-------------------------");
        if (table.isEmpty()) {
            out.println("(No variables defined)");
        } else {
            table.forEach((k, v) -> {
                String bVal;
                if (v instanceof Boolean) {
                    bVal = (Boolean) v ? "সত্য" : "মিথ্যা";
                } else if (v instanceof String) {
                    bVal = (String) v;
                } else {
                    bVal = SemanticAnalyzer.convertEnglishToBangla((Integer) v);
                }
                out.println(k + " : " + bVal);
            });
        }
    }
}