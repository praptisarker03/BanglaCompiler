import java.util.List;

public abstract class ASTNode {}

class NumberNode extends ASTNode {
    int value;
    NumberNode(int value) { this.value = value; }
}

class StringNode extends ASTNode {
    String value;
    StringNode(String value) { this.value = value; }
}

class VarNode extends ASTNode {
    String name;
    VarNode(String name) { this.name = name; }
}

class BinOpNode extends ASTNode {
    ASTNode left;
    String operator;
    ASTNode right;
    BinOpNode(ASTNode left, String operator, ASTNode right) {
        this.left = left; this.operator = operator; this.right = right;
    }
}

class AssignNode extends ASTNode {
    String name;
    ASTNode expr;
    AssignNode(String name, ASTNode expr) { this.name = name; this.expr = expr; }
}

class BooleanNode extends ASTNode {
    boolean value;
    BooleanNode(boolean value) { this.value = value; }
}

class IfNode extends ASTNode {
    ASTNode condition;
    List<ASTNode> thenBody;
    List<ASTNode> elseBody; // can be null if no else
    IfNode(ASTNode condition, List<ASTNode> thenBody, List<ASTNode> elseBody) {
        this.condition = condition;
        this.thenBody = thenBody;
        this.elseBody = elseBody;
    }
}

class WhileNode extends ASTNode {
    ASTNode condition;
    List<ASTNode> body;
    WhileNode(ASTNode condition, List<ASTNode> body) {
        this.condition = condition;
        this.body = body;
    }
}

class PrintNode extends ASTNode {
    ASTNode expr;
    PrintNode(ASTNode expr) { this.expr = expr; }
}