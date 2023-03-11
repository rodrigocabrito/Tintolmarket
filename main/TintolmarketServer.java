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
import Tintolmarket.exceptions.NotEnoughBudgetException;
import Tintolmarket.exceptions.UtilizadorNotFoundException;
import Tintolmarket.exceptions.WineDuplicatedException;
import Tintolmarket.exceptions.WineNotFoundException;
import Tintolmarket.exceptions.NotEnoughWineQuantityException;

/*
 * @authors:
 *      Rodrigo Cabrito 54455
 *      João Costa 54482
 *      João Fraga 44837
 */
//TODO talk with buffer, remove exceptions e adicionar if elses
//TODO client(read objects, ciclos)

public class TintolmarketServer {
    
    private static int port;

    //authentication.txt
    static BufferedReader brAuth = null; //may not be used
    static BufferedWriter bwAuth = null;

    //balance.txt
    static BufferedReader brBal = null;
    static BufferedWriter bwBal = null;

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

            Socket clientSocket;
            clientHandlerThread clientThread = null;

			try {
				clientSocket = serverSocket.accept();
				clientThread = new clientHandlerThread(clientSocket);
                clientThread.start();
		    }
		    catch (IOException e) {
		        e.printStackTrace();
		    } finally {
                updateDataBases(clientThread);
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

            brBal = new BufferedReader(new FileReader("data_bas/balance.txt"));
            fillListaUts(brBal);
            brBal.close();

        } catch (FileNotFoundException e) {
            
            System.out.println("File not found, creating file...");
            //file doesn't exist, create it
            try {

                FileWriter fw = new FileWriter("data_base/balance.txt");
                fw.close();

                brBal = new BufferedReader(new FileReader("data_base/balance.txt"));
                fillListaUts(brBal);
                brBal.close();

            } catch (IOException ex) {
                System.out.println("Failed to create file.");
                ex.printStackTrace();
            }

        } finally {
            
            if (brBal != null) {
                try {
                    brBal.close();
                } catch (IOException e) {
                e.printStackTrace();
                }
            }
        }

