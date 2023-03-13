

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/*
 * @authors:
 *      Rodrigo Cabrito 54455
 *      João Costa 54482
 *      João Fraga 44837
 */

public class Tintolmarket {
    
    private static final int port = 11127;

    public static void main(String[] args) {
        
        try {
            
            Socket clientSocket = new Socket(args[0],port);

            //streams
            ObjectInputStream inStream = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream outStream = new ObjectOutputStream(clientSocket.getOutputStream());

            //authentication
            outStream.writeObject(args[1]);
            outStream.writeObject(args[2]);

            while(true) {

                //print menu
                System.out.print((String) inStream.readObject());
                Scanner sc = new Scanner(System.in);
                
                //send command
                String command = sc.nextLine();
                System.out.println(command);
                outStream.writeObject(command);

                //print answer(s)
                System.out.println(inStream.readObject());

                //print powering off
                if (command.equals("exit")) {
                    System.out.println((String) inStream.readObject());
                    sc.close();
                    clientSocket.close();
                    System.exit(0);
                }
            }            

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
