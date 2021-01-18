package miniplc0java.tokenizer;

import miniplc0java.error.TokenizeError;
import miniplc0java.error.ErrorCode;
import miniplc0java.util.Pos;

import java.math.BigDecimal;

public class Tokenizer {

    private StringIter it;

    public Tokenizer(StringIter it) {
        this.it = it;
    }

    // 这里本来是想实现 Iterator<Token> 的，但是 Iterator 不允许抛异常，于是就这样了
    /**
     * 获取下一个 Token
     * 
     * @return
     * @throws TokenizeError 如果解析有异常则抛出
     */
    public Token nextToken() throws TokenizeError {
        it.readAll();

        // 跳过之前的所有空白字符
        skipSpaceCharacters();

        if (it.isEOF()) {
            return new Token(TokenType.EOF, "", it.currentPos(), it.currentPos());
        }

        char peek = it.peekChar();
        if (Character.isDigit(peek)) {
            return lexUIntOrDouble();
        } else if (Character.isAlphabetic(peek) || peek == '_') {
            return lexIdentOrKeyword();
        } else {
            return lexOperatorOrOthers();
        }
    }

    private Token lexUIntOrDouble() throws TokenizeError {
        // 请填空：
        // 直到查看下一个字符不是数字为止:
        // -- 前进一个字符，并存储这个字符
        //
        // 解析存储的字符串为无符号整数
        // 解析成功则返回无符号整数类型的token，否则返回编译错误
        //
        // Token 的 Value 应填写数字的值

        Pos start_pos = it.currentPos();
        var token_type = TokenType.UINT_LITERAL;
        String str = "";

        while(Character.isDigit(it.peekChar())) {
            str += it.nextChar();
        }

        if (it.peekChar() == '.'){
            token_type = TokenType.DOUBLE_LITERAL;
            str += it.nextChar();
            if (!Character.isDigit(it.peekChar())) {
                throw new Error("Not implemented");
            }
            while(Character.isDigit(it.peekChar())) {
                str += it.nextChar();
            }
            if (it.peekChar() == 'e' || it.peekChar() == 'E') {
                str += 'e';
                it.nextChar();
                if (it.peekChar() == '+' || it.peekChar() == '-') {
                    str += it.nextChar();
                }
                if (!Character.isDigit(it.peekChar())) {
                    throw new Error("Not implemented");
                }
                while (Character.isDigit(it.peekChar())) {
                    str += it.nextChar();
                }
            }
        }

        Pos end_pos = it.currentPos();

        if (token_type == TokenType.UINT_LITERAL) {
            return new Token(token_type, Long.valueOf(str), start_pos, end_pos);
        }
        else {
            BigDecimal double_value = new BigDecimal(str);
            return new Token(token_type, Double.valueOf(double_value.toPlainString()), start_pos, end_pos);
        }
    }

    private Token lexIdentOrKeyword() throws TokenizeError {
        // 请填空：
        // 直到查看下一个字符不是数字或字母为止:
        // -- 前进一个字符，并存储这个字符
        //
        // 尝试将存储的字符串解释为关键字
        // -- 如果是关键字，则返回关键字类型的 token
        // -- 否则，返回标识符
        //
        // Token 的 Value 应填写标识符或关键字的字符串

        Pos start_pos = it.currentPos();
        String str = new String();

        while(Character.isAlphabetic(it.peekChar()) || Character.isDigit(it.peekChar()) || it.peekChar() == '_') {
            str += it.nextChar();
        }

        Pos end_pos = it.currentPos();

        if("fn".equals(str)) {
            return new Token(TokenType.FN_KW, str, start_pos, end_pos);
        }
        else if("let".equals(str)) {
            return new Token(TokenType.LET_KW, str, start_pos, end_pos);
        }
        else if("const".equals(str)) {
            return new Token(TokenType.CONST_KW, str, start_pos, end_pos);
        }
        else if("as".equals(str)) {
            return new Token(TokenType.AS_KW, str, start_pos, end_pos);
        }
        else if("while".equals(str)) {
            return new Token(TokenType.WHILE_KW, str, start_pos, end_pos);
        }
        else if("if".equals(str)) {
            return new Token(TokenType.IF_KW, str, start_pos, end_pos);
        }
        else if("else".equals(str)) {
            return new Token(TokenType.ELSE_KW, str, start_pos, end_pos);
        }
        else if("return".equals(str)) {
            return new Token(TokenType.RETURN_KW, str, start_pos, end_pos);
        }
        else if("break".equals(str)) {
            return new Token(TokenType.BREAK_KW, str, start_pos, end_pos);
        }
        else if("continue".equals(str)) {
            return new Token(TokenType.CONTINUE_KW, str, start_pos, end_pos);
        }
        else {
            return new Token(TokenType.IDENT, str, start_pos, end_pos);
        }
    }

