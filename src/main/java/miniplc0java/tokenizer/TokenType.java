package miniplc0java.tokenizer;

public enum TokenType {
    /** 关键字 */
    FN_KW,
    LET_KW,
    CONST_KW,
    AS_KW,
    WHILE_KW,
    IF_KW,
    ELSE_KW,
    RETURN_KW,
    BREAK_KW,
    CONTINUE_KW,
    /** 空 */
    None,
    /** 字面量 */
    UINT_LITERAL,
    DOUBLE_LITERAL,
    STRING_LITERAL,
    CHAR_LITERAL,
    /** 标识符 */
    IDENT,
    /** 符号*/
    PLUS,
    MINUS,
    MUL,
    DIV,
    ASSIGN,
    EQ,
    NEQ,
    LT,
    GT,
    LE,
    GE,
    L_PAREN,
    R_PAREN,
    L_BRACE,
    R_BRACE,
    ARROW,
    COMMA,
    COLON,
    SEMICOLON,
    COMMENT,
    EOF;

    @Override
    public String toString() {
        switch (this) {
            case FN_KW:
                return "fn";
            case LET_KW:
                return "let";
            case CONST_KW:
                return "const";
            case AS_KW:
                return "as";
            case WHILE_KW:
                return "while";
            case IF_KW:
                return "if";
            case ELSE_KW:
                return "else";
            case RETURN_KW:
                return "return";
            case BREAK_KW:
                return "break";
            case CONTINUE_KW:
                return "continue";
            case None:
                return "NullToken";
            case UINT_LITERAL:
                return "unit";
            case DOUBLE_LITERAL:
                return "double";
            case STRING_LITERAL:
                return "string";
            case EOF:
                return "EOF";
            case CHAR_LITERAL:
                return "char";
            case IDENT:
                return "ident";
            case ASSIGN:
                return "assign";
            case EQ:
                return "equal";
            case NEQ:
                return "notequal";
            case MINUS:
                return "MinusSign";
            case MUL:
                return "MultiplicationSign";
            case PLUS:
                return "PlusSign";
            case DIV:
                return "DivSign";
            case LT:
                return "lower";
            case GT:
                return "greater";
            case LE:
                return "lower_equal";
            case GE:
                return "greater_equal";
            case L_PAREN:
                return "L_PAREN";
            case R_PAREN:
                return "R_PAREN";
            case L_BRACE:
                return "L_BRACE";
            case R_BRACE:
                return "R_BRACE";
            case ARROW:
                return "UnsignedInteger";
            case COMMA:
                return "COMMA";
            case COLON:
                return "COLON";
            case SEMICOLON:
                return "SEMICOLON";
            case COMMENT:
                return "COMMENT";
            default:
                return "InvalidToken";
        }
    }
}
