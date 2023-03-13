
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
import java.util.Arrays;
import java.util.HashMap;

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
    private static HashMap<Utilizador,ArrayList<Sale>> forSale = new HashMap<>();

    public static void main(String[] args) throws IOException {
        
        //update structures in server from .txt files
        updateServerMemory();

        if (args.length == 0) {
            port = 12345;
        } else {
            port = Integer.parseInt(args[0]);
        }
           
        
        ServerSocket serverSocket = null;

        try {

            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);

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

            brBal = new BufferedReader(new FileReader("balance.txt"));
            fillListaUts(brBal);
            brBal.close();

        } catch (FileNotFoundException e) {
            
            System.out.println("File not found, creating file...");
            //file doesn't exist, create it
            try {

                FileWriter fw = new FileWriter("balance.txt");
                fw.close();

                brBal = new BufferedReader(new FileReader("balance.txt"));
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

            brWine = new BufferedReader(new FileReader("wines.txt"));
            fillListaWines(brWine);
            brWine.close();

        } catch (FileNotFoundException e) {
            
            System.out.println("File not found, creating file...");
            //file doesn't exist, create it
            try {

                FileWriter fw = new FileWriter("wines.txt");
                fw.close();

                brWine = new BufferedReader(new FileReader("wines.txt"));
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

            brSale = new BufferedReader(new FileReader("forSale.txt"));
            fillListaForSale(brSale);
            brSale.close();

        } catch (FileNotFoundException e) {

            System.out.println("File not found, creating file...");
            //file doesn't exist, create it
            try {

                FileWriter fw = new FileWriter("forSale.txt");
                fw.close();

                brSale = new BufferedReader(new FileReader("forSale.txt"));
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
     * for each line in forSale.txt, create a Sale and add to hashmap forSale(Utilizador,Sale)
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
            
            ArrayList<Sale> sales = new ArrayList<>();
            sales.add(new Sale(wSale,Integer.parseInt(splitLine[2]),Integer.parseInt(splitLine[2])));
            forSale.put(utSale,sales);
        }
        System.out.println("listaForSale updated");
    }

    /*
     * Classe que representa todos os argumentos de uma venda para alem do vendedor (Wine,price,quantity)
     */
    static class Sale {
        private final Wine wine;
        private int value;
        private int quantity;

        public Sale(Wine wine, int value, int quantity) { 
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
        private static Utilizador ut = null;

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
                    System.out.println("authentication recebida");

                    ut = new Utilizador(clientID, 200);
                    listaUts.add(ut);
                    System.out.println("utilizador criado e adicionado a listaUts");

                } catch (Exception e) {
                    e.printStackTrace();
                }

                // writer/reader -> authentication.txt
                bwAuth = new BufferedWriter(new FileWriter("authentication.txt"));
                brAuth = new BufferedReader(new FileReader("authentication.txt"));

                boolean registered = false;
                String line = " : ";

                while(((line = brAuth.readLine()) != null)  && !registered) {
                    if (line.contains(clientID)) {
                        registered = true;
                    }
                }
                brAuth.close();

                //check if registered
                if (!registered) {
                    
                    bwAuth.write(clientID + ":" + passwd + "\n");
                    bwAuth.close();
                }
                

                brAuth = new BufferedReader(new FileReader("authentication.txt"));
                boolean found = false;
                String lineForSplit = null;

                while(((line = brAuth.readLine()) != null)  && !found) {
                    if (line.contains(clientID)) {
                        lineForSplit = line;
                        found = true;
                    }
                }
                brAuth.close();

                //authentication
                String[] splitLine = lineForSplit.split(":");
                if (splitLine[1].equals(passwd)) {
                    
                    String command;
                    boolean exit = false;

                    while(!exit) {
                        
                        outStream.writeObject(menu());
                        command = (String) inStream.readObject();
                        System.out.println(command);

                        if(command.equals("exit")) {
                            exit = true;
                        } else {
                            process(command, outStream, listaUts, listaWines, forSale);
                            updateDataBases();
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

                    outStream.writeObject("Powering off... \n");

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

        private static void updateDataBases() {
        
            //update balance.txt (balance of all Utilizador)
            try {
    
                bwBal = new BufferedWriter(new FileWriter("balance.txt"));
                brBal = new BufferedReader(new FileReader("balance.txt"));
    
                Utilizador ut = listaUts.get(listaUts.indexOf(getUtilizador()));
                
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
                brWine = new BufferedReader(new FileReader("wines.txt"));
                bwWine = new BufferedWriter(new FileWriter("wines.txt"));
    
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
                bwSale = new BufferedWriter(new FileWriter("forSale.txt"));
    
                for (Utilizador ut : forSale.keySet()) {
    
                    ArrayList<Sale> sales = forSale.get(ut);

                    for (Sale sale : sales) {
                        //seller;wine;price;quantity
                        bwSale.write(ut.getUserID() + ";" + sale.wine + ";" + sale.value + ";" + sale.quantity + "\n");
                    }
                    
                }
    
                bwSale.close();
    
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /*
         * App functionalities
         */
        private void process(String command, ObjectOutputStream outStream, ArrayList<Utilizador> listaUts, ArrayList<Wine> listaWines, HashMap<Utilizador, ArrayList<TintolmarketServer.Sale>> forSale) throws IOException {
            String[] splitCommand = command.split(" ");
            boolean isValid = false;
            System.out.println(Arrays.toString(splitCommand));

            //add
            if (splitCommand[0].equals("add") || splitCommand[0].equals("a")) {
                isValid = true;  

                if (splitCommand.length != 3) {
                    outStream.writeObject("Comando invalido! Operacao add necessita de 2 argumentos <name> <image>");
                } else {            
                    if(listaWines.size() == 0) {

                        listaWines.add(new Wine(splitCommand[1], splitCommand[2], new ArrayList<>()));
                        outStream.writeObject("Vinho adicionado com sucesso! \n");

                    } else {
                        boolean contains = false;
                        for(Wine w : listaWines) {

                            if (w.getName().equals(splitCommand[1])) {
                                contains = true;
                                outStream.writeObject("O vinho que deseja adicionar ja existe! \n");
                            }
                        }

                        if (!contains) {
                            listaWines.add(new Wine(splitCommand[1], splitCommand[2], new ArrayList<>()));
                            outStream.writeObject("Vinho adicionado com sucesso! \n");
                        }                        
                    }
                }
            }

            //sell
            if (splitCommand[0].equals("sell") || splitCommand[0].equals("s")) {
                isValid = true;
                boolean contains = false;
                Wine wine = null;

                if (splitCommand.length != 3) {
                    outStream.writeObject("Comando invalido! Operacao sell necessita de 3 argumentos <name> <image> <quantity>");
                } else {
                    
                    if (listaWines.size() == 0) {
                        outStream.writeObject("O vinho que deseja vender nao existe! \n");

                    } else {
                        for (Wine w : listaWines) {
                            if (wine.getName().equals(splitCommand[1]) && !contains){
                                wine = w;
                                contains = true;
                            }
                        }

                        if(!contains) {
                            outStream.writeObject("O vinho que deseja vender nao existe!");

                        } else if (forSale.size() == 0) {
                            ArrayList<Sale> sales = new ArrayList<>();
                            sales.add(new Sale(wine, Integer.parseInt(splitCommand[2]), Integer.parseInt(splitCommand[3])));
                            forSale.put(ut, sales);
                            outStream.writeObject("Vinho colocado a venda!");

                        } else {
                            boolean toUpdate = false;
                            for (Utilizador u: forSale.keySet()) {

                                if(u.equals(ut)) {
                                
                                    ArrayList<Sale> sales = forSale.get(u);
                                    for (Sale sale : sales) {
                                        if(sale.getWine().getName().equals(splitCommand[1])){ //caso o vinho ja esteja a venda(mesmo ut)

                                            sale.updateValue(Integer.parseInt(splitCommand[2])); //muda o preço
                                            sale.updateQuantity(Integer.parseInt(splitCommand[3])); //incrementa a quantidade
                                            toUpdate = true;
                                        }
                                    }
                                }
                            }

                            if(!toUpdate) {

                                System.out.println("antes" + forSale.size());
                                ArrayList<Sale> sales = new ArrayList<>();
                                sales.add(new Sale(wine, Integer.parseInt(splitCommand[2]), Integer.parseInt(splitCommand[3])));
                                forSale.put(ut, sales);
                            }
                            
                            outStream.writeObject("Vinho colocado a venda! \n");
                            System.out.println("depois" + forSale.size());
                        }
                    }
                }
            }

            //view
            if (splitCommand[0].equals("view") || splitCommand[0].equals("v")) {
                isValid = true;
                System.out.println("entrou no view");
                boolean contains = false;
                Wine w = null;
                for (Wine wine : listaWines) {
                    if (wine.getName().equals(splitCommand[1]) && !contains){
                        contains = true;
                        w = wine;
                    }
                }

                if(!contains) {
                    outStream.writeObject("O vinho que deseja ver nao existe! \n");
                } else {

                    StringBuilder sb = new StringBuilder();
                    sb.append("Nome: " + w.getName() + "\n");
                    sb.append("Imagem: " + w.getImage() + "\n");
                    sb.append("Classificacao media: " + w.getAvgRate() + "\n");

                    for (Utilizador ut : forSale.keySet()) {
                        ArrayList<Sale> sales = forSale.get(ut);
                        for(Sale sale : sales) {
                            if(w.getName().equals(sale.getWine().getName()) && sale.getQuantity() > 0) {

                                sb.append("-------------------------------------- \n");
                                sb.append("Vendedor: " + ut.getUserID() + "\n");
                                sb.append("Preco: " + sale.getValue() + "\n");
                                sb.append("Quantidade: " + sale.getQuantity() + "\n");
                            }
                        }                        
                    }
                    
                    outStream.writeObject(sb.toString());
                }
            }

            //buy
            if (splitCommand[0].equals("buy") || splitCommand[0].equals("b")) {
                isValid = true;
                System.out.println("entrou no buy");
                boolean contains = false;
                for (Wine wine : listaWines) {
                    if (wine.getName().equals(splitCommand[1]) && !contains){
                        contains = true;
                    }
                }

                if(!contains) {
                    outStream.writeObject("O vinho que deseja comprar nao existe! \n");
                } else {

                    boolean sold = false;//boolean to check if sale was already made
                    for (Utilizador utSeller : forSale.keySet()) {

                        if(utSeller.getUserID().equals(splitCommand[2]) && !sold) {// find seller

                            ArrayList<Sale> sales = forSale.get(utSeller); //vendas encontradas a partir do seller
                            for(Sale sale : sales) {

                                if(sale.getQuantity() < Integer.parseInt(splitCommand[3])) {
                                    outStream.writeObject("Nao existe quantidade suficiente deste vinho para venda! \n");

                                } else if(ut.getBalance() < sale.getValue()* (Integer.parseInt(splitCommand[3]))) { 
                                    outStream.writeObject("Saldo insuficiente! \n");

                                } else {
                                    sale.updateQuantity(-(Integer.parseInt(splitCommand[3])));
                                    ut.updateWallet(-(sale.getValue() * (Integer.parseInt(splitCommand[3]))));
                                    sold = true;
                                    
                                    outStream.writeObject("Compra realizada com sucesso! \n");//yey
                                }
                            }
                        }
                    }
                }
            }

            //wallet
            if (splitCommand[0].equals("wallet") || splitCommand[0].equals("w")) {
                isValid = true;
                System.out.println("entrou no if wallet");
                outStream.writeObject(ut.getBalance());
            }

            //classify
            if (splitCommand[0].equals("classify") || splitCommand[0].equals("c")) {
                isValid = true;
                System.out.println("entrou no classify");
                boolean contains = false;

                for (Wine wine : listaWines) {
                    if (wine.getName().equals(splitCommand[1]) && !contains){
                        wine.classify(Integer.parseInt(splitCommand[2]));
                        contains = true;
                        outStream.writeObject("Vinho classificado! \n");
                       
                    }
                }

                if(!contains) {
                    outStream.writeObject("O vinho que deseja classificar nao existe! \n");
                }
            }

            //talk
            if (splitCommand[0].equals("talk") || splitCommand[0].equals("t")) {
                isValid = true;
                System.out.println("entrou no talk");
                boolean contains = false;

                for (Utilizador u : listaUts) {
                    if(u.getUserID().equals(splitCommand[1]) && !contains) {
                        contains = true;
                    }
                }

                if(!contains) {
                    outStream.writeObject("O utilizador nao existe! \n");
                } else {

                    try {

                        bwChat = new BufferedWriter(new FileWriter("chat.txt"));
                        bwChat.write(ut.getUserID() + ";" + splitCommand[1] + ";" + splitCommand[2] + "\n");
                        outStream.writeObject("Mensagem enviada! \n");

                        

                        bwChat.close();
                    } catch (Exception e) {
                        outStream.writeObject("Erro no envio da mensagem! \n");
                        e.printStackTrace();
                    }
                }
            }

            //read
            if (splitCommand[0].equals("read") || splitCommand[0].equals("r")) {
                isValid = true;
                System.out.println("entrou no read");
                try {
                    brChat = new BufferedReader(new FileReader("chat.txt"));
                    String line;

                    while ((line = brChat.readLine()) != null) {

                        String[] splitLine = line.split(";");
                        if (splitLine[1].equals(ut.getUserID())) {

                            outStream.writeObject("mensagem de" + splitLine[0] + " : " + splitLine[2] + "\n");
                            bwChat.write("" + "\n");   //remove msg from server
                        } else {
                            bwChat.write(line + "\n");
                        }
                    }

                    brChat.close();
                    bwChat.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (!isValid) {
                outStream.writeObject(("Comando Inválido! Indique um dos comandos apresentados acima. \n"));
            }
            
        }


        private static Utilizador getUtilizador() {
            return ut;
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
