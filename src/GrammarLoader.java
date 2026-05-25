import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class GrammarLoader {
    public final Map<String, List<List<String>>> productions = new LinkedHashMap<>();
    public String startSymbol;
    public final Set<String> terminals = new HashSet<>();
    public final Set<String> nonTerminals = new HashSet<>();
    public final Map<String, Map<String, List<String>>> parseTable = new HashMap<>();
    
    private final Map<String, Set<String>> firstSets = new HashMap<>();
    private final Map<String, Set<String>> followSets = new HashMap<>();

    public void loadGrammar(Path bnfFile) throws IOException {
        List<String> lines = Files.readAllLines(bnfFile);
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split("::=");
            if (parts.length != 2) continue;
            String lhs = parts[0].trim();
            if (startSymbol == null) startSymbol = lhs;
            nonTerminals.add(lhs);
            
            String[] rhsList = parts[1].split("\\|");
            List<List<String>> prods = productions.computeIfAbsent(lhs, k -> new ArrayList<>());
            for (String rhs : rhsList) {
                String[] symbols = rhs.trim().split("\\s+");
                List<String> prod = new ArrayList<>();
                for (String sym : symbols) {
                    if (!sym.isEmpty()) {
                        prod.add(sym);
                    }
                }
                prods.add(prod);
            }
        }
        
        for (List<List<String>> prods : productions.values()) {
            for (List<String> prod : prods) {
                for (String sym : prod) {
                    if (!nonTerminals.contains(sym) && !sym.startsWith("@") && !sym.equals("EPSILON")) {
                        terminals.add(sym);
                    }
                }
            }
        }
        
        computeFirstSets();
        computeFollowSets();
        buildParseTable();
    }
    
    private void computeFirstSets() {
        for (String nt : nonTerminals) firstSets.put(nt, new HashSet<>());
        for (String t : terminals) firstSets.put(t, new HashSet<>(Arrays.asList(t)));
        firstSets.put("EPSILON", new HashSet<>(Arrays.asList("EPSILON")));
        
        boolean changed;
        do {
            changed = false;
            for (String nt : nonTerminals) {
                for (List<String> prod : productions.get(nt)) {
                    boolean allEpsilon = true;
                    for (String sym : prod) {
                        if (sym.startsWith("@")) continue;
                        Set<String> symFirst = firstSets.get(sym);
                        if (symFirst != null) {
                            for (String f : symFirst) {
                                if (!f.equals("EPSILON") && firstSets.get(nt).add(f)) {
                                    changed = true;
                                }
                            }
                            if (!symFirst.contains("EPSILON")) {
                                allEpsilon = false;
                                break;
                            }
                        }
                    }
                    if (allEpsilon && firstSets.get(nt).add("EPSILON")) {
                        changed = true;
                    }
                }
            }
        } while (changed);
    }
    
    private void computeFollowSets() {
        for (String nt : nonTerminals) followSets.put(nt, new HashSet<>());
        
        boolean changed;
        do {
            changed = false;
            for (String nt : nonTerminals) {
                for (List<String> prod : productions.get(nt)) {
                    for (int i = 0; i < prod.size(); i++) {
                        String sym = prod.get(i);
                        if (nonTerminals.contains(sym)) {
                            Set<String> followSym = followSets.get(sym);
                            boolean allEpsilon = true;
                            for (int j = i + 1; j < prod.size(); j++) {
                                String next = prod.get(j);
                                if (next.startsWith("@")) continue;
                                Set<String> nextFirst = firstSets.get(next);
                                if (nextFirst != null) {
                                    for (String f : nextFirst) {
                                        if (!f.equals("EPSILON") && followSym.add(f)) {
                                            changed = true;
                                        }
                                    }
                                    if (!nextFirst.contains("EPSILON")) {
                                        allEpsilon = false;
                                        break;
                                    }
                                }
                            }
                            if (allEpsilon) {
                                for (String f : followSets.get(nt)) {
                                    if (followSym.add(f)) {
                                        changed = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } while (changed);
    }
    
    private void buildParseTable() {
        for (String nt : nonTerminals) {
            parseTable.put(nt, new HashMap<>());
            for (List<String> prod : productions.get(nt)) {
                Set<String> firstOfProd = new HashSet<>();
                boolean allEpsilon = true;
                for (String sym : prod) {
                    if (sym.startsWith("@")) continue;
                    Set<String> symFirst = firstSets.get(sym);
                    if (symFirst != null) {
                        for (String f : symFirst) {
                            if (!f.equals("EPSILON")) firstOfProd.add(f);
                        }
                        if (!symFirst.contains("EPSILON")) {
                            allEpsilon = false;
                            break;
                        }
                    }
                }
                if (allEpsilon) firstOfProd.add("EPSILON");
                
                for (String term : firstOfProd) {
                    if (!term.equals("EPSILON")) {
                        parseTable.get(nt).put(term, prod);
                    }
                }
                
                if (firstOfProd.contains("EPSILON")) {
                    for (String followTerm : followSets.get(nt)) {
                        parseTable.get(nt).put(followTerm, prod);
                    }
                }
            }
        }
    }
}