        //update listaWines
        try {

            brWine = new BufferedReader(new FileReader("data_bas/wines.txt"));
            fillListaWines(brWine);
            brWine.close();

        } catch (FileNotFoundException e) {
            
            System.out.println("File not found, creating file...");
            //file doesn't exist, create it
            try {

                FileWriter fw = new FileWriter("data_base/wines.txt");
                fw.close();

                brWine = new BufferedReader(new FileReader("data_base/wines.txt"));
                fillListaWines(brWine);
                brWine.close();

            } catch (IOException ex) {
                System.out.println("Failed to create file.");
                ex.printStackTrace();
            }

        } finally {
            
            if (brWine != null) {
                try {
                    brWine.close();
                } catch (IOException e) {
                e.printStackTrace();
                }
            }
        }
        try {

            brSale = new BufferedReader(new FileReader("data_bas/forSale.txt"));
            fillListaForSale(brSale);
            brSale.close();

        } catch (FileNotFoundException e) {

            System.out.println("File not found, creating file...");
            //file doesn't exist, create it
            try {

                FileWriter fw = new FileWriter("data_base/forSale.txt");
                fw.close();

                brSale = new BufferedReader(new FileReader("data_base/forSale.txt"));
                fillListaForSale(brSale);
                brSale.close();

            } catch (IOException ex) {
                System.out.println("Failed to create file.");
                ex.printStackTrace();
            }

        } finally {

            if (brSale != null) {
                try {
                    brSale.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void updateDataBases(clientHandlerThread clientThread) {
        
        //update balance.txt (balance of all Utilizador)
        try {

            bwBal = new BufferedWriter(new FileWriter("data_bas/balance.txt"));
            brBal = new BufferedReader(new FileReader("data_bas/balance.txt"));

            Utilizador ut = listaUts.get(listaUts.indexOf(clientThread.getUtilizador()));
            String line;

            while ((line = brBal.readLine()) != null) {

                String[] splitLine = line.split(":");
                if (line.contains(ut.getUserID())) {
                    bwBal.write(splitLine[0] + ";" + ut.getBalance() + "\n"); //utilizador;saldo
                } else {
                    bwBal.write(line + "\n");
                }
            }

            bwBal.close();
            brBal.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        //update Wines.txt
        try {
            brWine = new BufferedReader(new FileReader("data_bas/wines.txt"));
            bwWine = new BufferedWriter(new FileWriter("data_bas/wines.txt"));

            for (Wine wine : listaWines) {

                StringBuilder sb = new StringBuilder();
                for (Integer j : wine.getStars()) {
                    sb.append(j);
                    sb.append(";");
                }                        
                String stars = sb.toString();
                //nome;imagem;star1;star2;star3...
                bwWine.write(wine.getName() + ";" + wine.getImage() + ";" + stars + "\n");
            }
            brWine.close();
            bwWine.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        //update forSale.txt
        try {
            bwSale = new BufferedWriter(new FileWriter("data_bas/forSale.txt"));

            for (Utilizador ut : forSale.keySet()) {

                Triplet sale = forSale.get(ut);
                //seller;wine;price;quantity
                bwSale.write(ut.getUserID() + ";" + sale.wine + ";" + sale.value + ";" + sale.quantity + "\n");
            }

            bwSale.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * for each line in balance.txt, create an Utilizador and add to listaUts
     */
    private static void fillListaUts(BufferedReader br) throws NumberFormatException, IOException {
        String line;
        while((line = br.readLine()) != null){

            String[] lineSplit = line.split(";");
            listaUts.add(new Utilizador(lineSplit[0], Integer.parseInt(lineSplit[1])));
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

    /*
     * for each line in forSale.txt, create a Sale and add to hashmap forSale(Utilizador,Triplet)
     */
    private static void fillListaForSale(BufferedReader br) throws NumberFormatException, IOException{
        String line;
        while((line=br.readLine()) != null){
            String[]splitLine = line.split(";");

            Utilizador utSale = null;
            for (Utilizador u:listaUts) {
                if(u.getUserID().equals(splitLine[0])){
                    utSale = u;
                }
            }

            Wine wSale = null;
            for (Wine w:listaWines) {
                if(w.getName().equals(splitLine[1])){
                    wSale = w;
                }
            }

            forSale.put(utSale,new Triplet(wSale,Integer.parseInt(splitLine[2]),Integer.parseInt(splitLine[2])));
        }
        System.out.println("listaForSale updated");
    }

    /*
     * Classe que representa todos os argumentos de uma venda para alem do vendedor (Wine,price,quantity)
     */
    static class Triplet {
        private final Wine wine;
        private int value;
        private int quantity;

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

        public void updateQuantity(int delta) {
            this.quantity += delta;
        }

        public void updateValue(int price) {
            this.value = price;
        }
    }

    /*
     * classe para criar threads para vários clientes comunicarem com o servidor
     */
    static class clientHandlerThread extends Thread {

        private Socket socket;
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

                    ut = new Utilizador(clientID, 200);
                    listaUts.add(ut);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                // writer/reader -> authentication.txt
                bwAuth = new BufferedWriter(new FileWriter("data_base/authentication.txt"));
                Scanner sc = new Scanner("data_base/authentication");

                boolean registered = false;
                String line = " : ";

                while (sc.hasNextLine() && !registered) {

                    line = sc.next();
                    if (line.contains(clientID)) {
                        registered = true;
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
                    boolean exit = false;

                    while(!exit) {

                        outStream.writeBytes(menu());
                        command = (String) inStream.readObject();

                        //check if client wants more commands and if command as valid <check>
                        if(command.equals("exit")) {
                            exit = true;
                        } else {
                            //check if command is valid and operate as expected
                            process(command);
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

                    outStream.writeBytes("Powering off... \n");

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
        private void process(String command) throws IOException{
            String[] splitCommand = command.split(" ");
            boolean sucess = false;

            //add
            if (splitCommand[0].equals("add") || splitCommand[0].equals("a")) {
                
                for (Wine w : listaWines) {
                    if (w.getName().equals(splitCommand[1])) {
                        outStream.writeBytes("O vinho que deseja adicionar ja existe! \n");
                    } else {
                        listaWines.add(new Wine(splitCommand[1], splitCommand[2], new ArrayList<>()));
                        sucess = true;
                        outStream.writeBytes("Vinho adicionado com sucesso! \n");
                    }
                }
            }

            //sell
            if (splitCommand[0].equals("sell") || splitCommand[0].equals("s")) {
                
                boolean contains = false;
                Wine wine = null;
                for (Wine w : listaWines) {
                    if (wine.getName().equals(splitCommand[1]) && !contains){
                        wine = w;
                        contains = true;
                    }
                }

                if(!contains) {
                    outStream.writeBytes("O vinho que deseja vender nao existe! \n");
                } else {

                    boolean toUpdate = false;
                    for (Utilizador u: forSale.keySet()) {

                        Triplet t = forSale.get(u);
                        if(t.getWine().getName().equals(splitCommand[1])){//caso o vinho ja esteja a venda(mesmo ut)
                            t.updateValue(Integer.parseInt(splitCommand[2]));//muda o preço
                            toUpdate = true;
                        }
                    }
                    if(!toUpdate) {
                        forSale.put(ut, new Triplet(wine, Integer.parseInt(splitCommand[2]), Integer.parseInt(splitCommand[3])));
                    }
                    sucess = true;
                    outStream.writeBytes("Vinho colocado a venda! \n");
                }
            }

            //view
            if (splitCommand[0].equals("view") || splitCommand[0].equals("v")) {
                
                boolean contains = false;
                Wine w = null;
                for (Wine wine : listaWines) {
                    if (wine.getName().equals(splitCommand[1]) && !contains){
                        contains = true;
                        w = wine;
                    }
                }

                if(!contains) {
                    outStream.writeBytes("O vinho que deseja ver nao existe! \n");
                } else {

                    StringBuilder sb = new StringBuilder();
                    sb.append("Nome: " + w.getName() + "\n");
                    sb.append("Imagem: " + w.getImage() + "\n");
                    sb.append("Classificacao media: " + w.getAvgRate() + "\n");

                    for (Utilizador ut : forSale.keySet()) {

                        Triplet sale = forSale.get(ut);
                        if(w.getName().equals(sale.getWine().getName()) && sale.getQuantity() > 0) {

                            sb.append("-------------------------------------- \n");
                            sb.append("Vendedor: " + ut.getUserID() + "\n");
                            sb.append("Preco: " + sale.getValue() + "\n");
                            sb.append("Quantidade: " + sale.getQuantity() + "\n");
                        }
                    }
                    sucess = true;
                    outStream.writeBytes(sb.toString());
                }
            }

            //buy
            if (splitCommand[0].equals("buy") || splitCommand[0].equals("b")) {
                
                boolean contains = false;
                for (Wine wine : listaWines) {
                    if (wine.getName().equals(splitCommand[1]) && !contains){
                        contains = true;
                    }
                }

                if(!contains) {
                    outStream.writeBytes("O vinho que deseja comprar nao existe! \n");
                } else {

                    boolean sold = false;//boolean to check if sale was already made
                    for (Utilizador utSeller : forSale.keySet()) {

                        if(utSeller.getUserID().equals(splitCommand[2]) && !sold) {// find seller

                            Triplet sale = forSale.get(utSeller);//venda encontrada a partir do seller
                            if(sale.getQuantity() < Integer.parseInt(splitCommand[3])) {
                                outStream.writeBytes("Nao existe quantidade suficiente deste vinho para venda! \n");

                            } else if(ut.getBalance() < sale.getValue()* (Integer.parseInt(splitCommand[3]))) { 
                                outStream.writeBytes("Saldo insuficiente! \n");

                            } else {
                                sale.updateQuantity(-(Integer.parseInt(splitCommand[3])));
                                ut.updateWallet(-(sale.getValue() * (Integer.parseInt(splitCommand[3]))));
                                sold = true;
                                sucess = true;
                                outStream.writeBytes("Compra realizada com sucesso! \n");//yey
                            } 
                        }
                    }
                }
            }

            //wallet
            if (splitCommand[0].equals("wallet") || splitCommand[0].equals("w")) {
                sucess = true;
                outStream.write(ut.getBalance());
            }

            //classify
            if (splitCommand[0].equals("classify") || splitCommand[0].equals("c")) {
                
                boolean contains = false;

                for (Wine wine : listaWines) {
                    if (wine.getName().equals(splitCommand[1]) && !contains){
                        wine.classify(Integer.parseInt(splitCommand[2]));
                        contains = true;
                        outStream.writeBytes("Vinho classificado! \n");
                        sucess = true;
                    }
                }

                if(!contains) {
                    outStream.writeBytes("O vinho que deseja classificar nao existe! \n");
                }
            }

            //talk
            if (splitCommand[0].equals("talk") || splitCommand[0].equals("t")) {
                
                boolean contains = false;

                for (Utilizador u : listaUts) {
                    if(u.getUserID().equals(splitCommand[1]) && !contains) {
                        contains = true;
                    }
                }

                if(!contains) {
                    outStream.writeBytes("O utilizador nao existe! \n");
                } else {

                    try {

                        bwChat = new BufferedWriter(new FileWriter("data_bases/chat.txt"));
                        bwChat.write(ut.getUserID() + ";" + splitCommand[1] + ";" + splitCommand[2] + "\n");
                        outStream.writeBytes("Mensagem enviada! \n");

                        sucess = true;

                        bwChat.close();
                    } catch (Exception e) {
                        outStream.writeBytes("Erro no envio da mensagem! \n");
                        e.printStackTrace();
                    }
                }
            }

            //read
            if (splitCommand[0].equals("read") || splitCommand[0].equals("r")) {
                
                try {
                    brChat = new BufferedReader(new FileReader("data_bases/chat.txt"));
                    String line;

                    while ((line = brChat.readLine()) != null) {

                        String[] splitLine = line.split(";");
                        if (splitLine[1].equals(ut.getUserID())) {

                            outStream.writeBytes("mensagem de" + splitLine[0] + " : " + splitLine[2] + "\n");
                            bwChat.write("" + "\n");   //remove msg from server
                        } else {
                            bwChat.write(line + "\n");
                        }
                    }

                    sucess = true;

                    brChat.close();
                    bwChat.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (!sucess) {
                outStream.writeBytes(("Comando Inválido! Indique um dos comandos apresentados acima. \n"));
            }
            
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
