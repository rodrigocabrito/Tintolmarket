
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

/*
 * @authors:
 *      Rodrigo Cabrito 54455
 *      João Costa 54482
 *      João Fraga 44837
 */

public class Tintolmarket {

    private static final String CLIENT_DIR = "./clientFiles/";

    public static void main(String[] args) {
        
        try {

            Socket clientSocket = null;
            String[] ipPort = args[0].split(":");

            if (ipPort.length == 1) {
                clientSocket = new Socket(args[0],12345);
            } else {
                clientSocket = new Socket(ipPort[0],Integer.parseInt(ipPort[1]));
            }

            //streams
            ObjectInputStream inStream = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream outStream = new ObjectOutputStream(clientSocket.getOutputStream());

            //authentication
            outStream.writeObject(args[1]);
            outStream.writeObject(args[2]);

            while(true) {

                //print menu
                System.out.print(inStream.readObject());
                Scanner sc = new Scanner(System.in);
                
                // get command from system in
                String command = sc.nextLine();

                String[] splitCommand = command.split(" ");

                if (splitCommand[0].equals("add") || splitCommand[0].equals("a")) {

                    outStream.writeObject(command); //send command

                    File fileToSend = new File(CLIENT_DIR + splitCommand[2]);
                    FileInputStream fileInputStream = new FileInputStream(fileToSend);
                    InputStream inputFile = new BufferedInputStream(fileInputStream);
                
                    byte[] buffer = new byte[1024];
                    outStream.writeObject((int) fileToSend.length());
                    int bytesRead;
                    
                    // enviar ficheiro
                    while ((bytesRead = inputFile.read(buffer)) != -1) {
                        outStream.write(buffer, 0, bytesRead); //send file
                    }

                    fileInputStream.close();

                } else if (splitCommand[0].equals("view") || splitCommand[0].equals("v")) {

                    outStream.writeObject(command); //send command

                    int filesize;
                    int bytesRead;
                    byte[] buffer = new byte[1024];

                    File f = new File(CLIENT_DIR + splitCommand[1]);
                    FileOutputStream fileOutputStream = new FileOutputStream(f);
                    OutputStream outputFile = new BufferedOutputStream(fileOutputStream);

                    try {
                        filesize = (int) inStream.readObject();
                        int totalsize = filesize;

                        //receive file
                        while (totalsize > 0 ) {
                            if(totalsize >= 1024) {
                                bytesRead = inStream.read(buffer,0,1024);
                            } else {
                                bytesRead = inStream.read(buffer,0,totalsize);
                            }
                            outputFile.write(buffer,0,bytesRead);
                            totalsize -= bytesRead;
                        }

                        outputFile.close();
                        fileOutputStream.close();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }

                } else {
                    outStream.writeObject(command); //send command
                }

                //print powering off
                if (command.equals("exit")) {
                    System.out.println("\n" + inStream.readObject());
                    sc.close();
                    clientSocket.close();
                    System.exit(0);

                } else {
                    //print answer
                    System.out.println("\n" + inStream.readObject());
                }
            }            

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
