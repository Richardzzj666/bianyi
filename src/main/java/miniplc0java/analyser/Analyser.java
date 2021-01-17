package miniplc0java.analyser;

import miniplc0java.error.AnalyzeError;
import miniplc0java.error.CompileError;
import miniplc0java.error.ErrorCode;
import miniplc0java.error.ExpectedTokenError;
import miniplc0java.error.TokenizeError;
import miniplc0java.instruction.Global;
import miniplc0java.instruction.Function;
import miniplc0java.instruction.Operation;
import miniplc0java.tokenizer.Token;
import miniplc0java.tokenizer.TokenType;
import miniplc0java.tokenizer.Tokenizer;
import miniplc0java.util.Pos;

import java.util.*;

public final class Analyser {

    Tokenizer tokenizer;
    //全局变量
    ArrayList<Global> globals;
    //函数
    HashMap<String, Function> functions;
    //栈上数据类型
    Stack stack;
    String function_name;
    Token peekedToken = null;

    //全局变量符号表
    HashMap<String, SymbolEntry> global_symbol_table = new HashMap<>();
    //函数变量符号表集
    HashMap<String, HashMap> function_symbol_tables = new HashMap<>();
    //函数参数符号表集
    HashMap<String, HashMap> function_param_tables = new HashMap<>();

    boolean br_false = false;

