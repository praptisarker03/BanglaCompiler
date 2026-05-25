import java.util.*;

public class Parser {
    private final List<Token> tokens;
    private final List<String> errors = new ArrayList<>();
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // Keep old constructor for backward compatibility (grammar loader ignored now)
    public Parser(List<Token> tokens, GrammarLoader grammar) {
        this.tokens = tokens;
    }

    public List<ASTNode> parse() {
        List<ASTNode> statements = new ArrayList<>();
        while (!isAtEnd()) {
            try {
                statements.add(parseStatement());
            } catch (RuntimeException e) {
                errors.add(e.getMessage());
                synchronize();
            }
        }
        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder("Syntax Error(s):\n");
            for (String err : errors) {
                sb.append("- ").append(err).append("\n");
            }
            throw new RuntimeException(sb.toString().trim());
        }
        return statements;
    }

    // statement = declaration | assignment | if-else | while | print
    private ASTNode parseStatement() {
        if (check(TokenType.DHORO)) {
            return parseDeclaration();
        }
        if (check(TokenType.IF)) {
            return parseIf();
        }
        if (check(TokenType.WHILE)) {
            return parseWhile();
        }
        if (check(TokenType.PRINT)) {
            return parsePrint();
        }
        if (check(TokenType.ID)) {
            return parseAssignment();
        }
        throw new RuntimeException("Syntax Error: unexpected token '" + peek().lexeme + "' at line " + peek().line);
    }

    // declaration = DHORO ID ASSIGN expression SEMI
    private ASTNode parseDeclaration() {
        consume(TokenType.DHORO, "Expected 'ধরো'");
        Token name = consume(TokenType.ID, "Expected variable name");
        consume(TokenType.ASSIGN, "Expected '='");
        ASTNode expr = parseExpression();
        consume(TokenType.SEMI, "Expected ';'");
        return new AssignNode(name.lexeme, expr);
    }

    // assignment = ID ASSIGN expression SEMI (re-assignment without সংখ্যা)
    private ASTNode parseAssignment() {
        Token name = consume(TokenType.ID, "Expected variable name");
        consume(TokenType.ASSIGN, "Expected '='");
        ASTNode expr = parseExpression();
        consume(TokenType.SEMI, "Expected ';'");
        return new AssignNode(name.lexeme, expr);
    }

    // if = IF LPAREN expression RPAREN LBRACE statements RBRACE (ELSE LBRACE statements RBRACE)?
    private ASTNode parseIf() {
        consume(TokenType.IF, "Expected 'যদি'");
        consume(TokenType.LPAREN, "Expected '('");
        ASTNode condition = parseExpression();
        consume(TokenType.RPAREN, "Expected ')'");
        consume(TokenType.LBRACE, "Expected '{'");
        List<ASTNode> thenBody = parseBlock();
        consume(TokenType.RBRACE, "Expected '}'");

        List<ASTNode> elseBody = null;
        if (check(TokenType.ELSE)) {
            advance();
            consume(TokenType.LBRACE, "Expected '{'");
            elseBody = parseBlock();
            consume(TokenType.RBRACE, "Expected '}'");
        }

        return new IfNode(condition, thenBody, elseBody);
    }

    // while = WHILE LPAREN expression RPAREN LBRACE statements RBRACE
    private ASTNode parseWhile() {
        consume(TokenType.WHILE, "Expected 'যতক্ষণ'");
        consume(TokenType.LPAREN, "Expected '('");
        ASTNode condition = parseExpression();
        consume(TokenType.RPAREN, "Expected ')'");
        consume(TokenType.LBRACE, "Expected '{'");
        List<ASTNode> body = parseBlock();
        consume(TokenType.RBRACE, "Expected '}'");
        return new WhileNode(condition, body);
    }

    // print = PRINT LPAREN expression RPAREN SEMI
    private ASTNode parsePrint() {
        consume(TokenType.PRINT, "Expected 'দেখাও'");
        consume(TokenType.LPAREN, "Expected '('");
        ASTNode expr = parseExpression();
        consume(TokenType.RPAREN, "Expected ')'");
        consume(TokenType.SEMI, "Expected ';'");
        return new PrintNode(expr);
    }

    // block = statement*  (until RBRACE)
    private List<ASTNode> parseBlock() {
        List<ASTNode> stmts = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            stmts.add(parseStatement());
        }
        return stmts;
    }

    // expression = comparison
    private ASTNode parseExpression() {
        return parseComparison();
    }

    // comparison = addition ( (== | != | > | < | >= | <=) addition )*
    private ASTNode parseComparison() {
        ASTNode left = parseAddition();
        while (check(TokenType.EQ) || check(TokenType.NEQ) || check(TokenType.GT) ||
               check(TokenType.LT) || check(TokenType.GTE) || check(TokenType.LTE)) {
            Token op = advance();
            ASTNode right = parseAddition();
            left = new BinOpNode(left, op.lexeme, right);
        }
        return left;
    }

    // addition = multiplication ( (+ | -) multiplication )*
    private ASTNode parseAddition() {
        ASTNode left = parseMultiplication();
        while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            Token op = advance();
            ASTNode right = parseMultiplication();
            left = new BinOpNode(left, op.lexeme, right);
        }
        return left;
    }

    // multiplication = primary ( (* | /) primary )*
    private ASTNode parseMultiplication() {
        ASTNode left = parsePrimary();
        while (check(TokenType.MUL) || check(TokenType.DIV)) {
            Token op = advance();
            ASTNode right = parsePrimary();
            left = new BinOpNode(left, op.lexeme, right);
        }
        return left;
    }

    // primary = NUMBER | STRING | ID | TRUE | FALSE | LPAREN expression RPAREN
    private ASTNode parsePrimary() {
        if (check(TokenType.NUMBER)) {
            Token t = advance();
            return new NumberNode(SemanticAnalyzer.convertBanglaToEnglish(t.lexeme));
        }
        if (check(TokenType.STRING)) {
            Token t = advance();
            return new StringNode(t.lexeme);
        }
        if (check(TokenType.ID)) {
            Token t = advance();
            return new VarNode(t.lexeme);
        }
        if (check(TokenType.TRUE)) {
            advance();
            return new BooleanNode(true);
        }
        if (check(TokenType.FALSE)) {
            advance();
            return new BooleanNode(false);
        }
        if (check(TokenType.LPAREN)) {
            advance();
            ASTNode expr = parseExpression();
            consume(TokenType.RPAREN, "Expected ')'");
            return expr;
        }
        throw new RuntimeException("Syntax Error: unexpected token '" + peek().lexeme + "' at line " + peek().line);
    }

    // --- Helper methods ---
    private Token advance() { 
        if (!isAtEnd()) current++; 
        return tokens.get(current - 1); 
    }
    private boolean isAtEnd() { return peek().type == TokenType.EOF; }
    private Token peek() { return tokens.get(current); }
    private boolean check(TokenType type) { return !isAtEnd() && peek().type == type; }
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw new RuntimeException("Syntax Error: " + message + " but found '" + peek().lexeme + "' at line " + peek().line);
    }

    // Panic-mode synchronization: skip tokens until likely statement boundary.
    private void synchronize() {
        while (!isAtEnd()) {
            if (peek().type == TokenType.SEMI) {
                advance();
                return;
            }

            if (peek().type == TokenType.RBRACE) {
                return;
            }

            if (peek().type == TokenType.DHORO ||
                peek().type == TokenType.IF ||
                peek().type == TokenType.WHILE ||
                peek().type == TokenType.PRINT ||
                peek().type == TokenType.ID) {
                return;
            }

            advance();
        }
    }
}