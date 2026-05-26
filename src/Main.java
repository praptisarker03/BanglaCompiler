import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

// Main: Entry point of the Bangla Compiler
// Orchestrates the full compilation pipeline:
// 1. Lexical Analysis (Tokenization)
// 2. Syntax Analysis (Parsing to AST)
// 3. Semantic Analysis (Type checking, Symbol Table)
// 4. Intermediate Code Generation
// 5. Code Optimization
// 6. Target Code Generation (Python)
public class Main {
    public static void main(String[] args) {
        // Get input and output file paths from arguments or use defaults
        String inputFilePath = args.length > 0 ? args[0] : "input.bn";
        String outputFilePath = args.length > 1 ? args[1] : "output.txt";
        // Current working directory
        java.nio.file.Path cwd = Paths.get("").toAbsolutePath();
        java.nio.file.Path inputPath = Paths.get(inputFilePath);
        java.nio.file.Path outputPath = Paths.get(outputFilePath);
        if (!inputPath.isAbsolute()) {
            inputPath = cwd.resolve(inputFilePath).normalize();
            
            if (!Files.exists(inputPath) && cwd.endsWith("src")) {
                inputPath = cwd.getParent().resolve(inputFilePath).normalize();
            }
        }
        if (!outputPath.isAbsolute()) {
            outputPath = cwd.resolve(outputFilePath).normalize();
        
            if (cwd.endsWith("src")) {
                outputPath = cwd.getParent().resolve(outputFilePath).normalize();
            }
        }
        try (PrintWriter out = new PrintWriter(outputPath.toFile(), StandardCharsets.UTF_8)) {
            try {
                // Read source code from Bangla file
                String source = Files.readString(inputPath, StandardCharsets.UTF_8);

                // Step 1: Lexical Analysis - Convert source to tokens
                Lexer lexer = new Lexer(source);
                List<Token> tokens = lexer.scanTokens();

                // Step 2: Syntax Analysis - Parse tokens into AST
                Parser parser = new Parser(tokens);
                List<ASTNode> astNodes = parser.parse();
                
                // Step 3: Semantic Analysis - Type checking and symbol table
                SymbolTable st = new SymbolTable();
                SemanticAnalyzer analyzer = new SemanticAnalyzer(st);
                analyzer.analyze(astNodes);
                
                out.println("[Input]: " + inputPath);
                out.println("[Output]: " + outputPath);
                // Print compilation report
                out.println("=== STEP 1: LEXICAL ANALYSIS (TOKENS) ===");
                for (Token t : tokens) {
                    out.println(t.toString());
                }

                out.println("\n=== STEP 2: SYNTAX ANALYSIS (AST) ===");
                for (ASTNode node : astNodes) {
                    writeTreeToFile(node, 0, out);
                }

                out.println("\n=== STEP 3: SEMANTIC ANALYSIS & SYMBOL TABLE ===");
                out.println("[Status]: Type checking passed.");
                out.println("[Status]: Scope check passed.");
                out.println("-------------------------");
                st.displayToFile(out);
                
                List<String> printOutput = analyzer.getPrintOutput();
                if (!printOutput.isEmpty()) {
                    out.println("-------------------------");
                    out.println("[Print Output]:");
                    for (String po : printOutput) {
                        out.println("  " + po);
                    }
                }

                // Step 4: Intermediate Code Generation
                IntermediateCodeGenerator icg = new IntermediateCodeGenerator();
                List<Instruction> instructions = icg.generate(astNodes);
                
                out.println("\n=== STEP 4: INTERMEDIATE CODE ===");
                for (Instruction inst : instructions) {
                    out.println(inst.toString());
                }

                // Step 5: Code Optimization - Constant folding, dead code removal
                Optimizer optimizer = new Optimizer();
                List<Instruction> optimized = optimizer.optimize(instructions);

                out.println("\n=== STEP 5: CODE OPTIMIZATION ===");
                out.println("[Before]: " + instructions.size() + " instructions");
                out.println("[After]:  " + optimized.size() + " instructions");
                out.println("[Removed]: " + (instructions.size() - optimized.size()) + " instructions");
                out.println("-------------------------");
                out.println("[Optimized Code]:");
                for (Instruction inst : optimized) {
                    out.println("  " + inst.toString());
                }

                // Step 6: Target Code Generation - Generate Python code
                TargetCodeGenerator tcg = new TargetCodeGenerator();
                String pythonCode = tcg.generatePythonFromAST(astNodes);
                
                // Write generated Python to file
                java.nio.file.Path targetPath = outputPath.getParent().resolve("output.py");
                Files.writeString(targetPath, pythonCode, StandardCharsets.UTF_8);

                out.println("\n=== STEP 6: TARGET CODE GENERATION ===");
                out.println("[Status]: Python target code generated successfully.");
                out.println("[Target File]: " + targetPath);

                System.out.println("Process completed successfully. Check output at: " + outputPath);
                System.out.println("Target code generated at: " + targetPath);

            } catch (RuntimeException semanticOrSyntaxError) {
                out.println("\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                out.println("COMPILER ERROR: " + semanticOrSyntaxError.getMessage());
                out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.err.println("Compiler error detected. Details written to output.txt");

            } catch (IOException e) {
                out.println("FILE ERROR: Could not find or read '" + inputPath + "'");
                System.err.println("File error written to output.txt");
            }

        } catch (Exception fatalError) {
            
            System.err.println("Fatal Error: Could not write to output file. " + fatalError.getMessage());
        }
    }

    public static void writeTreeToFile(ASTNode node, int indent, PrintWriter out) {
        String p = "  ".repeat(indent);
        if (node instanceof AssignNode) {
            AssignNode a = (AssignNode) node;
            out.println(p + "Assignment: " + a.name);
            writeTreeToFile(a.expr, indent + 1, out);
        } else if (node instanceof BinOpNode) {
            BinOpNode b = (BinOpNode) node;
            out.println(p + "Binary Operator: " + b.operator);
            writeTreeToFile(b.left, indent + 1, out);
            writeTreeToFile(b.right, indent + 1, out);
        } else if (node instanceof VarNode) {
            out.println(p + "Variable: " + ((VarNode) node).name);
        } else if (node instanceof NumberNode) {
            int val = ((NumberNode) node).value;
            String bVal = SemanticAnalyzer.convertEnglishToBangla(val);
            out.println(p + "Integer Value: " + bVal);
        } else if (node instanceof BooleanNode) {
            boolean val = ((BooleanNode) node).value;
            out.println(p + "Boolean Value: " + (val ? "সত্য" : "মিথ্যা"));
        } else if (node instanceof IfNode) {
            IfNode ifNode = (IfNode) node;
            out.println(p + "If Statement:");
            out.println(p + "  Condition:");
            writeTreeToFile(ifNode.condition, indent + 2, out);
            out.println(p + "  Then:");
            for (ASTNode stmt : ifNode.thenBody) {
                writeTreeToFile(stmt, indent + 2, out);
            }
            if (ifNode.elseBody != null) {
                out.println(p + "  Else:");
                for (ASTNode stmt : ifNode.elseBody) {
                    writeTreeToFile(stmt, indent + 2, out);
                }
            }
        } else if (node instanceof WhileNode) {
            WhileNode whileNode = (WhileNode) node;
            out.println(p + "While Loop:");
            out.println(p + "  Condition:");
            writeTreeToFile(whileNode.condition, indent + 2, out);
            out.println(p + "  Body:");
            for (ASTNode stmt : whileNode.body) {
                writeTreeToFile(stmt, indent + 2, out);
            }
        } else if (node instanceof PrintNode) {
            PrintNode printNode = (PrintNode) node;
            out.println(p + "Print:");
            writeTreeToFile(printNode.expr, indent + 1, out);
        }
    }
}
