package Tintolmarket.main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import Tintolmarket.domain.Utilizador;
import Tintolmarket.domain.Wine;
import Tintolmarket.exceptions.UtilizadorNotFoundException;
import Tintolmarket.exceptions.WineDuplicatedException;
import Tintolmarket.exceptions.WineNotFoundException;

/*
 * @authors:
 *      Rodrigo Cabrito 54455
 *      João Costa 54482
 *      João Fraga 44837
 */

public class TintolmarketServer {
    
    private static int port;

    //authentication.txt
    static BufferedReader brAuth = null;
    static BufferedWriter bwAuth = null;

    //wines.txt
    static BufferedReader brWine = null;
    static BufferedWriter bwWine = null;

    //forSale.txt
    static BufferedReader brSale = null;
    static BufferedWriter bwSale = null;

    //chat.txt
    static BufferedReader brChat = null;
    static BufferedWriter bwChat = null;

    private static ArrayList<Utilizador> listaUts = new ArrayList<>();
    private static ArrayList<Wine> listaWines = new ArrayList<>();
    private static HashMap<Utilizador,Triplet> forSale = new HashMap<>();
    //private static ArrayList<Triplet> forSale = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        
        //update structures in server from .txt files
        updateServerMemory();

        if (args.length == 0) {
            port = 12345;
        }
        
        port = Integer.parseInt(args[0]);
        
        ServerSocket serverSocket = null;

        try {

            serverSocket = new ServerSocket(port);

        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }

        while(true) {

            Socket clientSocket = null;
            clientHandlerThread clientThread = null;

			try {
				clientSocket = serverSocket.accept();
				clientThread = new clientHandlerThread(clientSocket);
                clientThread.start();
		    }
		    catch (IOException e) {
		        e.printStackTrace();
		    } finally {
                updateDataBases(clientThread); //TODO
            }
        }

