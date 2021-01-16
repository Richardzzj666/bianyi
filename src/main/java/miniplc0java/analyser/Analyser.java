package miniplc0java.analyser;

import miniplc0java.error.AnalyzeError;
import miniplc0java.error.CompileError;
import miniplc0java.error.ErrorCode;
import miniplc0java.error.ExpectedTokenError;
import miniplc0java.error.TokenizeError;
import miniplc0java.instruction.Global;
import miniplc0java.instruction.Function;
import miniplc0java.tokenizer.Token;
import miniplc0java.tokenizer.TokenType;
import miniplc0java.tokenizer.Tokenizer;
import miniplc0java.util.Pos;

import java.util.*;

public final class Analyser {

    Tokenizer tokenizer;
    ArrayList<Global> globals;
    ArrayList<Function> functions;

    Token peekedToken = null;

    HashMap<String, SymbolEntry> global_symbol_table = new HashMap<>();
    ArrayList<HashMap> function_symbol_tables = new ArrayList<>();

    public Analyser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.globals = new ArrayList<>();
        this.functions = new ArrayList<>();
    }

    public void analyse() throws CompileError {
        analyseProgram();
    }

    private Token peek() throws TokenizeError {
        if (peekedToken == null) {
            peekedToken = tokenizer.nextToken();
        }
        return peekedToken;
    }

    private Token next() throws TokenizeError {
        if (peekedToken != null) {
            var token = peekedToken;
            peekedToken = null;
            return token;
        } else {
            return tokenizer.nextToken();
        }
    }

    private boolean check(TokenType tt) throws TokenizeError {
        var token = peek();
        return token.getTokenType() == tt;
    }

    private Token expect(TokenType tt) throws CompileError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            throw new ExpectedTokenError(tt, token);
        }
    }

    private void addSymbol(String name, boolean isConstant, boolean isInitialized, boolean isFunction, Pos curPos,
                           HashMap<String, SymbolEntry> table, byte[] value, int index, String type) throws AnalyzeError {
        if (table.get(name) != null) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        } else {
            table.put(name, new SymbolEntry(isConstant, isInitialized, isFunction, value, index, type));
        }
    }

    private void analyseProgram() throws CompileError {
        while (!check(TokenType.EOF)) {
            if (check(TokenType.FN_KW)) {
                analyseFunction();
            } else {
                analyseGlobalDeclareStatement();
            }
        }
        expect(TokenType.EOF);
    }

    private void analyseGlobalDeclareStatement() throws CompileError {
        if (check(TokenType.CONST_KW)) {
            analyseConstDeclareStatement(this.global_symbol_table, true);
        } else if (check(TokenType.LET_KW)) {
            analyseLetDeclareStatement(this.global_symbol_table, true);
        } else {
            throw new AnalyzeError(ErrorCode.ExpectedToken, peek().getStartPos());
        }
    }

    private void analyseConstDeclareStatement(HashMap<String, SymbolEntry> table, boolean is_global) throws CompileError {
        expect(TokenType.CONST_KW);
        String name = (String) expect(TokenType.IDENT).getValue();
        expect(TokenType.COLON);
        String type = (String) expect(TokenType.IDENT).getValue();
        expect(TokenType.ASSIGN);
        byte[] value = analyseExpresion(type);
        addSymbol(name, true, true, false, peek().getStartPos(), table, value, table.size(), type);
        expect(TokenType.SEMICOLON);

        if (is_global) {
            this.globals.add(new Global(true, value.length, value));
        }
    }

    private void analyseLetDeclareStatement(HashMap<String, SymbolEntry> table, boolean is_global) throws CompileError {
        expect(TokenType.LET_KW);
        String name = (String) expect(TokenType.IDENT).getValue();
        expect(TokenType.COLON);
        String type = (String) expect(TokenType.IDENT).getValue();
        byte[] value = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        boolean is_initialized = false;
        if (check(TokenType.ASSIGN)) {
            value = analyseExpresion(type);
            is_initialized = true;
        }
        addSymbol(name, false, is_initialized, false, peek().getStartPos(), table, value, table.size(), type);
        expect(TokenType.SEMICOLON);

        if (is_global) {
            this.globals.add(new Global(false, value.length, value));
        }
    }

    private byte[] analyseExpresion(String type) {
        byte[] value = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

        return value;
    }











    private void analyseFunction() throws CompileError {
        expect(TokenType.FN_KW);
        String function_name = (String) expect(TokenType.IDENT).getValue();
        expect(TokenType.L_PAREN);
        int name = this.globals.size();
        int param_slot = analyseFunctionParamList(name);
        expect(TokenType.L_PAREN);
        expect(TokenType.ARROW);
        String return_type = (String) expect(TokenType.IDENT).getValue();
        int ret_slot;
        switch (return_type) {
            case "void":
                ret_slot = 0;
                break;
            case "int":
            case "double":
                ret_slot = 1;
                break;
            default:
                throw new AnalyzeError(ErrorCode.NotDeclared, peek().getStartPos());
        }
        this.globals.add(new Global(false, function_name.length(), function_name.getBytes()));
        this.functions.add(new Function(name, ret_slot, param_slot, 0));
        analyseFunctionBlockStatement();
    }

    private int analyseFunctionParamList(int function_name) throws CompileError {
        int count = 0;
        HashMap<String, SymbolEntry> function_table = new HashMap<>();
        while (!check(TokenType.R_PAREN)) {
            if (count > 0) {
                expect(TokenType.COMMA);
            }
            count++;
            analyseFunctionParam(function_table);
        }
        this.function_symbol_tables.add(function_table);
        return count;
    }

    private void analyseFunctionBlockStatement() throws CompileError {

    }

    private void analyseFunctionParam(HashMap<String, SymbolEntry> function_table) throws CompileError {

    }