    private Token lexOperatorOrOthers() throws TokenizeError {
        Pos start_pos = it.currentPos();
        switch (it.nextChar()) {
            case '+':
                return new Token(TokenType.PLUS, '+', start_pos, it.currentPos());
            case '-':
                if (it.peekChar() == '>') {
                    it.nextChar();
                    return new Token(TokenType.ARROW, "->", start_pos, it.currentPos());
                }
                return new Token(TokenType.MINUS, '-', start_pos, it.currentPos());
            case '*':
                return new Token(TokenType.MUL, '*', start_pos, it.currentPos());
            case '/':
                if (it.peekChar() == '/') {
                    String str = "//";
                    while (it.peekChar() != '\n') {
                        str += it.nextChar();
                    }
                    str += it.nextChar();
                    return new Token(TokenType.COMMENT, str, start_pos, it.currentPos());
                }
                return new Token(TokenType.DIV, '/', start_pos, it.currentPos());
            case '=':
                if (it.peekChar() == '=') {
                    it.nextChar();
                    return new Token(TokenType.EQ, "==", start_pos, it.currentPos());
                }
                return new Token(TokenType.ASSIGN, '=', start_pos, it.currentPos());
            case '!':
                if (it.peekChar() == '=') {
                    it.nextChar();
                    return new Token(TokenType.NEQ, "!=", start_pos, it.currentPos());
                }
                throw new TokenizeError(ErrorCode.InvalidInput, start_pos);
            case '<':
                if (it.peekChar() == '=') {
                    it.nextChar();
                    return new Token(TokenType.LE, "<=", start_pos, it.currentPos());
                }
                return new Token(TokenType.LT, '<', start_pos, it.currentPos());
            case '>':
                if (it.peekChar() == '=') {
                    it.nextChar();
                    return new Token(TokenType.GE, ">=", start_pos, it.currentPos());
                }
                return new Token(TokenType.GT, '>', start_pos, it.currentPos());
            case '(':
                return new Token(TokenType.L_PAREN, '(', start_pos, it.currentPos());
            case ')':
                return new Token(TokenType.R_PAREN, ')', start_pos, it.currentPos());
            case '{':
                return new Token(TokenType.L_BRACE, '{', start_pos, it.currentPos());
            case '}':
                return new Token(TokenType.R_BRACE, '}', start_pos, it.currentPos());
            case ',':
                return new Token(TokenType.COMMA, ',', start_pos, it.currentPos());
            case ':':
                return new Token(TokenType.COLON, ':', start_pos, it.currentPos());
            case ';':
                return new Token(TokenType.SEMICOLON, ';', start_pos, it.currentPos());
            case '\"':
                String str = "";
                while (it.peekChar() != '\"') {
                    if (it.isEOF()) {
                        throw new TokenizeError(ErrorCode.InvalidInput, start_pos);
                    }
                    if (it.peekChar() == '\\') {
                        it.nextChar();
                        switch (it.nextChar()) {
                            case '\\':
                                str += '\\';
                                break;
                            case '\'':
                                str += '\'';
                                break;
                            case '\"':
                                str += '\"';
                                break;
                            case 'n':
                                str += '\n';
                                break;
                            case 't':
                                str += '\t';
                                break;
                            case 'r':
                                str += '\r';
                                break;
                            default:
                                throw new TokenizeError(ErrorCode.InvalidInput, start_pos);
                        }
                    } else {
                        str += it.nextChar();
                    }
                }
                it.nextChar();
                return new Token(TokenType.STRING_LITERAL, str, start_pos, it.currentPos());
            case '\'':
                if (it.isEOF()) {
                    throw new TokenizeError(ErrorCode.InvalidInput, start_pos);
                }
                char value;
                if (it.peekChar() == '\\') {
                    it.nextChar();
                    switch (it.nextChar()) {
                        case '\\':
                            value = '\\';
                            break;
                        case '\'':
                            value = '\'';
                            break;
                        case '\"':
                            value = '\"';
                            break;
                        case 'n':
                            value = '\n';
                            break;
                        case 't':
                            value = '\t';
                            break;
                        case 'r':
                            value = '\r';
                            break;
                        default:
                            throw new TokenizeError(ErrorCode.InvalidInput, start_pos);
                    }
                } else if (it.peekChar() == '\'') {
                    value = 0;
                } else {
                    value = it.nextChar();
                }
                it.nextChar();
                //char作为int处理
                return new Token(TokenType.UINT_LITERAL, (int) value, start_pos, it.currentPos());
            default:
                throw new TokenizeError(ErrorCode.InvalidInput, start_pos);
        }
    }

    private void skipSpaceCharacters() {
        while (!it.isEOF() && Character.isWhitespace(it.peekChar())) {
            it.nextChar();
        }
    }
}
