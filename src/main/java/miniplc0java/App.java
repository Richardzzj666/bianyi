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
        Scanner scanner = new Scanner(inputStream,"UTF-8");
//        while (scanner.hasNext()) {
//            System.out.print(scanner.next());
//        }
        StringIter stringIter = new StringIter(scanner);
        Tokenizer tokenizer = new Tokenizer(stringIter);
        Analyser analyser = new Analyser(tokenizer);
        analyser.analyse(output);

        inputStream = new FileInputStream(new File(output));
        byte[] buff = inputStream.readAllBytes();
        int ln = 0;
        for(int i = 0; i < buff.length; i++)
        {
            ln++;
            System.out.print(work(buff[i])+" ");
            if(ln > 15) {
                System.out.println("");
                ln = 0;
            }
        }
    }
    public static String work(byte a)
    {
        int num = ((int)a + 256) % 256;
        int first = num / 16;
        int second = num - first * 16;
        String s1 = "", s2 = "";
        if(first < 10) {
            s1 = String.valueOf(first);
        } else {
            switch (first) {
                case 10:
                    s1 = "A";
                    break;
                case 11:
                    s1 = "B";
                    break;
                case 12:
                    s1 = "C";
                    break;
                case 13:
                    s1 = "D";
                    break;
                case 14:
                    s1 = "E";
                    break;
                case 15:
                    s1 = "F";
                    break;
                default:
                    s1 = null;
            }
        }
        if(second < 10) {
            s2 = String.valueOf(second);
        } else {
            switch (second)
            {
                case 10:
                    s2 = "A";
                    break;
                case 11:
                    s2 = "B";
                    break;
                case 12:
                    s2 = "C";
                    break;
                case 13:
                    s2 = "D";
                    break;
                case 14:
                    s2 = "E";
                    break;
                case 15:
                    s2 = "F";
                    break;
                default:
                    s2 = null;
            }
        }
        return  s1+s2;
    }
}