//    /**
//     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回这个 token
//     *
//     * @param tt 类型
//     * @return 如果匹配则返回这个 token，否则返回 null
//     * @throws TokenizeError
//     */
//    private Token nextIf(TokenType tt) throws TokenizeError {
//        var token = peek();
//        if (token.getTokenType() == tt) {
//            return next();
//        } else {
//            return null;
//        }
//    }
//    /**
//     * 获取下一个变量的栈偏移
//     *
//     * @return
//     */
//    private int getNextVariableOffset() {
//        return this.nextOffset++;
//    }
//
//
//    /**
//     * 设置符号为已赋值
//     *
//     * @param name   符号名称
//     * @param curPos 当前位置（报错用）
//     * @throws AnalyzeError 如果未定义则抛异常
//     */
//    private void initializeSymbol(String name, Pos curPos) throws AnalyzeError {
//        var entry = this.symbol_table.get(name);
//        if (entry == null) {
//            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
//        } else {
//            entry.setInitialized(true);
//        }
//    }
//
//    /**
//     * 获取变量在栈上的偏移
//     *
//     * @param name   符号名
//     * @param curPos 当前位置（报错用）
//     * @return 栈偏移
//     * @throws AnalyzeError
//     */
//    private int getOffset(String name, Pos curPos) throws AnalyzeError {
//        var entry = this.symbol_table.get(name);
//        if (entry == null) {
//            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
//        } else {
//            return entry.getStackOffset();
//        }
//    }
//
//    /**
//     * 获取变量是否是常量
//     *
//     * @param name   符号名
//     * @param curPos 当前位置（报错用）
//     * @return 是否为常量
//     * @throws AnalyzeError
//     */
//    private boolean isConstant(String name, Pos curPos) throws AnalyzeError {
//        var entry = this.symbol_table.get(name);
//        if (entry == null) {
//            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
//        } else {
//            return entry.isConstant();
//        }
//    }
//    private void analyseMain() throws CompileError {
//        // 主过程 -> 常量声明 变量声明 语句序列
//
//        while (!check(TokenType.End)) {
//            if (check(TokenType.Const)) {
//                analyseConstantDeclaration();
//            } else if (check(TokenType.Var)) {
//                analyseVariableDeclaration();
//            } else {
//                analyseStatementSequence();
//            }
//        }
//
//        // throw new Error("Not implemented");
//    }

