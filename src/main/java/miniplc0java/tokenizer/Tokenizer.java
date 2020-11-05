package miniplc0java.tokenizer;

import miniplc0java.error.TokenizeError;
import miniplc0java.error.ErrorCode;
import miniplc0java.util.Pos;

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
            return lexUInt();
        } else if (Character.isAlphabetic(peek)) {
            return lexIdentOrKeyword();
        } else {
            return lexOperatorOrUnknown();
        }
    }

    private Token lexUInt() throws TokenizeError {
        // 请填空：
        // 直到查看下一个字符不是数字为止:
        // -- 前进一个字符，并存储这个字符
        //
        // 解析存储的字符串为无符号整数
        // 解析成功则返回无符号整数类型的token，否则返回编译错误
        //
        // Token 的 Value 应填写数字的值

        int i;
        char []str_char = new char[100];
        Pos start_pos = it.currentPos();

        for(i = 0; Character.isDigit(it.peekChar()); i++) {
            str_char[i] = it.nextChar();
        }

        Pos end_pos = it.currentPos();
        str_char[i + 1] = '\0';
        String str = new String(str_char);

        return new Token(TokenType.Uint, Integer.valueOf(str), start_pos, end_pos);

        // throw new Error("Not implemented");
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

        int i;
        char []str_char = new char[100];
        Pos start_pos = it.currentPos();

        for(i = 0; Character.isLetter(it.peekChar()) || Character.isDigit(it.peekChar()); i++) {
            str_char[i] = it.nextChar();
        }

        Pos end_pos = it.currentPos();
        str_char[i + 1] = '\0';
        String str = new String(str_char);

        if("BEGIN".equals(str) || "begin".equals(str)) {
            return new Token(TokenType.Begin, str, start_pos, end_pos);
        }
        else if("END".equals(str) || "end".equals(str)) {
            return new Token(TokenType.End, str, start_pos, end_pos);
        }
        else if("VAR".equals(str) || "var".equals(str)) {
            return new Token(TokenType.Var, str, start_pos, end_pos);
        }
        else if("CONST".equals(str) || "const".equals(str)) {
            return new Token(TokenType.Const, str, start_pos, end_pos);
        }
        else if("PRINT".equals(str) || "print".equals(str)) {
            return new Token(TokenType.Print, str, start_pos, end_pos);
        }
        else {
            return new Token(TokenType.Ident, str, start_pos, end_pos);
        }

        // throw new Error("Not implemented");
    }

    private Token lexOperatorOrUnknown() throws TokenizeError {
        switch (it.nextChar()) {
            case '+':
                return new Token(TokenType.Plus, '+', it.previousPos(), it.currentPos());
            case '-':
                return new Token(TokenType.Minus, '-', it.previousPos(), it.currentPos());
            case '*':
                return new Token(TokenType.Mult, '*', it.previousPos(), it.currentPos());
            case '/':
                return new Token(TokenType.Div, '/', it.previousPos(), it.currentPos());
            case '=':
                return new Token(TokenType.Equal, '=', it.previousPos(), it.currentPos());
            case ';':
                return new Token(TokenType.Semicolon, ';', it.previousPos(), it.currentPos());
            case '(':
                return new Token(TokenType.LParen, '(', it.previousPos(), it.currentPos());
            case ')':
                return new Token(TokenType.RParen, ')', it.previousPos(), it.currentPos());
            default:
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
    }

    private void skipSpaceCharacters() {
        while (!it.isEOF() && Character.isWhitespace(it.peekChar())) {
            it.nextChar();
        }
    }
}
