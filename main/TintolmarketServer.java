package Tintolmarket.main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

import Tintolmarket.domain.Utilizador;
import projeto1.dominio.Wine;
import projeto1.exceptions.UtilizadorNotFoundException;
import projeto1.exceptions.WineDuplicatedException;
import projeto1.exceptions.WineNotFoundException;

/*
 * @authors:
 *      Rodrigo Cabrito 54455
 *      João Costa 54482
 *      João Fraga 44837
 */

public class TintolmarketServer {
    
    private static int port;
    private static File f = new File("authenticaion.txt");

    private static ArrayList<Utilizador> listaUts = new ArrayList<>();
    private static ArrayList<Wine> listaWines = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        
        if (args.length == 0) {
            port = 12345;
        }
        
        port = Integer.parseInt(args[1]);
        
        ServerSocket serverSocket = null;

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
        private Utilizador ut = null;

        clientHandlerThread(Socket socket) {
            this.socket = socket;
        }

        
        public void run() {

            //streams
            try {

                ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
			    ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

                String clientID = null;
                String passw = null;

                try {

                    clientID = (String) inStream.readObject();
                    passw = (String) inStream.readObject();
                    System.out.println("user e passw recebidos");

                    //TODO utilizador
                    ut = new Utilizador(clientID);
                    listaUts.add(ut);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                BufferedWriter bw = new BufferedWriter(new PrintWriter(f));
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));

                boolean registered = false;
                String line = null;

                while (true) {

                    line = br.readLine();
                    if (line.contains(clientID)) {
                        registered = true;
                        break;
                    }   
                }

                //check if registered
                if (!registered) {
                    
                    bw.write(clientID + ":" + passw + "\n");
                
                    bw.close();
                    br.close();
                }

                //authentication
                String[] splitLine = line.split(":");
                if (splitLine[1].equals(passw)) {

                    Scanner sc = new Scanner(System.in);

                    while(true) {

                        printMenu();
                        String command = sc.next();

                        //check if command is valid and operate as expected
                        int i = process(command);
                        if (i != -9998)
                            break; 

                        System.out.println("Comando Inválido! Indique um dos comandos apresentados acima.");
                    }

                    sc.close();


                } else {
                    System.err.println("Username ou password incorretos!");
                    System.exit(-2);

                    outStream.close();
				    inStream.close();
                    socket.close();
                }

                //close streams and socket

            } catch (Exception e) {
                e.printStackTrace();
            }
            
        }

        private int process(String command) throws WineDuplicatedException, WineNotFoundException, UtilizadorNotFoundException {
            String[] splitCommand = command.split(" ");

            //add
            if (splitCommand[0].equals("add") || splitCommand[0].equals("a")) {
                
                for (Wine w : listaWines) {
                    if (w.getName().equals(splitCommand[1])) {
                        throw new WineDuplicatedException("O vinho que deseja adicionar ja existe!");
                    }
                }

                listaWines.add(new Wine(splitCommand[1], splitCommand[2]));
                
                return -9999;
            }

            //sell
            if (splitCommand[0].equals("sell") || splitCommand[0].equals("s")) {
                
                boolean contains = false;
                for (Wine wine : listaWines) {
                    if (wine.getName().equals(splitCommand[1])){
                        contains = true;
                        break;
                    }
                }

                if(!contains) {
                    throw new WineNotFoundException("O vinho que deseja vender nao existe!");
                }

                //TODO

                return -9999;
            }

            //view
            if (splitCommand[0].equals("view") || splitCommand[0].equals("v")) {
                
                boolean contains = false;
                for (Wine wine : listaWines) {
                    if (wine.getName().equals(splitCommand[1])){
                        contains = true;
                        break;
                    }
                }

                if(!contains) {
                    throw new WineNotFoundException("O vinho que deseja ver nao existe!");
                }

                //TODO

                return -9999;
            }

            //buy
            if (splitCommand[0].equals("buy") || splitCommand[0].equals("b")) {
                
                boolean contains = false;
                for (Wine wine : listaWines) {
                    if (wine.getName().equals(splitCommand[1])){
                        contains = true;
                        break;
                    }
                }

                if(!contains) {
                    throw new WineNotFoundException("O vinho que deseja comprar nao existe!");
                }

                //TODO

                return -9999;
            }

            //wallet
            if (splitCommand[0].equals("wallet") || splitCommand[0].equals("w")) {
                return ut.getBalance();
            }

            //classify
            if (splitCommand[0].equals("classify") || splitCommand[0].equals("c")) {
                
                boolean contains = false;

                for (Wine wine : listaWines) {
                    if (wine.getName().equals(splitCommand[1])){
                        wine.classify(Integer.parseInt(splitCommand[2]));
                        contains = true;
                        break;
                    }
                }

                if(!contains) {
                    throw new WineNotFoundException("O vinho que deseja classificar nao existe!");
                }

                return -9999;
            }

            //talk
            if (splitCommand[0].equals("talk") || splitCommand[0].equals("t")) {
                
                boolean contains = false;

                for (Utilizador ut : listaUts) {
                    if(ut.getUserID().equals(splitCommand[1])) {
                        contains = true;
                        break;
                    }
                }

                if(!contains) {
                    throw new UtilizadorNotFoundException("O utilizador nao existe!");
                }

                //TODO
                return -9999;
            }

            //read
            if (splitCommand[0].equals("read") || splitCommand[0].equals("r")) {
                
                //TODO
                return -9999;
            }

            return -9998;
        }

        /*
         * prints a menu with some commands
         */
        private void printMenu() {
            System.out.println("-------------------------------------------");
            System.out.println(" -> add <wine> <image>");
            System.out.println(" -> sell <wine> <value> <quantity>");
            System.out.println(" -> view <wine>");
            System.out.println(" -> buy <wine> <seller> <quantity>");
            System.out.println(" -> wallet");
            System.out.println(" -> classify <wine> <stars>");
            System.out.println(" -> talk <user> <message>");
            System.out.println(" -> read");
            System.out.print(" Selecione um comando: ");
        }
    }
}