//    private void analyseConstantDeclaration() throws CompileError {
//        // 示例函数，示例如何解析常量声明
//        // 常量声明 -> 常量声明语句*
//
//        // 如果下一个 token 是 const 就继续
//        while (nextIf(TokenType.Const) != null) {
//            // 常量声明语句 -> 'const' 变量名 '=' 常表达式 ';'
//
//            // 变量名
//            var nameToken = expect(TokenType.Ident);
//
//            // 加入符号表
//            String name = (String) nameToken.getValue();
//            addSymbol(name, true, true, nameToken.getStartPos());
//
//            // 等于号
//            expect(TokenType.Equal);
//
//            // 常表达式
//            var value = analyseConstantExpression();
//
//            // 分号
//            expect(TokenType.Semicolon);
//
//            // 这里把常量值直接放进栈里，位置和符号表记录的一样。
//            // 更高级的程序还可以把常量的值记录下来，遇到相应的变量直接替换成这个常数值，
//            // 我们这里就先不这么干了。
//            instructions.add(new Instruction(Operation.LIT, value));
//        }
//    }
//
//    private void analyseVariableDeclaration() throws CompileError {
//        // 变量声明 -> 变量声明语句*
//
//        // 如果下一个 token 是 var 就继续
//        while (nextIf(TokenType.Var) != null) {
//            // 变量声明语句 -> 'var' 变量名 ('=' 表达式)? ';'
//
//            // 变量名
//            var nameToken = expect(TokenType.Ident);
//
//            // 变量初始化了吗
//            boolean initialized = false;
//
//            // 下个 token 是等于号吗？如果是的话分析初始化
//            if(nextIf(TokenType.Equal) != null){
//                initialized = true;
//                // 分析初始化的表达式
//                analyseExpression();
//            }
//
//            // 分号
//            expect(TokenType.Semicolon);
//
//            // 加入符号表，请填写名字和当前位置（报错用）
//            String name = (String) nameToken.getValue();
//            addSymbol(name, initialized, false, nameToken.getStartPos());
//
//            // 如果没有初始化的话在栈里推入一个初始值
//            if (!initialized) {
//                instructions.add(new Instruction(Operation.LIT, 0));
//            }
//        }
//    }
//
//    private void analyseStatementSequence() throws CompileError {
//        // 语句序列 -> 语句*
//        // 语句 -> 赋值语句 | 输出语句 | 空语句
//
//        while (true) {
//            // 如果下一个 token 是……
//            var peeked = peek();
//            if (peeked.getTokenType() == TokenType.Ident) {
//                // 调用相应的分析函数
//                analyseAssignmentStatement();
//                // 如果遇到其他非终结符的 FIRST 集呢？
//            } else if (peeked.getTokenType() == TokenType.Print){
//                analyseOutputStatement();
//            } else if (peeked.getTokenType() == TokenType.Semicolon){
//                expect(TokenType.Semicolon);
//                continue;
//            } else {
//                // 都不是，摸了
//                break;
//            }
//        }
//        // throw new Error("Not implemented");
//    }
//
//    private int analyseConstantExpression() throws CompileError {
//        // 常表达式 -> 符号? 无符号整数
//        boolean negative = false;
//        if (nextIf(TokenType.Plus) != null) {
//            negative = false;
//        } else if (nextIf(TokenType.Minus) != null) {
//            negative = true;
//        }
//
//        var token = expect(TokenType.Uint);
//
//        int value = (int) token.getValue();
//        if (negative) {
//            value = -value;
//        }
//
//        return value;
//    }
//
//    private void analyseExpression() throws CompileError {
//        // 表达式 -> 项 (加法运算符 项)*
//        // 项
//        analyseItem();
//
//        while (true) {
//            // 预读可能是运算符的 token
//            Token op = peek();
//            if (op.getTokenType() != TokenType.Plus && op.getTokenType() != TokenType.Minus) {
//                break;
//            }
//
//            // 运算符
//            next();
//
//            // 项
//            analyseItem();
//
//            // 生成代码
//            if (op.getTokenType() == TokenType.Plus) {
//                instructions.add(new Instruction(Operation.ADD));
//            } else if (op.getTokenType() == TokenType.Minus) {
//                instructions.add(new Instruction(Operation.SUB));
//            }
//        }
//    }
//
//    private void analyseAssignmentStatement() throws CompileError {
//        // 赋值语句 -> 标识符 '=' 表达式 ';'
//
//        // 分析这个语句
//
//        // 标识符是什么？
//        var nameToken = expect(TokenType.Ident);
//        String name = (String) nameToken.getValue();
//        var symbol = symbol_table.get(name);
//        if (symbol == null) {
//            // 没有这个标识符
//            throw new AnalyzeError(ErrorCode.NotDeclared, nameToken.getStartPos());
//        } else if (symbol.isConstant) {
//            // 标识符是常量
//            throw new AnalyzeError(ErrorCode.AssignToConstant, nameToken.getStartPos());
//        }
//        // 设置符号已初始化
//        initializeSymbol(name, nameToken.getStartPos());
//        expect(TokenType.Equal);
//        analyseExpression();
//
//        // 把结果保存
//        var offset = getOffset(name, nameToken.getStartPos());
//        instructions.add(new Instruction(Operation.STO, offset));
//        expect(TokenType.Semicolon);
//    }
//
//    private void analyseOutputStatement() throws CompileError {
//        // 输出语句 -> 'print' '(' 表达式 ')' ';'
//
//        expect(TokenType.Print);
//        expect(TokenType.LParen);
//
//        analyseExpression();
//
//        expect(TokenType.RParen);
//        expect(TokenType.Semicolon);
//
//        instructions.add(new Instruction(Operation.WRT));
//    }
//
//    private void analyseItem() throws CompileError {
//        // 项 -> 因子 (乘法运算符 因子)*
//
//        // 因子
//        analyseFactor();
//
//        while (true) {
//            // 预读可能是运算符的 token
//            Token op = peek();
//            if (op.getTokenType() != TokenType.Mult && op.getTokenType() != TokenType.Div) {
//                break;
//            }
//
//            // 运算符
//            next();
//
//            // 因子
//            analyseFactor();
//
//            // 生成代码
//            if (op.getTokenType() == TokenType.Mult) {
//                instructions.add(new Instruction(Operation.MUL));
//            } else if (op.getTokenType() == TokenType.Div) {
//                instructions.add(new Instruction(Operation.DIV));
//            }
//        }
//    }
//
//    private void analyseFactor() throws CompileError {
//        // 因子 -> 符号? (标识符 | 无符号整数 | '(' 表达式 ')')
//
//        boolean negate;
//        if (nextIf(TokenType.Minus) != null) {
//            negate = true;
//            // 计算结果需要被 0 减
//            instructions.add(new Instruction(Operation.LIT, 0));
//        } else {
//            nextIf(TokenType.Plus);
//            negate = false;
//        }
//
//        if (check(TokenType.Ident)) {
//            // 是标识符
//            // 加载标识符的值
//            var nameToken = expect(TokenType.Ident);
//            String name = (String) nameToken.getValue();
//            var symbol = symbol_table.get(name);
//            if (symbol == null) {
//                // 没有这个标识符
//                throw new AnalyzeError(ErrorCode.NotDeclared, nameToken.getStartPos());
//            } else if (!symbol.isInitialized) {
//                // 标识符没初始化
//                throw new AnalyzeError(ErrorCode.NotInitialized, nameToken.getStartPos());
//            }
//            var offset = getOffset(name, null);
//            instructions.add(new Instruction(Operation.LOD, offset));
//        } else if (check(TokenType.Uint)) {
//            // 是整数
//            // 加载整数值
//            int value = (int)expect(TokenType.Uint).getValue();
//            instructions.add(new Instruction(Operation.LIT, value));
//        } else if (check(TokenType.LParen)) {
//            // 是表达式
//            // 调用相应的处理函数
//            expect(TokenType.LParen);
//            analyseExpression();
//            expect(TokenType.RParen);
//        } else {
//            // 都不是，摸了
//            throw new ExpectedTokenError(List.of(TokenType.Ident, TokenType.Uint, TokenType.LParen), next());
//        }
//
//        if (negate) {
//            instructions.add(new Instruction(Operation.SUB));
//        }
//        // throw new Error("Not implemented");
//    }
}
