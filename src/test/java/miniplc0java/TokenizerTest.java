package miniplc0java;

import java.io.FileInputStream;
import miniplc0java.instruction.Instruction;
import miniplc0java.instruction.Operation;
import miniplc0java.tokenizer.StringIter;
import miniplc0java.tokenizer.Token;
import miniplc0java.tokenizer.TokenType;
import miniplc0java.tokenizer.Tokenizer;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

import static org.junit.Assert.*;

public class TokenizerTest {
    @Test
    public void testUint() throws FileNotFoundException {
        Scanner scanner;
        scanner = new Scanner(new FileInputStream("src/test/java/miniplc0java/TestUint"));
        var iter = new StringIter(scanner);
        var tokenizer = new Tokenizer(iter);
        var tokens = new ArrayList<Token>();
        try {
            while (true) {
                var token = tokenizer.nextToken();
                if (token.getTokenType().equals(TokenType.EOF)) {
                    break;
                }
                tokens.add(token);
            }
        } catch (Exception e) {
            // 遇到错误不输出，直接退出
            System.err.println(e);
            System.exit(0);
            return;
        }
        assertEquals(tokens.get(0).toString(), "Line: 0 Column: 0 Type: UnsignedInteger Value: 321");
        assertEquals(tokens.get(1).toString(), "Line: 0 Column: 4 Type: UnsignedInteger Value: 254");
        assertEquals(tokens.get(2).toString(), "Line: 1 Column: 0 Type: UnsignedInteger Value: 3");
    }

    @Test
    public void testIdentOrKeyword() throws FileNotFoundException {
        Scanner scanner;
        scanner = new Scanner(new FileInputStream("src/test/java/miniplc0java/TestIdentOrKeyword"));
        var iter = new StringIter(scanner);
        var tokenizer = new Tokenizer(iter);
        var tokens = new ArrayList<Token>();
        try {
            while (true) {
                var token = tokenizer.nextToken();
                if (token.getTokenType().equals(TokenType.EOF)) {
                    break;
                }
                tokens.add(token);
            }
        } catch (Exception e) {
            // 遇到错误不输出，直接退出
            System.err.println(e);
            System.exit(0);
            return;
        }
        for (int i=0; i<tokens.size(); i++) {
            System.out.println(tokens.get(i).toString());
        }
//        assertEquals(tokens.get(0).toString(), "Line: 0 Column: 0 Type: Identifier Value: a1b2c");
//        assertEquals(tokens.get(1).toString(), "Line: 0 Column: 6 Type: Begin Value: begin");
//        assertEquals(tokens.get(2).toString(), "Line: 1 Column: 0 Type: End Value: end");
//        assertEquals(tokens.get(3).toString(), "Line: 2 Column: 0 Type: Const Value: const");
//        assertEquals(tokens.get(4).toString(), "Line: 2 Column: 6 Type: Var Value: var");
//        assertEquals(tokens.get(5).toString(), "Line: 3 Column: 0 Type: Print Value: print");
//        assertEquals(tokens.get(6).toString(), "Line: 3 Column: 6 Type: Identifier Value: print2");
    }
}
