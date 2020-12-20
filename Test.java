import java.util.Scanner;
public class Test {
    public static void main(String[] args) {
        if (args.length > 1) {
            System.out.print("Test called with "+args.length+" args: ");
            for (String arg: args) {
                System.out.print(arg+" ");
            }
            System.out.println();
        }
        System.out.println("Hello. I am Test. Please enter input now.");
        Scanner sc = new Scanner(System.in);
        System.out.println("Thank you for the input: "+sc.nextLine());
        sc.close();
    }
}
