/*
    Arano, Christian Timothy Z.
    12203211
    NSCOM01 - S12
*/
import java.util.*;

/*
    TFTP is the main class, which contains the main method, the driver of the application.
*/
public class TFTP {
    public static void main(String[] args){        
        TFTPClient client = new TFTPClient();
        Scanner scanner = new Scanner(System.in);
        int select;
           
        client.ObtainServerIP();
        boolean validInput = true;
       
        do{
            validInput = true;
            System.out.println("Menu:");
            System.out.println("[1] Read File from Server");
            System.out.println("[2] Write File to Server");
            System.out.print("Select: ");

            select = scanner.nextInt();

            switch(select){
                case 1:
                    client.SendRRQ();
                    break;
                case 2:
                    client.SendWRQ();
                    break;
                default:
                    System.out.println("Invalid input");
                    validInput = false;
                    break;
            }
        } while(!validInput);
        scanner.close();
    }
}