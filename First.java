import java.io.*;

public class First {
    public static char [] stack = new char[5000];
    public static int [][] arr = {{1, 2, 2, 2, 1},
            {1, 1, 2, 2, 1},
            {1, 1, 0, 0, 1},
            {2, 2, 2, 2, 3},
            {1, 1, 0, 0, 1}};
    public static int loc = 0, sloc = 0, index1 = -1, index2 = -1, index3 = -1;
    // 0:error 1:> 2:< 3:=    +*i()

    public static void main(String[] args) throws IOException{
        String path = args[0];
        BufferedReader in  = new BufferedReader(new FileReader(path));
        String str = in.readLine();
        run(str);
    }

    public static void run(String str){
;        for(; loc < str.length(); loc++, index1 = index3){
            switch (str.charAt(loc)){
                case ' ':
                    continue;
                case 'i':
                    index2 = 2;
                    index3 = 2;
                    break;
                case '+':
                    index2 = 0;
                    index3 = 0;
                    break;
                case '*':
                    index2 = 1;
                    index3 = 1;
                    break;
                case '(':
                    index2 = 3;
                    index3 = 3;
                    break;
                case ')':
                    index2 = 4;
                    index3 = 4;
                    break;
                default:
                    System.out.println("E");
                    return;
            }
            if(index1 >= 0 && (arr[index1][index2] == 1)){
                if(!sta()){
                    System.out.println("RE");
                    return;
                }
                System.out.println("R");
                loc--;
                index3 = index1;
            } else if(index1 >= 0 && arr[index1][index2] == 0){
                System.out.println("E");
                return;
            } else{
                stack[sloc++] = str.charAt(loc);
                System.out.println("I" + str.charAt(loc));
            }
        }
        while(sloc != 1 || sloc - 1 != 'N'){
            if(!sta()){
                System.out.println("RE");
                return;
            }
            System.out.println("R");
        }

    }

    public static boolean sta(){
        switch (index1){
            case 0:
                if(sloc > 2 && stack[sloc - 3] == 'N' && stack[sloc - 2] == '+' && stack[sloc - 1] == 'N'){
                    stack[sloc - 3] = 'N';
                    sloc -= 2;
                    break;
                }
                else{
                    return false;
                }
            case 1:
                if(sloc > 2 && stack[sloc - 3] == 'N' && stack[sloc - 2] == '*' && stack[sloc - 1] == 'N'){
                    stack[sloc - 3] = 'N';
                    sloc -= 2;
                    break;
                }
                else{
                    return false;
                }
            case 2:
                stack[sloc - 1] = 'N';
                break;
            case 4:
                if(sloc > 2 && stack[sloc - 2] == 'N' && stack[sloc - 3] == '('){
                    stack[sloc - 3] = 'N';
                    sloc -= 2;
                    break;
                }
                else{
                    return false;
                }
        }
        if(sloc == 1){
            index1 = -1;
        } else{
            int i;
            for(i = sloc - 2; i >= 0 && stack[i] == 'N'; i--){}
            if(i < 0) {
                return false;
            } else{
                switch (stack[i]){
                    case '+':
                        index1 = 0;
                        break;
                    case '*':
                        index1 = 1;
                        break;
                    case '(':
                        index1 = 3;
                        break;
                }
            }
        }
        return true;
    }
}