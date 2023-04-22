
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ServerSocketFactory;
import javax.net.ssl.*;


/**
 * @authors:
 *      Rodrigo Cabrito 54455
 *      João Costa 54482
 *      João Fraga 44837
 */

// TODO adicionar streams de cifras juntamente com as streams de leitura e escrita
// TODO decidir se é para cifrar todos os ficheiros ou só authentication

public class TintolmarketServer {

    // balance
    static BufferedReader brBal = null;
    static BufferedWriter bwBal = null;

    // wines
    static BufferedReader brWine = null;
    static BufferedWriter bwWine = null;

    // forSale
    static BufferedReader brSale = null;
    static BufferedWriter bwSale = null;

    // chat
    static BufferedReader brChat = null;
    static BufferedWriter bwChat = null;

    // file paths

    private static final String AUTHENTICATION_FILE_TXT = "./data_bases/authentication.txt";
    private static final String AUTHENTICATION_FILE_CIF = "./ciphers/authentication.cif";
    private static final String AUTHENTICATION_FILE_KEY = "./keys/authentication.key";
    private static final String BALANCE_FILE_TXT = "./data_bases/balance.txt";
    private static final String BALANCE_FILE_CIF = "./ciphers/balance.cif";
    private static final String BALANCE_FILE_KEY = "./keys/balance.key";
    private static final String WINES_FILE_TXT = "./data_bases/wines.txt";
    private static final String WINES_FILE_CIF = "./ciphers/wines.cif";
    private static final String WINES_FILE_KEY = "./keys/wines.key";
    private static final String FORSALE_FILE_TXT = "./data_bases/forSale.txt";
    private static final String FORSALE_FILE_CIF = "./ciphers/forSale.cif";
    private static final String FORSALE_FILE_KEY = "./keys/forSale.key";
    private static final String CHAT_FILE_TXT = "./data_bases/chat.txt";
    private static final String CHAT_FILE_CIF = "./ciphers/chat.cif";
    private static final String CHAT_FILE_KEY = "./keys/chat.key";

    private static final String SERVER_DIR = "./serverFiles/";
    private static final String SERVER_KEY_STORE_DIR = "./keystores/";

    private static final ArrayList<Utilizador> listaUts = new ArrayList<>();
    private static final ArrayList<Wine> listaWines = new ArrayList<>();
    private static final HashMap<Utilizador, ArrayList<Sale>> forSale = new HashMap<>();

