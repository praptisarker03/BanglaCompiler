public class Instruction {
    public String type;
    public String result;
    public String arg1;
    public String operator;
    public String arg2;

    public Instruction(String result, String arg1, String operator, String arg2) {
        this.type = (operator != null) ? "BINOP" : "ASSIGN";
        this.result = result;
        this.arg1 = arg1;
        this.operator = operator;
        this.arg2 = arg2;
    }

    public Instruction(String type, String arg1, String arg2) {
        this.type = type;
        this.result = null;
        this.arg1 = arg1;
        this.operator = null;
        this.arg2 = arg2;
    }

    @Override
    public String toString() {
        switch (type) {
            case "LABEL":
                return arg1 + ":";
            case "GOTO":
                return "GOTO " + arg1;
            case "IF_FALSE_GOTO":
                return "IF_FALSE " + arg1 + " GOTO " + arg2;
            case "PRINT":
                return "PRINT " + arg1;
            default:
                if (operator == null && arg2 == null) {
                    return result + " = " + arg1;
                }
                return result + " = " + arg1 + " " + operator + " " + arg2;
        }
    }
}
