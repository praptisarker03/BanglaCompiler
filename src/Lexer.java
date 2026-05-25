import java.util.*;

public class Lexer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("ধরো", TokenType.DHORO);
        keywords.put("সত্য", TokenType.TRUE);
        keywords.put("মিথ্যা", TokenType.FALSE);
        keywords.put("যদি", TokenType.IF);
        keywords.put("নাহলে", TokenType.ELSE);
        keywords.put("যতক্ষণ", TokenType.WHILE);
        keywords.put("দেখাও", TokenType.PRINT);
    }

    public Lexer(String source) {
        this.source = source;
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(TokenType.LPAREN); break;
            case ')': addToken(TokenType.RPAREN); break;
            case '{': addToken(TokenType.LBRACE); break;
            case '}': addToken(TokenType.RBRACE); break;
            case '+': addToken(TokenType.PLUS); break;
            case '-': addToken(TokenType.MINUS); break;
            case '*': addToken(TokenType.MUL); break;
            case '/': addToken(TokenType.DIV); break;
            case ';': addToken(TokenType.SEMI); break;
            case '=': 
                if (match('=')) {
                    addToken(TokenType.EQ);
                } else {
                    addToken(TokenType.ASSIGN);
                }
                break;
            case '!':
                if (match('=')) {
                    addToken(TokenType.NEQ);
                } else {
                    System.err.println("Line " + line + ": Expected '=' after '!'");
                }
                break;
            case '>':
                if (match('=')) {
                    addToken(TokenType.GTE);
                } else {
                    addToken(TokenType.GT);
                }
                break;
            case '"':
                stringLiteral();
                break;
            case '<':
                if (match('=')) {
                    addToken(TokenType.LTE);
                } else {
                    addToken(TokenType.LT);
                }
                break;
            case ' ':
            case '\r':
            case '\t': break;
            case '\n': line++; break;
            default:
                if (isBanglaDigit(c)) {
                    number();
                } else if (isBanglaLetter(c)) {
                    identifier();
                } else {
                    System.err.println("Line " + line + ": Unexpected character: " + c);
                }
                break;
        }
    }

    private void identifier() {
        while (isBanglaLetter(peek()) || isBanglaDigit(peek())) advance();
        String text = source.substring(start, current);
        TokenType type = keywords.getOrDefault(text, TokenType.ID);
        addToken(type);
    }

    private void number() {
        while (isBanglaDigit(peek())) advance();
        addToken(TokenType.NUMBER);
    }

    private void stringLiteral() {
        while (!isAtEnd() && peek() != '"') {
            if (peek() == '\n') line++;
            advance();
        }
        if (isAtEnd()) {
            System.err.println("Line " + line + ": Unterminated string literal.");
            return;
        }
        advance(); // closing quote
        String text = source.substring(start + 1, current - 1);
        tokens.add(new Token(TokenType.STRING, text, line));
    }

    private boolean isBanglaLetter(char c) {
        return (c >= '\u0980' && c <= '\u09FF');
    }

    private boolean isBanglaDigit(char c) {
        return (c >= '০' && c <= '৯');
    }

    private char advance() { return source.charAt(current++); }
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        current++;
        return true;
    }
    private void addToken(TokenType type) { tokens.add(new Token(type, source.substring(start, current), line)); }
    private boolean isAtEnd() { return current >= source.length(); }
    private char peek() { return isAtEnd() ? '\0' : source.charAt(current); }
}