    public static void main(String[] args) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException,
            InvalidKeyException, NoSuchPaddingException, KeyStoreException, CertificateException,
            UnrecoverableKeyException, KeyManagementException {

        int port = Integer.parseInt(args[0]);
        String password = args[1];
        String keyStorePath = args[2];
        String keyStorePassword = args[3];

        String truststorePath = SERVER_KEY_STORE_DIR + "mytruststore.jks";
        char[] truststorePassword = "your_truststore_password_here".toCharArray();

        // create keystore
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(new FileInputStream(keyStorePath), keyStorePassword.toCharArray());

        // Create a TrustStore containing the client's trusted certificates (if needed)
        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(new FileInputStream("client.truststore"), truststorePassword);

        // Create an SSLContext using the KeyManager and TrustManager obtained from the KeyStore and TrustStore, respectively
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, "key_password".toCharArray());

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());

        // TODO verificar salt e iterationCount param da funcao PBEKeySpec (20)
        // Generate the key based on the password passeed by args[1]
        byte[] salt = { (byte) 0xc9, (byte) 0x36, (byte) 0x78, (byte) 0x99, (byte) 0x52, (byte) 0x3e, (byte) 0xea, (byte) 0xf2 };
        PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 20); // passw, salt, iterations
        SecretKeyFactory kf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
        SecretKey key = kf.generateSecret(keySpec);

        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        updateServerMemory();

        try {
            BufferedReader br = new BufferedReader(new FileReader(CHAT_FILE_TXT));

            br.close();
        } catch (FileNotFoundException e) {
            // create chat.txt
            FileWriter f = new FileWriter(CHAT_FILE_TXT);
            f.close();
        }

        System.setProperty("javax.net.ssl.keyStore", keyStorePath);
        System.setProperty("javax.net.ssl.keyStorePassword", password);

        SSLServerSocket serverSocket = null;
        ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
        
        try {

            serverSocket = (SSLServerSocket) ssf.createServerSocket(port);

        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }

        while (true) {

            SSLSocket clientSocketSSL;
            clientHandlerThread clientSSLThread = null;

            try {

                clientSocketSSL = (SSLSocket) serverSocket.accept();

                SSLEngine sslEngine = sslContext.createSSLEngine();
                sslEngine.setUseClientMode(false);
                sslEngine.beginHandshake();
                SSLSession sslSession = sslEngine.getSession();

                clientSSLThread = new clientHandlerThread(clientSocketSSL, cipher, key);
                clientSSLThread.start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //serverSocket.close();
    }

    /**
     * Updates structures in server from .txt files
     * @throws IOException
     */
    private static void updateServerMemory() throws IOException, NoSuchAlgorithmException {

        // update listaUts

        try {

            brBal = new BufferedReader(new FileReader(BALANCE_FILE_TXT));

            fillListaUts(brBal);
            brBal.close();

        } catch (FileNotFoundException e) {

            // file doesn't exist, create it
            try {

                FileWriter fw = new FileWriter(BALANCE_FILE_TXT);
                fw.close();

                brBal = new BufferedReader(new FileReader(BALANCE_FILE_TXT));

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

        // update listaWines
        try {

            brWine = new BufferedReader(new FileReader(WINES_FILE_TXT));

            fillListaWines(brWine); // TODO swap for new streams
            brWine.close();

        } catch (FileNotFoundException e) {

            // file doesn't exist, create it
            try {

                FileWriter fw = new FileWriter(WINES_FILE_TXT);
                fw.close();

                brWine = new BufferedReader(new FileReader(WINES_FILE_TXT));
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

        // update forSale HashMap
        try {

            brSale = new BufferedReader(new FileReader(FORSALE_FILE_TXT));
            fillMapForSale(brSale);
            brSale.close();

        } catch (FileNotFoundException e) {

            // file doesn't exist, create it
            try {

                FileWriter fw = new FileWriter(FORSALE_FILE_TXT);
                fw.close();

                brSale = new BufferedReader(new FileReader(FORSALE_FILE_TXT));
                fillMapForSale(brSale); // TODO swap for new streams
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

    /**
     * For each line in balance.txt, create an Utilizador and add to listaUts
     * 
     * @param br BufferedReader for balance.txt
     */
    private static void fillListaUts(BufferedReader br) throws IOException, NoSuchAlgorithmException {
        byte[] hash = digestFile(BALANCE_FILE_TXT);

        String line;
        while ((line = br.readLine()) != null) {

            String[] lineSplit = line.split(";");
            listaUts.add(new Utilizador(lineSplit[0], Integer.parseInt(lineSplit[1])));
        }

        if (isCorrupted(BALANCE_FILE_TXT, hash)) {
            System.out.println("  balance.txt file corrupted!");
        }

        System.out.println("  listaUts updated");
    }

    /**
     * For each line in wines.txt, create a Wine and add to listaWines
     * 
     * @param br BufferedReader for wines.txt
     */
    private static void fillListaWines(BufferedReader br) throws IOException, NoSuchAlgorithmException {
        byte[] hash = digestFile(WINES_FILE_TXT);

        String line;
        while ((line = br.readLine()) != null) {

            String[] lineSplit = line.split(";");
            ArrayList<Integer> stars = new ArrayList<>();

            for (int i = 2; i < lineSplit.length; i++) {
                stars.add(Integer.parseInt(lineSplit[i]));
            }

            listaWines.add(new Wine(lineSplit[0], lineSplit[1], stars));
        }
        if (isCorrupted(WINES_FILE_TXT, hash)) {
            System.out.println("  wines.txt file corrupted!");
        }

        System.out.println("  listaWines updated");
    }

    /**
     * For each line in forSale.txt, create a Sale and add to hashmap
     * forSale<Utilizador,ArrayList<Sale>>
     * 
     * @param br BufferedReader for forSale.txt
     */
    private static void fillMapForSale(BufferedReader br) throws IOException, NoSuchAlgorithmException {
        byte[] hash = digestFile(WINES_FILE_TXT);

        String line;
        while ((line = br.readLine()) != null) {
            String[] splitLine = line.split(";");

            Utilizador utSale = null;
            for (Utilizador u : listaUts) {
                if (u.getUserID().equals(splitLine[0])) {
                    utSale = u;
                }
            }

            Wine wSale = null;
            for (Wine w : listaWines) {
                if (w.getName().equals(splitLine[1])) {
                    wSale = w;
                }
            }

            ArrayList<Sale> sales = forSale.get(utSale);

            if (sales == null) {
                sales = new ArrayList<>();
            }
            
            sales.add(new Sale(wSale, Integer.parseInt(splitLine[2]), Integer.parseInt(splitLine[3])));
            forSale.put(utSale, sales);
        }

        if (isCorrupted(FORSALE_FILE_TXT, hash)) {
            System.out.println("  forSale.txt file corrupted!");
        }

        System.out.println("  listaForSale updated");
    }

    private static boolean isCorrupted(String pathToFile, byte[] hashToCompare) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(Files.readAllBytes(Paths.get(pathToFile)));

        return Arrays.equals(hashToCompare, hash);
    }

    private static byte[] digestFile(String pathToFile) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(Files.readAllBytes(Paths.get(pathToFile)));
    }

    /**
     * Classe que representa todos os argumentos de uma venda (exceto seller)
     * <Wine,price,quantity>
     */
    protected static class Sale {
        private final Wine wine;
        private int value;
        private int quantity;

        //construtor
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

    /**
     * Classe para criar threads para vários clientes comunicarem com o servidor
     */
    static class clientHandlerThread extends Thread {

        private final SSLSocket sslSocket;
        private final Cipher cipher;
        private final SecretKey key;
        private static Utilizador ut = null;

        private static ObjectOutputStream outStream;
        private static ObjectInputStream inStream;

        private BufferedWriter bwAuthTxt = null;
        private FileOutputStream fosAuthKey = null;
        private FileInputStream fisAuthTxt = null;
        private FileOutputStream fosAuthCif = null;
        private CipherOutputStream cosAuth = null;
        private ObjectOutputStream oos = null;

        private BufferedReader brAuthTxt = null;
        private FileInputStream fisAuthKey = null;
        private FileOutputStream fosAuthTxt = null;
        private FileInputStream fisAuthCif = null;
        private CipherInputStream cisAuth = null;
        private ObjectInputStream oisKey = null;


        clientHandlerThread(SSLSocket sslSocket, Cipher cipher, SecretKey key) {
            this.sslSocket = sslSocket;
            this.cipher = cipher;
            this.key = key;
        }

        @Override
        public void run() {

            try {

                // streams
                outStream = new ObjectOutputStream(sslSocket.getOutputStream());
                inStream = new ObjectInputStream(sslSocket.getInputStream());

                String clientID = null;
                String passwd = null;
                boolean newUser = true;
                Random random = new Random();
                long nonce = random.nextLong() & 0xFFFFFFFFFFFFFFL;

                try {

                    clientID = (String) inStream.readObject();
                    passwd = (String) inStream.readObject();

                    if (listaUts.size() != 0) {

                        for (Utilizador u : listaUts) {
                            if (u.getUserID().equals(clientID)) {
                                ut = u;
                                newUser = false;
                            }
                        }
                    }

                    if (newUser) {
                        // enviar nonce com flag de ut desconhecido
                        outStream.writeObject(nonce + ":newUser");

                        // receber nonce, assinatura, certificado e chave publica do certificado
                        long nonceFromClient = (long) inStream.readObject();
                        Signature signatureFromClient = (Signature) inStream.readObject();
                        PublicKey publicKeyFromClient = (PublicKey) inStream.readObject();

                        // verificar se nonce é o enviado e verificar assinatura com a chave publica
                        // TODO como conseguir os bytes da assinatura para a verificação
                        signatureFromClient.initVerify(publicKeyFromClient);
                        byte[] signatureBytes = signatureFromClient.sign();
                        boolean verifiedSignature = signatureFromClient.verify(signatureBytes);

                        if ((nonceFromClient == nonce) && verifiedSignature) {
                            outStream.writeObject("Registo e autenticacao bem sucedida! \n");
                        } else {
                            outStream.writeObject("Registo e autenticacao nao foi bem sucedida... \n");
                        }

                        ut = new Utilizador(clientID, 200);
                        listaUts.add(ut);

                    } else {
                        outStream.writeObject(nonce);

                        // receber assinatura do cliente e verificar com a
                        // chave publica do users.txt desse cliente
                        byte[] signedNonce = (byte[]) inStream.readObject();

                        Signature verifier = Signature.getInstance("SHA256withRSA");
                        //verifier.initVerify(publicKey);
                        verifier.update(String.valueOf(nonce).getBytes());
                        boolean validSignature = verifier.verify(signedNonce);

                        if (!validSignature) {
                            outStream.writeObject("Autenticacao inválida! \n");
                        } else {
                            outStream.writeObject("Autenticacao bem sucedida! \n");
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                StringBuilder sb = new StringBuilder();

                try {

                    brAuthTxt = new BufferedReader(new FileReader(AUTHENTICATION_FILE_TXT));

                } catch (FileNotFoundException e) {

                    // file doesn't exist, create it
                    try {

                        FileWriter fw = new FileWriter(AUTHENTICATION_FILE_TXT);
                        fw.close();

                        brAuthTxt = new BufferedReader(new FileReader(AUTHENTICATION_FILE_TXT));

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                String line = null;

                while (((line = brAuthTxt.readLine()) != null)) {
                    sb.append(line + "\n");
                }
                brAuthTxt.close();

                bwAuthTxt = new BufferedWriter(new FileWriter(AUTHENTICATION_FILE_TXT));

                if (newUser) {
                    sb.append(clientID + ":" + passwd + "\n"); // insert new user credentials
                }
                bwAuthTxt.write(sb.toString());
                bwAuthTxt.close();

                fisAuthTxt = new FileInputStream(AUTHENTICATION_FILE_TXT);
                fosAuthCif = new FileOutputStream(AUTHENTICATION_FILE_CIF);
                cosAuth = new CipherOutputStream(fosAuthCif, cipher);

                byte[] buffer = new byte[16];
                int bytesRead = fisAuthTxt.read(buffer);
                while (bytesRead != -1) {
                    cosAuth.write(buffer, 0, bytesRead);
                    bytesRead = fisAuthTxt.read(buffer);
                }

                byte[] keyEncoded = key.getEncoded();

                fosAuthKey = new FileOutputStream(AUTHENTICATION_FILE_KEY);
                oos = new ObjectOutputStream(fosAuthKey);
                oos.writeObject(keyEncoded);

                oos.close();
                fosAuthKey.close();

                // decifrar
                fisAuthKey = new FileInputStream(AUTHENTICATION_FILE_KEY);
                oisKey = new ObjectInputStream(fisAuthKey);
                byte[] keyEncoded2 = (byte[]) oisKey.readObject();

                oisKey.close();

                SecretKeySpec keySpec2 = new SecretKeySpec(keyEncoded2, "AES");
                Cipher d = Cipher.getInstance("AES");
                d.init(Cipher.DECRYPT_MODE, keySpec2);

                fosAuthTxt = new FileOutputStream(AUTHENTICATION_FILE_TXT);
                fisAuthCif = new FileInputStream(AUTHENTICATION_FILE_CIF);
                cisAuth = new CipherInputStream(fisAuthCif, d);

                byte[] buffer1 = new byte[16];
                bytesRead = cisAuth.read(buffer1);
                while (bytesRead != -1) {
                    fosAuthTxt.write(buffer1, 0, bytesRead);
                    bytesRead = cisAuth.read(buffer1);
                }

                byte[] hash = digestFile(AUTHENTICATION_FILE_TXT);

                brAuthTxt = new BufferedReader(new FileReader(AUTHENTICATION_FILE_TXT));

                boolean found = false;
                String lineForSplit = null; // line with clientID user credentials

                while (((line = brAuthTxt.readLine()) != null) && !found) {
                    if (line.contains(clientID)) {
                        lineForSplit = line;
                        found = true;
                    }
                }
                brAuthTxt.close();

                if (isCorrupted(AUTHENTICATION_FILE_TXT, hash)) { //TODO add readObject()
                    outStream.writeObject("Tudo ok com o ficheiro authentication.txt" + "\n");
                } else {
                    outStream.writeObject("Ficheiro authentication.txt corrupto! \n");
                }

                // authentication
                String[] splitLine = lineForSplit.split(":");
                if (splitLine[1].equals(passwd)) {

                    outStream.writeObject("Acesso concedido! \n");

                    updateBalance();

                    String command;
                    boolean exit = false;

                    while (!exit) {

                        outStream.writeObject(menu());
                        command = (String) inStream.readObject();

                        if (command.equals("exit")) {
                            outStream.writeObject("Powering off... \n");

                            // close streams and socket
                            if (outStream != null) {
                                outStream.close();
                            }
                            if (inStream != null) {
                                inStream.close();
                                sslSocket.close();
                                exit = true;
                            }

                        } else {
                            process(command, outStream);
                        }
                    }

                } else {
                    outStream.writeObject("Username ou password incorretos! A fechar app... \n");
                    System.exit(-2);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * For each user in listaUts, updates its balance in balance.txt file
         */
        private static void updateBalance() {
            try {

                bwBal = new BufferedWriter(new FileWriter(BALANCE_FILE_TXT));

                if (!listaUts.isEmpty()) {
                    for (Utilizador u : listaUts) {
                        bwBal.write(u.getUserID() + ";" + u.getBalance() + "\n");
                    }
                }

                bwBal.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Updates wines.txt file from listaWines
         */
        private static void updateWines() {
            try {

                bwWine = new BufferedWriter(new FileWriter(WINES_FILE_TXT));

                for (Wine wine : listaWines) {

                    StringBuilder sb = new StringBuilder();
                    for (Integer j : wine.getStars()) {
                        sb.append(j);
                        sb.append(";");
                    }
                    String stars = sb.toString();

                    // nome;imagem;star1;star2;star3...
                    bwWine.write(wine.getName() + ";" + wine.getImage() + ";" + stars + "\n");
                }

                bwWine.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Updates forSale.txt file from forSale HashMap
         */
        private static void updateForSale() {
            try {

                bwSale = new BufferedWriter(new FileWriter(FORSALE_FILE_TXT));

                if (forSale.size() != 0) {
                    for (Utilizador u : forSale.keySet()) {

                        ArrayList<Sale> sales = forSale.get(u);

                        if (!sales.isEmpty()) {
                            for (Sale sale : sales) {
                                if (sale.getQuantity() > 0) {
                                    // seller;wine;price;quantity
                                    bwSale.write(u.getUserID() + ";" + (sale.getWine()).getName() + ";"
                                            + sale.getValue() + ";" + sale.getQuantity() + "\n");
                                }
                            }
                        }
                    }
                }

                bwSale.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Updates the chat.txt file, appending the new interaction between users.
         * 
         * @param receiver
         * @param msg
         */
        private static void updateChat(String receiver, String msg) {
            //TODO garantir confidencialidade entre chat de clientes
            try {
                byte[] hash = digestFile(CHAT_FILE_TXT);

                brChat = new BufferedReader(new FileReader(CHAT_FILE_TXT));
                String line;
                StringBuilder sb = new StringBuilder();

                while ((line = brChat.readLine()) != null) {
                    sb.append(line + "\n");
                }

                if (isCorrupted(CHAT_FILE_TXT, hash)) { // TODO add readObject() in client
                    outStream.writeObject("Tudo ok com o ficheiro chat.txt" + "\n");
                } else {
                    outStream.writeObject("Ficheiro chat.txt corrupto! \n");
                }

                bwChat = new BufferedWriter(new FileWriter(CHAT_FILE_TXT));

                // sender;receiver;msg
                bwChat.write(sb.toString());
                bwChat.write(ut.getUserID() + ";" + receiver + ";" + msg + "\n");
                bwChat.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * App functionalities
         *
         * @param command   is the command sent by client
         * @param outStream is the stream to send data to client
         * @throws IOException
         */
        private void process(String command, ObjectOutputStream outStream)
                throws IOException {
            String[] splitCommand = command.split(" ");
            boolean isValid = false;
            boolean receiveFile = false;

            // add
            if (splitCommand[0].equals("add") || splitCommand[0].equals("a")) {
                isValid = true;

                if (splitCommand.length != 3) {
                    outStream.writeObject("Comando invalido! A operacao add necessita de 2 argumentos <name> <image>.");
                } else {

                    if (TintolmarketServer.listaWines.isEmpty()) {

                        receiveFile = true;
                        TintolmarketServer.listaWines.add(new Wine(splitCommand[1], splitCommand[2], new ArrayList<>()));
                        outStream.writeObject("Vinho adicionado com sucesso! \n");

                    } else {
                        boolean contains = false;
                        for (Wine w : TintolmarketServer.listaWines) {

                            if (w.getName().equals(splitCommand[1])) {
                                contains = true;
                                outStream.writeObject("O vinho que deseja adicionar ja existe! \n");
                            }
                        }

                        if (!contains) {

                            receiveFile = true;
                            TintolmarketServer.listaWines.add(new Wine(splitCommand[1], splitCommand[2], new ArrayList<>()));
                            outStream.writeObject("Vinho adicionado com sucesso! \n");
                        }
                    }
                }
                
                if(receiveFile) {
                    receiveFile(splitCommand[2]); //receive image
                }

                updateWines();
            }

            // sell
            if (splitCommand[0].equals("sell") || splitCommand[0].equals("s")) {
                isValid = true;
                boolean contains = false;
                Wine wine = null;

                if (splitCommand.length != 4) {
                    outStream.writeObject(
                            "Comando invalido! A operacao sell necessita de 3 argumentos <name> <image> <quantity>. \n");
                } else {

                    if (TintolmarketServer.listaWines.isEmpty()) {
                        outStream.writeObject("O vinho que deseja vender nao existe! \n");

                    } else {
                        for (Wine w : TintolmarketServer.listaWines) {
                            if (w.getName().equals(splitCommand[1]) && !contains) {
                                wine = w;
                                contains = true;
                            }
                        }

                        if (!contains) {
                            outStream.writeObject("O vinho que deseja vender nao existe! \n");

                        } else if (TintolmarketServer.forSale.size() == 0) {
                            ArrayList<Sale> sales = new ArrayList<>();
                            sales.add(new Sale(wine, Integer.parseInt(splitCommand[2]),
                                    Integer.parseInt(splitCommand[3])));
                            TintolmarketServer.forSale.put(ut, sales);
                            outStream.writeObject("Vinho colocado a venda! \n");

                        } else {
                            boolean updated = false;
                            for (HashMap.Entry<Utilizador,ArrayList<Sale>> entry : TintolmarketServer.forSale.entrySet()) {
                                Utilizador u = entry.getKey();

                                if ((u.getUserID()).equals((ut.getUserID()))) {

                                    ArrayList<Sale> sales = entry.getValue();
                                    for (Sale sale : sales) {
                                        if (sale.getWine().getName().equals(splitCommand[1])) { // caso o vinho ja
                                                                                                // esteja a venda(mesmo
                                                                                                // ut)

                                            sale.updateValue(Integer.parseInt(splitCommand[2])); // muda o preço
                                            sale.updateQuantity(Integer.parseInt(splitCommand[3])); // incrementa a
                                                                                                    // quantidade

                                            outStream.writeObject(
                                                    "Vinho ja se encontrava a venda. O preco foi alterado para "
                                                            + Integer.parseInt(splitCommand[2]) + "\n");
                                            updated = true;
                                        }
                                    }
                                }
                            }

                            //if it was not a sale to update
                            if (!updated) {

                                ArrayList<Sale> sales = TintolmarketServer.forSale.get(ut);

                                if (sales == null) {
                                    sales = new ArrayList<>();
                                }

                                sales.add(new Sale(wine, Integer.parseInt(splitCommand[2]),
                                        Integer.parseInt(splitCommand[3])));
                                TintolmarketServer.forSale.put(ut, sales);
                                outStream.writeObject("Vinho colocado a venda! \n");
                            }
                        }
                    }
                }

                updateForSale();
            }

            // view
            if (splitCommand[0].equals("view") || splitCommand[0].equals("v")) {
                isValid = true;
                boolean contains = false;
                Wine w = null;

                if (splitCommand.length != 2) {
                    outStream.writeObject("Comando invalido! A operacao view necessita de 1 argumento <wine>. \n");

                } else {
                    for (Wine wine : TintolmarketServer.listaWines) {
                        if (wine.getName().equals(splitCommand[1]) && !contains) {
                            contains = true;
                            w = wine;
                        }
                    }

                    if (!contains) {

                        outStream.writeObject("O vinho que deseja ver nao existe! \n");
                    } else {    

                        StringBuilder sb = new StringBuilder();
                        sb.append("Nome: " + w.getName() + "\n");
                        sb.append("Imagem: " + w.getImage() + "\n");
                        sb.append("Classificacao media: " + w.getAvgRate() + "\n");

                        for (HashMap.Entry<Utilizador,ArrayList<Sale>> entry : TintolmarketServer.forSale.entrySet()) {
                            Utilizador u = entry.getKey();
                            ArrayList<Sale> sales = entry.getValue();
                            
                            for (Sale sale : sales) {
                                if (w.getName().equals(sale.getWine().getName()) && sale.getQuantity() > 0) {

                                    sb.append("-------------------------------------- \n");
                                    sb.append("Vendedor: " + u.getUserID() + "\n");
                                    sb.append("Preco unitario: " + sale.getValue() + "\n");
                                    sb.append("Quantidade: " + sale.getQuantity() + "\n");
                                }
                            }
                        }
                        sendFile(w.getImage());
                        outStream.writeObject(sb.toString());
                    }
                }
            }

            // buy
            if (splitCommand[0].equals("buy") || splitCommand[0].equals("b")) {
                isValid = true;
                boolean contains = false;

                if (splitCommand.length != 4) {
                    outStream.writeObject(
                            "Comando invalido! A operacao buy necessita de 3 argumentos <wine> <seller> <quantity>. \n");

                } else if (splitCommand[2].equals(ut.getUserID())) {
                    outStream.writeObject("Comando invalido! <seller> nao pode ser o proprio! \n");

                } else {
                    for (Wine wine : TintolmarketServer.listaWines) {
                        if (wine.getName().equals(splitCommand[1]) && !contains) {
                            contains = true;
                        }
                    }

                    if (!contains) {
                        outStream.writeObject("O vinho que deseja comprar nao existe! \n");
                    } else {
                        boolean notFound = true;
                        boolean sold = false;// boolean to check if sale was already made

                        for (HashMap.Entry<Utilizador,ArrayList<Sale>> entry : TintolmarketServer.forSale.entrySet()) {
                            Utilizador utSeller = entry.getKey();
                            
                            if (utSeller.getUserID().equals(splitCommand[2]) && !sold) {// find seller
                                
                                ArrayList<Sale> sales = entry.getValue();
                                Sale saleToRemove = null;
                                for (Sale sale : sales) {

                                    if (sale.getWine().getName().equals(splitCommand[1])) {
                                        notFound = false;

                                        if (sale.getQuantity() < Integer.parseInt(splitCommand[3])) {
                                            outStream.writeObject(
                                                    "Nao existe quantidade suficiente deste vinho para venda! \n");

                                        } else if (ut.getBalance() < (sale.getValue()
                                                * (Integer.parseInt(splitCommand[3])))) {
                                            outStream.writeObject("Saldo insuficiente! \n");

                                        } else {
                                            sale.updateQuantity(-(Integer.parseInt(splitCommand[3])));
                                            ut.updateWallet(-(sale.getValue() * (Integer.parseInt(splitCommand[3])))); // comprador
                                                                                                                       // desconta
                                                                                                                       // o
                                                                                                                       // dinheiro
                                            utSeller.updateWallet(
                                                    sale.getValue() * (Integer.parseInt(splitCommand[3]))); // seller
                                                                                                            // recebe o
                                                                                                            // dinheiro
                                            sold = true;

                                            outStream.writeObject("Compra realizada com sucesso! \n");

                                            // remover venda com quantidade 0
                                            if (sale.getQuantity() == 0) {
                                                saleToRemove = sale;
                                            }
                                        }
                                    }
                                }
                                sales.remove(saleToRemove);
                            }
                        }

                        if (notFound) {
                            outStream.writeObject("O vinho indicado nao se encontra a venda! \n");
                        }
                    }
                }

                updateForSale();
                updateBalance();
            }

            // wallet
            if (splitCommand[0].equals("wallet") || splitCommand[0].equals("w")) {
                isValid = true;
                outStream.writeObject(ut.getBalance());
            }

            // classify
            if (splitCommand[0].equals("classify") || splitCommand[0].equals("c")) {
                isValid = true;
                boolean contains = false;
                if (splitCommand.length != 3) {
                    outStream.writeObject(
                            "Comando invalido! A operacao classify necessita de 2 argumentos <wine> <stars>. \n");

                } else {
                    for (Wine wine : TintolmarketServer.listaWines) {
                        if (wine.getName().equals(splitCommand[1]) && !contains) {
                            wine.classify(Integer.parseInt(splitCommand[2]));
                            contains = true;
                            outStream.writeObject("Vinho classificado! \n");
                        }
                    }

                    if (!contains) {
                        outStream.writeObject("O vinho que deseja classificar nao existe! \n");
                    }
                }

                updateWines();
            }

            // talk
            if (splitCommand[0].equals("talk") || splitCommand[0].equals("t")) {
                isValid = true;
                boolean contains = false;
                StringBuilder message = new StringBuilder();

                if (splitCommand[1].equals(ut.getUserID())) {
                    outStream.writeObject("Comando invalido! O <receiver> nao pode ser o proprio! \n");

                } else {
                    for (Utilizador u : TintolmarketServer.listaUts) {
                        if (u.getUserID().equals(splitCommand[1]) && !contains) {
                            contains = true;
                        }
                    }

                    if (!contains) {
                        outStream.writeObject("O utilizador nao existe! \n");
                    } else {

                        try {

                            for (int i = 2; i < splitCommand.length; i++) {
                                message.append(splitCommand[i] + " ");
                            }

                            updateChat(splitCommand[1], message.toString());
                            outStream.writeObject("Mensagem enviada com sucesso! \n");

                        } catch (Exception e) {
                            outStream.writeObject("Erro no envio da mensagem! \n");
                            e.printStackTrace();
                        }
                    }
                }
            }

            // read
            if (splitCommand[0].equals("read") || splitCommand[0].equals("r")) {
                isValid = true;

                if (splitCommand.length != 1) {
                    outStream.writeObject("Comando invalido! A operacao read nao necessita de argumentos. \n");

                } else {
                    try {

                        brChat = new BufferedReader(new FileReader(CHAT_FILE_TXT));
                        String line;
                        boolean empty = true;
                        StringBuilder serverChat = new StringBuilder();
                        StringBuilder message = new StringBuilder();

                        while ((line = brChat.readLine()) != null) {

                            if (!line.equals("")) {
                                String[] splitLine = line.split(";");
                                if (splitLine[1].equals(ut.getUserID())) {
                                    // remove msg from server
                                    message.append("mensagem de " + splitLine[0] + " : " + splitLine[2] + "\n");
                                    empty = false;
                                } else {
                                    serverChat.append(line + "\n");
                                }
                            }
                        }

                        if (empty) {
                            outStream.writeObject("Nao tem mensagens por ler! \n");

                        } else {
                            outStream.writeObject(message);
                            bwChat = new BufferedWriter(new FileWriter(CHAT_FILE_TXT));
                            bwChat.write(serverChat.toString());
                            bwChat.close();
                        }

                        brChat.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if (!isValid) {
                outStream.writeObject(("Comando Inválido! Indique um dos comandos apresentados acima. \n"));
            }
        }

        /**
         * Receives a file from the inStream and stores its content (bytes) in a file
         * with {@code fileName} in serverFiles directory
         * 
         * @param fileName
         * @throws IOException
         */
        private void receiveFile(String fileName) throws IOException {
            int fileSize;
            int bytesRead;
            byte[] buffer = new byte[1024];

            File f = new File(SERVER_DIR + fileName);
            FileOutputStream fileOutputStream = new FileOutputStream(f);
            OutputStream outputFile = new BufferedOutputStream(fileOutputStream);

            try {
                fileSize = (int) inStream.readObject();
                int totalsize = fileSize;

                //receive file
                while (totalsize > 0 ) {

                    if(totalsize >= 1024) {
                        bytesRead = inStream.read(buffer, 0, 1024);
                    } else {
                        bytesRead = inStream.read(buffer, 0, totalsize);
                    }

                    outputFile.write(buffer,0,bytesRead);
					totalsize -= bytesRead;        
                    outputFile.flush();
                }

                outputFile.close();
                fileOutputStream.close();
                
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        /**
         * Sends a file from the serverFiles directory to the outStream
         * 
         * @param fileName
         * @throws IOException
         */
        private void sendFile(String fileName) throws IOException {

            File fileToSend = new File(SERVER_DIR + fileName);
            FileInputStream fileInputStream = new FileInputStream(fileToSend);
            InputStream inputFile = new BufferedInputStream(fileInputStream);
        
            byte[] buffer = new byte[1024];
            outStream.writeObject((int) fileToSend.length());
            int bytesRead;
            
            // enviar ficheiro
            while ((bytesRead = inputFile.read(buffer)) != -1) {

                outStream.write(buffer, 0, bytesRead); //send file
                outStream.flush();
            }
            
            inputFile.close();
        }

        /**
         * prints a menu with some commands
         * 
         * @return a String with the menu
         */
        private String menu() {
            return """
                    -------------------------------------------
                     -> add <wine> <image>
                     -> sell <wine> <value> <quantity>
                     -> view <wine>
                     -> buy <wine> <seller> <quantity>
                     -> wallet
                     -> classify <wine> <star>
                     -> talk <user> <message>
                     -> read
                     -> exit
                     Selecione um comando:\s""";
        }
    }
}