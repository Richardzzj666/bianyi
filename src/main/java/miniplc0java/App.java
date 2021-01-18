package miniplc0java;
import miniplc0java.analyser.*;
import miniplc0java.error.CompileError;
import miniplc0java.tokenizer.StringIter;
import miniplc0java.tokenizer.Tokenizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class App {
    public static void main(String[] args) throws IOException, CompileError {
        String input = args[0];
        String output = args[1];

        FileInputStream inputStream = new FileInputStream(new File(input));
        Scanner scanner = new Scanner(inputStream, "UTF-8");
//        while (scanner.hasNext()) {
//            System.out.print(scanner.next());
//        }
        StringIter stringIter = new StringIter(scanner);
        Tokenizer tokenizer = new Tokenizer(stringIter);
        Analyser analyser = new Analyser(tokenizer);
        analyser.analyse(output);
    }
}
