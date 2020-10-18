import java.io.*;

public class First {
    public static void main(String[] args) throws IOException{
        String path = args[0];
        BufferedReader in  = new BufferedReader(new FileReader(path));
        String str;
        while((str = in.readLine()) != null){
            if(run(str) == 0){
                break;
            }
        }
    }

    public static int run(String str){
        String[] array = str.split("\\s+");
        for(int i = 0, num = 0, sign = 0; i < array.length; i++, num = 0, sign = 0){
            //sign:1冒号,2数字,3字母
            char[] output = new char[array[i].length()];
            for(int j = 0; j < array[i].length(); j++){
                if(num == 0){
                    if(array[i].charAt(j) == '+'){
                        System.out.println("Plus");
                        continue;
                    }
                    else if(array[i].charAt(j) == '*'){
                        System.out.println("Star");
                        continue;
                    }
                    else if(array[i].charAt(j) == ','){
                        System.out.println("Comma");
                        continue;
                    }
                    else if(array[i].charAt(j) == '('){
                        System.out.println("LParenthesis");
                        continue;
                    }
                    else if(array[i].charAt(j) == ')'){
                        System.out.println("RParenthesis");
                        continue;
                    }
                    else if(array[i].charAt(j) == ':'){
                        sign = 1;
                        num++;
                        continue;
                    }
                    else if(array[i].charAt(j) >= 48 && array[i].charAt(j) <= 57){
                        sign = 2;
                        output[num++] = array[i].charAt(j);
                    }
                    else if((array[i].charAt(j) >= 65 && array[i].charAt(j) <= 90) || (array[i].charAt(j) >= 97 && array[i].charAt(j) <= 122)){
                        sign = 3;
                        output[num++] = array[i].charAt(j);
                    }
                    else{
                        System.out.println("Unknown");
                        return 0;
                    }
                }
                else{
                    if(sign == 1) {
                        num = 0;
                        if(array[i].charAt(j) == '='){
                            System.out.println("Assign");
                            continue;
                        }
                        else{
                            System.out.println("Colon");
                            j--;
                            continue;
                        }
                    }
                    else if(sign == 2){
                        if(array[i].charAt(j) >= 48 && array[i].charAt(j) <= 57){
                            output[num++] = array[i].charAt(j);
                        }
                        else{
                            word(new String(output).substring(0, num));
                            num = 0;
                            j--;
                            continue;
                        }
                    }
                    else{
                        if((array[i].charAt(j) >= 48 && array[i].charAt(j) <= 57) || (array[i].charAt(j) >= 65 && array[i].charAt(j) <= 90) || (array[i].charAt(j) >= 97 && array[i].charAt(j) <= 122)){
                            output[num++] = array[i].charAt(j);
                            continue;
                        }
                        else{
                            word(new String(output).substring(0, num));
                            j--;
                            num = 0;
                        }
                    }
                }
            }
            if(num != 0){
                if(sign == 1) {
                    System.out.println("Colon");
                }
                else{
                    word(new String(output).substring(0, num));
                }
            }
        }
        return 1;
    }

    public static void word(String str){
        if(str.equals("BEGIN")){
            System.out.println("Begin");
        }
        else if(str.equals("END")){
            System.out.println("End");
        }
        else if(str.equals("FOR")){
            System.out.println("For");
        }
        else if(str.equals("IF")){
            System.out.println("If");
        }
        else if(str.equals("THEN")){
            System.out.println("Then");
        }
        else if(str.equals("ELSE")){
            System.out.println("Else");
        }
        else if((str.charAt(0) >= 65 && str.charAt(0) <= 90) || (str.charAt(0) >= 97 && str.charAt(0) <= 122)){
            System.out.println("Ident(" + str + ")");
        }
        else{
            for(int i = 0; i < str.length(); i++){
                if(str.charAt(i) != 0 || i == str.length() - 1){
                    System.out.println("Int(" + str.substring(i) + ")");
                    break;
                }
            }
        }
        return;
    }
}
