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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public final class Analyser {

    int current_while = -1;
    int[] while_start = new int[500];
    int[] while_end = new int[500];
    int[] break_index = new int[500];
    int[][] breaks = new int[500][500];

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

    private byte[] boolToByte(boolean n) {
        byte[] b = new byte[1];
        b[0] = (byte) (n ? 0x01 : 0x00);
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

    public void analyse(String output) throws CompileError, IOException {
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
        this.function_name = "_start";
        int main = this.functions.get("main").name;
        this.functions.get(this.function_name).addItem((byte) 0x4a, intToByte32(main));

        //处理结果
        FileOutputStream f = new FileOutputStream(new File(output));
        get_result(f);
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

    private boolean functionNameJudge(String name) throws CompileError {
        if (this.global_symbol_table.get(name) != null) {
            return false;
        }
        return true;
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

    private void analyseLocalDeclareStatement() throws CompileError {
        if (check(TokenType.CONST_KW)) {
            analyseConstDeclareStatement(this.function_symbol_tables.get(this.function_name), false);
        } else if (check(TokenType.LET_KW)) {
            analyseLetDeclareStatement(this.function_symbol_tables.get(this.function_name), false);
        } else {
            throw new AnalyzeError(ErrorCode.ExpectedToken, peek().getStartPos());
        }
        this.functions.get(this.function_name).loc_slot++;
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
            expect(TokenType.ASSIGN);
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
            if (check(TokenType.L_PAREN)) {
                if (!sysFunction(ident)) {
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
                    this.functions.get(this.function_name).addItem((byte) 0x4a, intToByte32(call_function.name));
                    //栈中去掉参数
                    for (int i = 0; i < call_function.param_slot; i++) {
                        this.stack.pop();
                    }
                    expect(TokenType.R_PAREN);
                }
            } else {
                SymbolEntry symbol = (SymbolEntry) this.function_symbol_tables.get(this.function_name).get(ident);
                //加载参数地址入栈
                if (symbol == null) {
                    symbol = (SymbolEntry) this.function_param_tables.get(this.function_name).get(ident);
                    if (symbol == null) {
                        symbol = this.global_symbol_table.get(ident);
                        if (symbol == null) {
                            System.out.println(ident);
                            throw new AnalyzeError(ErrorCode.ExpectedToken, peek().getStartPos());
                        }
                        this.functions.get(this.function_name).addItem((byte) 0x0c, intToByte32(symbol.index));
                    } else {
                        this.functions.get(this.function_name).addItem((byte) 0x0b, intToByte32(symbol.index));
                    }
                } else {
                    this.functions.get(this.function_name).addItem((byte) 0x0a, intToByte32(symbol.index));
                }
                this.stack.push(StackItem.ADDR);
                if (!check(TokenType.ASSIGN)) {
                    //加载参数的值入栈
                    this.functions.get(this.function_name).addItem((byte) 0x13, null);
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
            stack.push(StackItem.DOUBLE);
        } else if (check(TokenType.STRING_LITERAL)) {
            String str = (String) next().getValue();
            int index = this.globals.size();
            this.globals.add(new Global(true, str.length(), str.getBytes()));
            addSymbol(str, true, true, false, peek().getStartPos(), this.global_symbol_table, index, null);
            this.functions.get(this.function_name).addItem((byte) 0x01, intToByte64(index));
            this.stack.push(StackItem.INT);
        } else {
            System.out.println(next().getValue());
            System.out.println(next().getValue());
            System.out.println(next().getValue());
            System.out.println(next().getValue());
            throw new AnalyzeError(ErrorCode.ExpectedToken, peek().getStartPos());
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
        if (!functionNameJudge(function_name)) {
            throw new AnalyzeError(ErrorCode.NotDeclared, peek().getStartPos());
        }
        expect(TokenType.L_PAREN);
        int index = this.globals.size();
        int param_slot = analyseFunctionParamList(function_name);
        expect(TokenType.R_PAREN);
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
        addSymbol(function_name, true, true, true, peek().getStartPos(), this.global_symbol_table, index, return_type);
        this.functions.put(function_name, new Function(index, ret_slot, param_slot, 0, type));
        String temp_name = this.function_name;
        this.function_name = function_name;
        if (ret_slot > 0) {
            for (Map.Entry<String, SymbolEntry> entry : ((HashMap<String, SymbolEntry>)this.function_param_tables.get(function_name)).entrySet()) {
                entry.getValue().index++;
            }
            addSymbol("0", true, false, false, peek().getStartPos(), this.function_param_tables.get(function_name), 0, return_type);
        }
        analyseBlockStatement();
        if ("void".equals(return_type)) {
            this.functions.get(function_name).addItem((byte) 0x49, null);
        }
        this.function_name = temp_name;
    }

    private int analyseFunctionParamList(String function_name) throws CompileError {
        int count = 0;
        HashMap<String, SymbolEntry> function_param_table = new HashMap<>();
        HashMap<String, SymbolEntry> function_symbol_table = new HashMap<>();
        while (!check(TokenType.R_PAREN)) {
            if (count > 0) {
                expect(TokenType.COMMA);
            }
            count++;
            analyseFunctionParam(function_param_table);
        }
        this.function_param_tables.put(function_name, function_param_table);
        this.function_symbol_tables.put(function_name, function_symbol_table);
        return count;
    }

    private void analyseFunctionParam(HashMap<String, SymbolEntry> function_param_table) throws CompileError {
        boolean is_const = false;
        if (check(TokenType.CONST_KW)) {
            expect(TokenType.CONST_KW);
            is_const = true;
        }
        String param_name = (String) expect(TokenType.IDENT).getValue();
        expect(TokenType.COLON);
        String type = (String) expect(TokenType.IDENT).getValue();
        addSymbol(param_name, is_const, false, false, peek().getStartPos(), function_param_table, function_param_table.size(), type);
    }

    private void analyseBlockStatement() throws CompileError {
        expect(TokenType.L_BRACE);
        while (!check(TokenType.R_BRACE)) {
            analyseStatement();
        }
        expect(TokenType.R_BRACE);
    }

    private void analyseStatement() throws CompileError {
        switch (peek().getTokenType()) {
            case IDENT:
            case L_PAREN:
            case UINT_LITERAL:
            case DOUBLE_LITERAL:
            case STRING_LITERAL:
            case MINUS:
                analyseExpresion();
                expect(TokenType.SEMICOLON);
                break;
            case CONST_KW:
            case LET_KW:
                analyseLocalDeclareStatement();
                break;
            case IF_KW:
                analyseIfStatement();
                break;
            case WHILE_KW:
                analyseWhileStatement();
                break;
            case RETURN_KW:
                analyseReturnStatement();
                break;
            case L_BRACE:
                analyseBlockStatement();
                break;
            case SEMICOLON:
                expect(TokenType.SEMICOLON);
                break;
            case BREAK_KW:
                expect(TokenType.CONTINUE_KW);
                int index = ++this.break_index[this.current_while];
                this.functions.get(this.function_name).addItem((byte) 0x41, intToByte32(0));
                this.breaks[this.current_while][index] = this.functions.get(this.function_name).items.size();
                expect(TokenType.SEMICOLON);
                break;
            case CONTINUE_KW:
                expect(TokenType.CONTINUE_KW);
                int current = this.functions.get(this.function_name).items.size();
                int jump = this.while_start[this.current_while] - current - 1;
                this.functions.get(this.function_name).addItem((byte) 0x41, intToByte32(jump));
                expect(TokenType.SEMICOLON);
                break;
            default:
                System.out.println(peek().getValue());
                throw new AnalyzeError(ErrorCode.NotDeclared, peek().getStartPos());
        }
    }

    private void analyseIfStatement() throws CompileError {
        expect(TokenType.IF_KW);
        analyseExpresion();
        byte operation = (byte) (br_false ? 0x42 : 0x43);
        this.functions.get(function_name).addItem(operation, intToByte32(0));
        int start = this.functions.get(function_name).items.size();
        analyseBlockStatement();
        int end = this.functions.get(function_name).items.size();
        this.functions.get(function_name).changeItem(start - 1, intToByte32(end - start));
        if (check(TokenType.ELSE_KW)) {
            expect(TokenType.ELSE_KW);
            if (check(TokenType.IF_KW)) {
                analyseIfStatement();
                return;
            }
            this.functions.get(function_name).changeItem(start - 1, intToByte32(end - start + 1));
            this.functions.get(function_name).addItem((byte) 0x41, intToByte32(0));
            start = this.functions.get(function_name).items.size();
            analyseBlockStatement();
            end = this.functions.get(function_name).items.size();
            this.functions.get(function_name).changeItem(start - 1, intToByte32(end - start));
        }
    }

    private void analyseWhileStatement() throws CompileError {
        expect(TokenType.WHILE_KW);
        this.current_while++;
        this.break_index[this.current_while] = -1;
        this.while_start[this.current_while] = this.functions.get(function_name).items.size();
        analyseExpresion();
        byte operation = (byte) (br_false ? 0x42 : 0x43);
        this.functions.get(function_name).addItem(operation, intToByte32(0));
        int start = this.functions.get(function_name).items.size();
        analyseBlockStatement();
        this.functions.get(function_name).addItem((byte) 0x41, intToByte32(this.while_start[this.current_while] - this.functions.get(function_name).items.size() - 1));
        this.while_end[this.current_while] = this.functions.get(function_name).items.size();
        this.functions.get(function_name).changeItem(start - 1, intToByte32(this.while_end[this.current_while] - start));
        //处理break
        for (int index = this.break_index[this.current_while]; index > -1 ; index--) {
            int offset = this.breaks[this.current_while][index];
            this.functions.get(function_name).changeItem(offset - 1, intToByte32(this.while_end[this.current_while] - offset));
        }
        this.current_while--;
    }

    private void analyseReturnStatement() throws CompileError {
        expect(TokenType.RETURN_KW);
        if (!check(TokenType.SEMICOLON)) {
            this.functions.get(function_name).addItem((byte) 0x0b, intToByte32(0));
            stack.push(StackItem.ADDR);
            analyseExpresion();
            this.functions.get(function_name).addItem((byte) 0x17, null);
            stack.pop();
            stack.pop();
        }
        this.functions.get(function_name).addItem((byte) 0x49, null);
        expect(TokenType.SEMICOLON);
    }

    private void get_result(FileOutputStream f) throws IOException {
        //print_result();
        //magic version
        byte[] magic = new byte[]{0x72, 0x30, 0x3b, 0x3e};
        byte[] version = new byte[]{0x00, 0x00,  0x00, 0x01};
        f.write(magic);
        f.write(version);
        //globals.count
        f.write(intToByte32(this.globals.size()));
        //globals
        for (int i = 0; i < this.globals.size(); i++) {
            f.write(boolToByte(globals.get(i).is_const));
            f.write(intToByte32(globals.get(i).count));
            for (int j = 0; j < this.globals.get(i).items.length; j++) {
                f.write(globals.get(i).items[j]);
            }
        }
        //functions.count
        f.write(intToByte32(this.functions.size()));
        //functions
        for (Map.Entry<String, Function> entry : this.functions.entrySet()) {
            Function function = entry.getValue();
            f.write(intToByte32(function.name));
            f.write(intToByte32(function.ret_slot));
            f.write(intToByte32(function.param_slot));
            f.write(intToByte32(function.loc_slot));
            f.write(intToByte32(function.count));
            for (int j = 0; j < function.count; j++) {
                f.write(function.getItemOperation(j));
                byte[] num = function.getItemNum(j);
                if (num != null) {
                    f.write(function.getItemNum(j));
                }
            }
        }
    }
    private void print_result() {
        System.out.println(this.globals.size());
        for (int i = 0; i < this.globals.size(); i++) {
            for (int j = 0; j < this.globals.get(i).items.length; j++) {
                System.out.print((char) globals.get(i).items[j]);
            }
            System.out.println();
        }
        //functions.count
        System.out.println(this.functions.size());
        //functions
        for (Map.Entry<String, Function> entry : this.functions.entrySet()) {
            Function function = entry.getValue();
            System.out.println(entry.getKey());
            System.out.println(function.name);
            System.out.println(function.ret_slot);
            System.out.println(function.param_slot);
            System.out.println(function.loc_slot);
            System.out.println(function.count);
            for (int j = 0; j < function.count; j++) {
                System.out.print(function.getItemOperation(j));
                System.out.print(" ");
                byte[] num = function.getItemNum(j);
                if (num != null) {
                    for (int i = 0; i < function.getItemNum(j).length; i++) {
                        System.out.print(function.getItemNum(j)[i]);
                        System.out.print(" ");
                    }
                }
                System.out.println();
            }
            System.out.println();
        }
    }

}
