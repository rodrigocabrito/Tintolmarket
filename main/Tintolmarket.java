package Tintolmarket.main;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

/*
 * @authors:
 *      Rodrigo Cabrito 54455
 *      João Costa 54482
 *      João Fraga 44837
 */

public class Tintolmarket {
    
    private static int port = 12345;

    public static void main(String[] args) {
        
        try {
            
            Socket clientSocket = new Socket(args[1],port);

            //streams
            ObjectInputStream inStream = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream outStream = new ObjectOutputStream(clientSocket.getOutputStream());

            outStream.writeObject(args[0]);
            outStream.writeObject(args[1]);
            
            //answer (credentials received or not)
            System.out.println((String) inStream.readObject() + "\n");

            while(true) {

                //print menu
                System.out.println((String) inStream.readObject());
                Scanner sc = new Scanner(System.in);
                
                //send command
                String command = sc.next();
                outStream.writeObject(command);

                //print answer
                System.out.println((String) inStream.readObject());

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
