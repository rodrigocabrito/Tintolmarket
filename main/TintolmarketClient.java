package Tintolmarket.main;

import java.net.Socket;

/*
 * @authors:
 *      Rodrigo Cabrito 54455
 *      João Costa 54482
 *      João Fraga 44837
 */

public class TintolmarketClient {
    
    private static int port = 9999;

    public static void main(String[] args) {
        
        try {
            
            Socket clientSocket = new Socket("",port);

            //streams

            //write/read to/from server

            //close streams and socket

        } catch (Exception e) {
            // TODO: handle exception
        }
    }
}
