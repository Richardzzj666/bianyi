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

    public Token nextToken() throws TokenizeError {
        it.readAll();

        // 跳过之前的所有空白字符
        skipSpaceCharacters();

        if (it.isEOF()) {
            return new Token(TokenType.EOF, "", it.currentPos(), it.currentPos());
        }

        char peek = it.peekChar();
        if (Character.isDigit(peek)) {
            return lexUIntDouble();
        } else if (Character.isAlphabetic(peek) || peek == '_') {
            return lexIdentOrKeyword();
        } else {
            return lexOperatorOrLiteral();
        }
    }

    private Token lexUIntDouble() throws TokenizeError {
        var tokenType = TokenType.UINT_LITERAL;
        var value = new StringBuilder();
        var startPos = it.currentPos();

        // 直到查看下一个字符不是数字为止:
        // -- 前进一个字符，并存储这个字符
        while (Character.isDigit(it.peekChar())) {
            value.append(it.nextChar());
        }
        if (it.peekChar() == '.') {
            tokenType = TokenType.DOUBLE_LITERAL;
            value.append(it.nextChar());
            while (Character.isDigit(it.peekChar())) {
                value.append(it.nextChar());
            }
            if (it.peekChar() == 'e' || it.peekChar() == 'E') {
                value.append('e');
                it.nextChar();
                while (Character.isDigit(it.peekChar())) {
                    value.append(it.nextChar());
                }
            }
        }

        try {
            if (tokenType == TokenType.UINT_LITERAL) {
                var uint = Integer.parseInt(value.toString());
                Token token = new Token(tokenType, uint, startPos, it.previousPos());
                return token;
            }else {
                BigDecimal bd = new BigDecimal(value.toString());
                var _double = Double.parseDouble(bd.toPlainString());
                Token token = new Token(tokenType, _double, startPos, it.previousPos());
                return token;
            }
        } catch (Exception e) {
            throw new Error("Not implemented");
        }
    }

    private Token lexIdentOrKeyword() throws TokenizeError {
        var value = new StringBuilder();
        var startPos = it.currentPos();

        while (Character.isDigit(it.peekChar()) || Character.isAlphabetic(it.peekChar()) || it.peekChar() == '_') {
            value.append(it.nextChar());
        }
        // 尝试将存储的字符串解释为关键字
        // -- 如果是关键字，则返回关键字类型的 token
        // -- 否则，返回标识符
        //
        // Token 的 Value 应填写标识符或关键字的字符串
        try {
            var toString = value.toString();
            Token token = checkIdentOrKeyword(toString, startPos, it.previousPos());
            return token;
        } catch (Exception e) {
            throw new Error("Not implemented");
        }

    }

    private Token checkIdentOrKeyword(String string, Pos startPos, Pos endPos) {
        TokenType tokenType = TokenType.IDENT;
        if (string.equals("fn")) tokenType = TokenType.FN_KW;
        if (string.equals("let")) tokenType = TokenType.LET_KW;
        if (string.equals("const")) tokenType = TokenType.CONST_KW;
        if (string.equals("as")) tokenType = TokenType.AS_KW;
        if (string.equals("while")) tokenType = TokenType.WHILE_KW;
        if (string.equals("if")) tokenType = TokenType.IF_KW;
        if (string.equals("else")) tokenType = TokenType.ELSE_KW;
        if (string.equals("return")) tokenType = TokenType.RETURN_KW;
        if (string.equals("break")) tokenType = TokenType.BREAK_KW;
        if (string.equals("continue")) tokenType = TokenType.CONTINUE_KW;
        return new Token(tokenType, string, startPos, endPos);
    }

    private Token lexOperatorOrLiteral() throws TokenizeError {
        var startPos = it.currentPos();
        var value = new StringBuilder();
        switch (it.nextChar()) {
            case '+':
                return new Token(TokenType.PLUS, '+', startPos, it.currentPos());
            case '-':
                if (it.peekChar() == '>') {
                    it.nextChar();
                    return new Token(TokenType.ARROW, "->", startPos, it.currentPos());
                }
                return new Token(TokenType.MINUS, '-', startPos, it.currentPos());
            case '*':
                return new Token(TokenType.MUL, '*', startPos, it.currentPos());
            case '/':
                if (it.peekChar() == '/' && (!it.isEOF())) {
                    while (it.peekChar() != '\n') {
                        it.nextChar();
                    }
                    it.nextChar();
                    return new Token(TokenType.COMMENT, "//", startPos, it.currentPos());
                }
                return new Token(TokenType.DIV, '/', startPos, it.currentPos());
            case '=':
                if (it.peekChar() == '=') {
                    it.nextChar();
                    return new Token(TokenType.EQ, "==", startPos, it.currentPos());
                }
                return new Token(TokenType.ASSIGN, '=', startPos, it.currentPos());
            case '!':
                if (it.peekChar() == '=') {
                    it.nextChar();
                    return new Token(TokenType.NEQ, "!=", startPos, it.currentPos());
                }else throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
            case '<':
                if (it.peekChar() == '=') {
                    it.nextChar();
                    return new Token(TokenType.LE, "<=", startPos, it.currentPos());
                }else
                return new Token(TokenType.LT, '<', startPos, it.currentPos());
            case '>':
                if (it.peekChar() == '=') {
                    it.nextChar();
                    return new Token(TokenType.GE, ">=", startPos, it.currentPos());
                }else
                    return new Token(TokenType.GT, '>', startPos, it.currentPos());
            case '(':
                return new Token(TokenType.L_PAREN, '(', startPos, it.currentPos());
            case ')':
                return new Token(TokenType.R_PAREN, ')', startPos, it.currentPos());
            case '{':
                return new Token(TokenType.L_BRACE, '{', startPos, it.currentPos());
            case '}':
                return new Token(TokenType.R_BRACE, '}', startPos, it.currentPos());
            case ',':
                return new Token(TokenType.COMMA, ',', startPos, it.currentPos());
            case ':':
                return new Token(TokenType.COLON, ':', startPos, it.currentPos());
            case ';':
                return new Token(TokenType.SEMICOLON, ';', startPos, it.currentPos());
            case '\"':
                while (it.peekChar() != '\"') {
                    if (it.isEOF()) throw new TokenizeError(ErrorCode.InvalidInput, startPos);
                    char _char = it.nextChar();
                    if (_char == '\\') {
                        _char = it.nextChar();
                        value.append(escapeSequence(_char, startPos));
                    }else {
                        value.append(_char);
                    }
                }
                if (it.peekChar() == '\"') {
                    it.nextChar();
                    return new Token(TokenType.STRING_LITERAL, value, startPos, it.currentPos());
                } else throw new TokenizeError(ErrorCode.InvalidInput, startPos);
            case '\'':
                if(it.isEOF() || it.peekChar() == '\'') throw new TokenizeError(ErrorCode.InvalidInput, startPos);
                char _char = it.nextChar();
                if(_char == '\\') {
                    _char = it.nextChar();
                    _char = escapeSequence(_char, startPos);
                }
                if(it.peekChar() != '\'') throw new TokenizeError(ErrorCode.InvalidInput, startPos);
                it.nextChar();
                return new Token(TokenType.CHAR_LITERAL, _char, startPos, it.currentPos());
            default:
                throw new TokenizeError(ErrorCode.InvalidInput, startPos);
        }
    }

    private void skipSpaceCharacters() {
        while (!it.isEOF() && Character.isWhitespace(it.peekChar())) {
            it.nextChar();
        }
    }

    private char escapeSequence(char _char, Pos startPos) throws TokenizeError{
        switch (_char) {
            case '\\':
                _char = '\\';
                break;
            case '\'':
                _char = '\'';
                break;
            case '\"':
                _char = '\"';
                break;
            case 'n':
                _char = '\n';
                break;
            case 't':
                _char = '\t';
                break;
            case 'r':
                _char = '\r';
                break;
            default:
                throw new TokenizeError(ErrorCode.InvalidInput, startPos);
        }
        return _char;
    }
}