        //serverSocket.close();
    }

    /*
     * updates the structures in the server memory
     */
    private static void updateServerMemory() throws IOException {
        
        //update listaUts
        
        try {

            brAuth = new BufferedReader(new FileReader("data bases/authentication.txt"));
            
            //do something with the file
            fillListaUts(brAuth);
        

        } catch (FileNotFoundException e) {
            
            System.out.println("File not found, creating file...");
            //file doesnt exist, create it
            try {

                FileWriter fw = new FileWriter("data bases/authentication.txt");
                fw.close();

                brAuth = new BufferedReader(new FileReader("data bases/authentication.txt"));

                //do something with file
                fillListaUts(brAuth);

            } catch (IOException ex) {
                System.out.println("Failed to create file.");
                ex.printStackTrace();
            }

        } finally {
            
            if (brAuth != null) {
                try {
                    brAuth.close();
                } catch (IOException e) {
                e.printStackTrace();
                }
            }
        }

        //update listaWines
        try {

            brAuth = new BufferedReader(new FileReader("data bases/wines.txt"));
            
            //do something with the file
            fillListaWines(brAuth);
        

        } catch (FileNotFoundException e) {
            
            System.out.println("File not found, creating file...");
            //file doesnt exist, create it
            try {

                FileWriter fw = new FileWriter("data bases/wines.txt");
                fw.close();

                brAuth = new BufferedReader(new FileReader("data bases/wines.txt"));

                //do something with file
                fillListaWines(brAuth);

            } catch (IOException ex) {
                System.out.println("Failed to create file.");
                ex.printStackTrace();
            }

        } finally {
            
            if (brAuth != null) {
                try {
                    brAuth.close();
                } catch (IOException e) {
                e.printStackTrace();
                }
            }
        }
    }

    private static void updateDataBases(clientHandlerThread clientThread) {
        
        //update authentication.txt (balance of current Utilizador)
        try {

            bwAuth = new BufferedWriter(new FileWriter("data bases/authentication.txt"));
            brAuth = new BufferedReader(new FileReader("data bases/authentication.txt"));

            Utilizador ut = listaUts.get(listaUts.indexOf(clientThread.getUtilizador()));
            int saldo = ut.getBalance();
            String line;

            while ((line = brAuth.readLine()) != null) {

                String[] splitLine = line.split(":");
                if (line.contains(ut.getUserID())) {
                    bwAuth.write(splitLine[0] + ":" + splitLine[1] + saldo + "\n");
                } else {
                    bwAuth.write(line + "\n");
                }
            }

            bwAuth.close();
            brAuth.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        //update Wines.txt
        try {
            brWine = new BufferedReader(new FileReader("data bases/wines.txt"));
            bwWine = new BufferedWriter(new FileWriter("data bases/wines.txt"));

            for (Wine wine : listaWines) {

                StringBuilder sb = new StringBuilder();
                for (Integer j : wine.getStars()) {
                    sb.append(j);
                    sb.append(";");
                }                        
                String stars = sb.toString();

                bwWine.write(wine.getName() + ";" + wine.getImage() + ";" + stars + "\n");
            }
            brWine.close();
            bwWine.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        //update forSale.txt
        try {
            bwSale = new BufferedWriter(new FileWriter("data bases/forSale.txt"));

            for (Utilizador ut : forSale.keySet()) {

                Triplet sale = forSale.get(ut);

                bwSale.write(ut.getUserID() + ";" + sale.wine + ";" + sale.value + ";" + sale.quantity + "\n");
            }

            bwSale.close();

        } catch (Exception e) {
            e.printStackTrace();;
        }
    }

    /*
     * for each line in authentication.txt, create an Utilizador and add to listaUts
     */
    private static void fillListaUts(BufferedReader br) throws NumberFormatException, IOException {
        String line;
        while((line = br.readLine()) != null){

            String[] lineSplit = line.split(":");
            listaUts.add(new Utilizador(lineSplit[0], Integer.parseInt(lineSplit[2])));
        }
        System.out.println("listaUts updated");
    }

    /*
     * for each line in wines.txt, create a Wine and add to listaWines
     */
    private static void fillListaWines(BufferedReader br) throws NumberFormatException, IOException{
        String line;
        while((line = br.readLine()) != null){

            String[] lineSplit = line.split(";");
            ArrayList<Integer> stars = new ArrayList<>();

            for (int i = 2; i < lineSplit.length; i++) {
                stars.add(Integer.parseInt(lineSplit[i]));
            }

            listaWines.add(new Wine(lineSplit[0], lineSplit[1], stars));
        }
        System.out.println("listaWines updated");
    }

    class Triplet {
        private Wine wine = null; 
        private int value = 0; 
        private int quantity = 0;

        public Triplet(Wine wine, int value, int quantity) { 
            this.wine = wine; 
            this.value = value; 
            this.quantity = quantity;
        }

        public Wine getWine() {
            return this.wine;
        }

        public int getValue() {
            return this.value;
        }

        public int getQuantity() {
            return this.quantity;
        }

        /*
         * 
         */
        public void update(int delta) {
            this.quantity += delta;
        }
    }

    /*
     * classe para criar threads para vários clientes comunicarem com o servidor
     */
    static class clientHandlerThread extends Thread {

        private Socket socket = null;
        private Utilizador ut = null;

        ObjectOutputStream outStream;
        ObjectInputStream inStream;

        clientHandlerThread(Socket socket) {
            this.socket = socket;
        }

        
        public void run() {

            //streams
            try {

                ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
			    ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

                String clientID = null;
                String passwd = null;

                try {

                    clientID = (String) inStream.readObject();
                    passwd = (String) inStream.readObject();
                    System.out.println("user e passw recebidos");

                    //TODO utilizador
                    ut = new Utilizador(clientID, 200);
                    listaUts.add(ut);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                // writer/reader -> authentication.txt
                bwAuth = new BufferedWriter(new FileWriter("data bases/authentication.txt"));
                Scanner sc = new Scanner("data bases/authentication");

                boolean registered = false;
                String line = " : ";

                while (sc.hasNextLine()) {

                    line = sc.next();
                    if (line.contains(clientID)) {
                        registered = true;
                        break;
                    }   
                }

                //check if registered
                if (!registered) {
                    
                    bwAuth.write(clientID + ":" + passwd + "\n");
                
                    bwAuth.close();
                    sc.close();
                }

                //authentication
                String[] splitLine = line.split(":");
                if (splitLine[1].equals(passwd) || !registered) {
                    
                    String command;
                    outStream.writeBytes(menu());

                    //check if client wants more commands
                    while(!(command = ((String) inStream.readObject())).equals("exit")) {   //TODO exit

                        //boolean to evaluate if the command was success
                        boolean check = true;
                        while(check) {

                            //check if command is valid and operate as expected
                            int i = process(command);
                            if (i != -9998) {
                                check = false;
                                outStream.writeBytes("Operacao realizada com sucesso!");
                            }
                            else {
                                outStream.writeBytes(("Comando Inválido! Indique um dos comandos apresentados acima."));
                            }
                        }
                    }

                } else {
                    System.err.println("Username ou password incorretos!");
                    System.exit(-2);
                }

            } catch (Exception e) {
                e.printStackTrace();

            } finally {

                try {

                    outStream.writeBytes("Powering off...");

                    //close streams and socket
                    if (outStream != null) {
                        outStream.close();   
                    }
                    if (inStream != null) {
                        inStream.close();
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
        }


        /*
         * App functionalities
         */
        private int process(String command) throws WineDuplicatedException, WineNotFoundException, UtilizadorNotFoundException {
            String[] splitCommand = command.split(" ");

            //add
            if (splitCommand[0].equals("add") || splitCommand[0].equals("a")) {
                
                for (Wine w : listaWines) {
                    if (w.getName().equals(splitCommand[1])) {
                        throw new WineDuplicatedException("O vinho que deseja adicionar ja existe!");
                    }
                }

                listaWines.add(new Wine(splitCommand[1], splitCommand[2], new ArrayList<Integer>()));
                
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

                //TODO

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


        private Utilizador getUtilizador() {
            return this.ut;
        }

        /*
         * prints a menu with some commands
         */
        private String menu() {

            StringBuilder sb = new StringBuilder();

            sb.append("-------------------------------------------\n");
            sb.append(" -> add <wine> <image>\n");
            sb.append(" -> sell <wine> <value> <quantity>\n");
            sb.append(" -> view <wine>\n");
            sb.append(" -> buy <wine> <seller> <quantity>\n");
            sb.append(" -> wallet\n");
            sb.append(" -> classify <wine> <stars>\n");
            sb.append(" -> talk <user> <message>\n");
            sb.append(" -> read\n");
            sb.append(" -> exit\n");
            sb.append(" Selecione um comando: ");

            return sb.toString();
        }
    }
}
