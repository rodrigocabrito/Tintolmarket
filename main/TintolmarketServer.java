package Tintolmarket.main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/*
 * @authors:
 *      Rodrigo Cabrito 54455
 *      João Costa 54482
 *      João Fraga 44837
 */

public class TintolmarketServer {
    
    private static int port;

    public static void main(String[] args) throws IOException {
        
        ServerSocket serverSocket = null;
        System.out.println("branch");

        try {

            serverSocket = new ServerSocket(port);

        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }

        while(true) {

            Socket clientSocket = null;

			try {
				clientSocket = serverSocket.accept();
				clientHandlerThread clientThread = new clientHandlerThread(clientSocket);
                clientThread.start();
		    }
		    catch (IOException e) {
		        e.printStackTrace();
		    }

        //serverSocket.close();
        }
    }


    /*
     * classe para criar threads para vários clientes comunicarem com o servidor
     */
    static class clientHandlerThread extends Thread {

        private Socket socket = null;

        clientHandlerThread(Socket socket) {
            this.socket = socket;
        }

        
        public void run() {
            //streams

            //read/write from/to client

            //close streams and socket
        }
    }
}