    public Analyser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.globals = new ArrayList<>();
        this.functions = new HashMap<>();
        this.function_name = "_start";
        this.stack = new Stack();
    }

    private byte[] intToByte32(int n) {
        byte[] b = new byte[4];
        b[3] = (byte) (n & 0xff);
        b[2] = (byte) (n >> 8 & 0xff);
        b[1] = (byte) (n >> 16 & 0xff);
        b[0] = (byte) (n >> 24 & 0xff);
        return b;
    }

    private byte[] intToByte64(int n) {
        byte[] b = new byte[8];
        b[7] = (byte) (n & 0xff);
        b[6] = (byte) (n >> 8 & 0xff);
        b[5] = (byte) (n >> 16 & 0xff);
        b[4] = (byte) (n >> 24 & 0xff);
        b[3] = 0x00;
        b[2] = 0x00;
        b[1] = 0x00;
        b[0] = 0x00;
        return b;
    }

    private byte[] longToByte64(long n) {
        byte[] b = new byte[8];
        b[7] = (byte) (n & 0xff);
        b[6] = (byte) (n >> 8 & 0xff);
        b[5] = (byte) (n >> 16 & 0xff);
        b[4] = (byte) (n >> 24 & 0xff);
        b[3] = (byte) (n >> 32 & 0xff);
        b[2] = (byte) (n >> 40 & 0xff);
        b[1] = (byte) (n >> 48 & 0xff);
        b[0] = (byte) (n >> 56 & 0xff);
        return b;
    }

    private byte[] doubleToByte64(double n) {
        byte[] b = longToByte64(Double.doubleToLongBits(n));
        return b;
    }

    public void analyse() throws CompileError {
        //_start()加入全局变量
        this.globals.add(new Global(true, 6, "_start".getBytes()));
        //_start()加入函数集
        this.functions.put("_start", new Function(0, 0, 0, 0, null));
        //_start()加入全局符号表
        this.global_symbol_table.put("_start", new SymbolEntry(true, true, true, 0, "void"));
        HashMap<String, SymbolEntry> symbol_table = new HashMap<>();
        HashMap<String, SymbolEntry> param_table = new HashMap<>();
        //_start()的局部变量符号表加入函数变量符号表集
        this.function_symbol_tables.put("_start", symbol_table);
        //_start()的参数符号表加入函数参数符号表表集
        this.function_param_tables.put("_start", param_table);
        analyseProgram();
        //把调用main()加入_start()

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
                           HashMap<String, SymbolEntry> table, int index, String type) throws AnalyzeError {
        if (table.get(name) != null) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        } else {
            table.put(name, new SymbolEntry(isConstant, isInitialized, isFunction, index, type));
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
        byte operation = 0x0a;
        if (is_global) {
            operation = 0x0c;
        }
        //把常量放到栈上
        this.functions.get(this.function_name).addItem(operation, intToByte32(table.size()));
        this.stack.push(StackItem.ADDR);
        analyseExpresion();
        //把赋值放到栈上
        this.functions.get(this.function_name).addItem((byte) 0x17, null);
        this.stack.pop();
        this.stack.pop();
        //把常量加入符号表
        addSymbol(name, true, true, false, peek().getStartPos(), table, table.size(), type);
        //把常量加入全局变量
        if (is_global) {
            this.globals.add(new Global(true, 8, new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00}));
        }
        expect(TokenType.SEMICOLON);
    }

    private void analyseLetDeclareStatement(HashMap<String, SymbolEntry> table, boolean is_global) throws CompileError {
        expect(TokenType.LET_KW);
        String name = (String) expect(TokenType.IDENT).getValue();
        expect(TokenType.COLON);
        String type = (String) expect(TokenType.IDENT).getValue();
        boolean is_initialized = false;
        byte operation = 0x0a;
        if (is_global) {
            operation = 0x0c;
        }
        if (check(TokenType.ASSIGN)) {
            //把变量放到栈上
            this.functions.get(this.function_name).addItem(operation, intToByte32(table.size()));
            this.stack.push(StackItem.ADDR);
            analyseExpresion();
            //把赋值放到栈上
            this.functions.get(this.function_name).addItem((byte) 0x17, null);
            this.stack.pop();
            this.stack.pop();
            is_initialized = true;
        }
        //把变量加入符号表
        addSymbol(name, false, is_initialized, false, peek().getStartPos(), table, table.size(), type);
        //把变量加入全局变量
        if (is_global) {
            this.globals.add(new Global(false, 8, new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00}));
        }
        expect(TokenType.SEMICOLON);
    }

    private void analyseExpresion() throws CompileError {
        exp1();
        if (check(TokenType.ASSIGN)) {
            expect(TokenType.ASSIGN);
            exp1();
            //把赋值放到栈上
            this.functions.get(this.function_name).addItem((byte) 0x17, null);
            this.stack.pop();
            this.stack.pop();
        }
    }

    private void exp1() throws CompileError {
        exp2();
        if (check(TokenType.LT) || check(TokenType.GT) || check(TokenType.GE) || check(TokenType.LE) || check(TokenType.EQ) || check(TokenType.NEQ)) {
            Token compare = next();
            exp2();
            //把比较结果放到栈上
            this.functions.get(this.function_name).addItem((byte) (stack.getTop() == StackItem.DOUBLE ? 0x32 : 0x30), null);
            this.stack.pop();
            this.stack.pop();
            this.stack.push(StackItem.INT);
            byte operation;
            switch (compare.getTokenType()) {
                case LT:
                    operation = 0x39;
                    this.br_false = true;
                    break;
                case LE:
                    operation = 0x3a;
                    this.br_false = false;
                    break;
                case GT:
                    operation = 0x3a;
                    this.br_false = true;
                    break;
                case GE:
                    operation = 0x39;
                    this.br_false = false;
                    break;
                case EQ:
                    operation = 0x00;
                    this.br_false = false;
                    break;
                case NEQ:
                    operation = 0x00;
                    this.br_false = true;
                    break;
                default:
                    throw new AnalyzeError(ErrorCode.ExpectedToken, peek().getStartPos());
            }
            if (operation != 0x00) {
                //把比较结果的转化放到栈上
                this.functions.get(this.function_name).addItem(operation, null);
                //pop()再push()不变
            }
        }
    }

    private void exp2() throws CompileError {
        exp3();
        while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            Token token = next();
            exp3();
            byte operation;
            if (this.stack.getTop() == StackItem.DOUBLE) {
                operation = (byte) (token.getTokenType() == TokenType.PLUS ? 0x24 : 0x25);
            } else if (this.stack.getTop() == StackItem.INT) {
                operation = (byte) (token.getTokenType() == TokenType.PLUS ? 0x20 : 0x21);
            } else {
                throw new AnalyzeError(ErrorCode.ExpectedToken, peek().getStartPos());
            }
            //把加减操作放入栈中
            this.functions.get(this.function_name).addItem(operation, null);
            //两次pop()一次push()
            this.stack.pop();
        }
    }

    private void exp3() throws CompileError {
        exp4();
        while (check(TokenType.MUL) || check(TokenType.DIV)) {
            Token token = next();
            exp4();
            byte operation;
            if (this.stack.getTop() == StackItem.DOUBLE) {
                operation = (byte) (token.getTokenType() == TokenType.MUL ? 0x26 : 0x27);
            } else if (this.stack.getTop() == StackItem.INT) {
                operation = (byte) (token.getTokenType() == TokenType.MUL ? 0x22 : 0x23);
            } else {
                throw new AnalyzeError(ErrorCode.ExpectedToken, peek().getStartPos());
            }
            //把乘除操作放入栈中
            this.functions.get(this.function_name).addItem(operation, null);
            //两次pop()一次push()
            this.stack.pop();
        }
    }

    private void exp4() throws CompileError {
        exp5();
        while (check(TokenType.AS_KW)) {
            expect(TokenType.AS_KW);
            String type = (String) expect(TokenType.IDENT).getValue();
            byte operation;
            if (this.stack.getTop() != StackItem.INT && this.stack.getTop() != StackItem.DOUBLE) {
                throw new AnalyzeError(ErrorCode.ExpectedToken, peek().getStartPos());
            }
            //数据转换
            this.stack.pop();
            if ("int".equals(type)) {
                operation = 0x37;
                this.stack.push(StackItem.INT);
            } else if ("double".equals(type)) {
                operation = 0x36;
                this.stack.push(StackItem.DOUBLE);
            } else {
                throw new AnalyzeError(ErrorCode.ExpectedToken, peek().getStartPos());
            }
            //把类型转换放入栈中
            this.functions.get(function_name).addItem(operation, null);
        }
    }
    private void exp5() throws CompileError {
        if (check(TokenType.MINUS)) {
            expect(TokenType.MINUS);
            exp5();
            byte operation = (byte) (this.stack.getTop() == StackItem.INT ? 0x34 : 0x35);
            //把取反放入栈中
            this.functions.get(this.function_name).addItem(operation, null);
        } else if (check(TokenType.IDENT)) {
            String ident = (String) next().getValue();
            if (check(TokenType.L_PAREN) && !sysFunction(ident)) {
                expect(TokenType.L_PAREN);
                Function call_function = this.functions.get(ident);
                if (call_function == null) {
                    throw new AnalyzeError(ErrorCode.ExpectedToken, peek().getStartPos());
                }
                if (call_function.ret_slot > 0) {
                    //分配返回值空间
                    this.functions.get(this.function_name).addItem((byte) 0x1a, intToByte32(call_function.ret_slot));
                    for (int i = 0; i < call_function.ret_slot; i++) {
                        this.stack.push(call_function.type);
                    }
                }
                //分配参数空间
                for (int i = 0; i < call_function.param_slot; i++) {
                    analyseExpresion();
                    if (i != call_function.param_slot - 1) {
                        expect(TokenType.COMMA);
                    }
                }
                this.functions.get(this.function_name).addItem((byte) 0x48, intToByte32(call_function.name));
                //栈中去掉参数
                for (int i = 0; i < call_function.param_slot; i++) {
                    this.stack.pop();
                }
                expect(TokenType.R_PAREN);
            } else {
                SymbolEntry symbol = (SymbolEntry) this.function_symbol_tables.get(this.function_name).get(ident);
                //加载参数地址入栈
                if (symbol == null) {
                    symbol = (SymbolEntry) this.function_param_tables.get(this.function_name).get(ident);
                    if (symbol == null) {
                        symbol = this.global_symbol_table.get(ident);
                        if (symbol == null) {
                            throw new AnalyzeError(ErrorCode.ExpectedToken, peek().getStartPos());
                        }
                        this.functions.get(this.function_name).addItem((byte) 0x0c, intToByte32(symbol.index));
                    }
                    this.functions.get(this.function_name).addItem((byte) 0x0b, intToByte32(symbol.index));
                } else {
                    this.functions.get(this.function_name).addItem((byte) 0x0a, intToByte32(symbol.index));
                }
                this.stack.push(StackItem.ADDR);
                if (!check(TokenType.ASSIGN)) {
                    //加载参数的值入栈
                    this.functions.get(this.function_name).addItem((byte) 0x13, intToByte32(symbol.index));
                    stack.pop();
                    stack.push(symbol.type);
                }
            }
        } else if (check(TokenType.L_PAREN)) {
            expect(TokenType.L_PAREN);
            analyseExpresion();
            expect(TokenType.L_PAREN);
        } else if (check(TokenType.UINT_LITERAL)) {
            this.functions.get(this.function_name).addItem((byte) 0x01, intToByte64((int) next().getValue()));
            stack.push(StackItem.INT);
            this.br_false = true;
        } else if (check(TokenType.DOUBLE_LITERAL)) {
            this.functions.get(this.function_name).addItem((byte) 0x01, doubleToByte64((double) next().getValue()));
            stack.push(StackItem.INT);
            this.br_false = true;
        }
    }

    private boolean sysFunction(String name) throws CompileError {
        expect(TokenType.L_PAREN);
        byte operation;
        if ("getint".equals(name)) {
            operation = 0x50;
            this.stack.push(StackItem.INT);
        } else if ("getdouble".equals(name)) {
            operation = 0x52;
            this.stack.push(StackItem.DOUBLE);
        } else if ("getchar".equals(name)) {
            operation = 0x51;
            this.stack.push(StackItem.INT);
        } else if ("putint".equals(name)) {
            operation = 0x54;
            analyseExpresion();
            this.stack.pop();
        } else if ("putdouble".equals(name)) {
            operation = 0x56;
            analyseExpresion();
            this.stack.pop();
        } else if ("putchar".equals(name)) {
            operation = 0x55;
            analyseExpresion();
            this.stack.pop();
        } else if ("putstr".equals(name)) {
            operation = 0x57;
            analyseExpresion();
            this.stack.pop();
        } else if ("putln".equals(name)) {
            operation = 0x58;
            this.stack.pop();
        } else {
            return false;
        }
        //把系统函数的调用放到栈上
        this.functions.get(this.function_name).addItem(operation, null);
        expect(TokenType.R_PAREN);
        return true;
    }

















    private void analyseFunction() throws CompileError {
        expect(TokenType.FN_KW);
        String function_name = (String) expect(TokenType.IDENT).getValue();
        expect(TokenType.L_PAREN);
        int name = this.globals.size();
        int param_slot = analyseFunctionParamList(function_name);
        expect(TokenType.L_PAREN);
        expect(TokenType.ARROW);
        String return_type = (String) expect(TokenType.IDENT).getValue();
        StackItem type;
        int ret_slot;
        switch (return_type) {
            case "void":
                ret_slot = 0;
                type = null;
                break;
            case "int":
                ret_slot = 1;
                type = StackItem.INT;
                break;
            case "double":
                ret_slot = 1;
                type = StackItem.DOUBLE;
                break;
            default:
                throw new AnalyzeError(ErrorCode.NotDeclared, peek().getStartPos());
        }
        this.globals.add(new Global(false, function_name.length(), function_name.getBytes()));
        this.functions.put(function_name, new Function(name, ret_slot, param_slot, 0, type));
        analyseFunctionBlockStatement();
    }

    private int analyseFunctionParamList(String function_name) throws CompileError {
        int count = 0;
        HashMap<String, SymbolEntry> function_table = new HashMap<>();
        while (!check(TokenType.R_PAREN)) {
            if (count > 0) {
                expect(TokenType.COMMA);
            }
            count++;
            analyseFunctionParam(function_table);
        }
        this.function_symbol_tables.put(function_name, function_table);
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
