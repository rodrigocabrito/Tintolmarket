import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;


/**
 * @authors:
 *      Rodrigo Cabrito 54455
 *      João Costa 54482
 *      João Fraga 44837
 */

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
    static BufferedReader brChat = null; //TODO remove
    static BufferedWriter bwChat = null; //TODO remove

    // file paths
    private static final String AUTHENTICATION_FILE_TXT = "./data_bases/users.txt";
    private static final String AUTHENTICATION_FILE_CIF = "./data_bases/users.cif";
    private static final String AUTHENTICATION_FILE_KEY = "./data_bases/users.key";
    private static final String BALANCE_FILE_TXT = "./data_bases/balance.txt";
    private static final String WINES_FILE_TXT = "./data_bases/wines.txt";
    private static final String FORSALE_FILE_TXT = "./data_bases/forSale.txt";
    private static final String CHAT_FILE_TXT = "./data_bases/chat.txt"; //TODO remove

    // directories
    private static final String SERVER_FILES_DIR = "./serverFiles/";
    private static final String KEYSTORE_DIR = "./keystores/";
    private static final String CHAT_DIR = "./chat/";

    // server information
    private static PublicKey serverPublicKey = null;
    private static PrivateKey serverPrivateKey = null;
    private static Signature serverSignature = null;

    // server memory
    private static PublicKey[] clientPublicKeys = null;
    private static final Blockchain blockchain = new Blockchain();
    private static final ArrayList<Utilizador> listaUts = new ArrayList<>();
    private static final ArrayList<Wine> listaWines = new ArrayList<>();
    private static final HashMap<Utilizador, ArrayList<Sale>> forSale = new HashMap<>();

    public static void main(String[] args) throws Exception {

        int port = Integer.parseInt(args[0]);
        String password = args[1];
        String keyStorePath = args[2];
        String keyStorePassword = args[3];

        String certificateAlias = "";
        String keyAlias = "";
        String keyPassword = "";

        String trustStorePath = KEYSTORE_DIR + "mytruststore.jks"; //TODO

        // get keystore from args
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(new FileInputStream(keyStorePath), keyStorePassword.toCharArray());

        // get a trustStore containing the client's trusted certificates (if needed)
        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(new FileInputStream("client.truststore"), null); //TODO get truststore path

        // get self certificate and keys
        Certificate cert = (Certificate) keyStore.getCertificate(certificateAlias);
        serverPublicKey = cert.getPublicKey();
        serverPrivateKey = (PrivateKey) keyStore.getKey(keyAlias, keyPassword.toCharArray());

        serverSignature = Signature.getInstance("MD5withRSA");
        serverSignature.initSign(serverPrivateKey);

        // TODO verificar salt e iterationCount param da funcao PBEKeySpec (20)
        // Generate the key based on the password passeed by args[1]
        byte[] salt = { (byte) 0xc9, (byte) 0x36, (byte) 0x78, (byte) 0x99, (byte) 0x52, (byte) 0x3e, (byte) 0xea, (byte) 0xf2 };
        PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 20); // passw, salt, iterations
        SecretKeyFactory kf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128"); //TODO check isntance com enunciado
        SecretKey key = kf.generateSecret(keySpec);

        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        updateServerMemory();

        verifyIntegrityBlockchain();

        try {
            BufferedReader br = new BufferedReader(new FileReader(CHAT_FILE_TXT));
            br.close();

        } catch (FileNotFoundException e) {
            // if it doesn't exist, create chat.txt
            FileWriter f = new FileWriter(CHAT_FILE_TXT);
            f.close();
        }

        System.setProperty("javax.net.ssl.keyStore", keyStorePath);
        System.setProperty("javax.net.ssl.keyStorePassword", password);
        System.setProperty("javax.net.ssl.trustStore", trustStorePath);

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
                clientSSLThread = new clientHandlerThread(clientSocketSSL, cipher, key);
                clientSSLThread.start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //serverSocket.close();
    }

    private static void verifyIntegrityBlockchain() throws Exception {
        blockchain.loadBlocks();

        if (blockchain.isChainValid()) {
            System.out.println(" Blockchain valida!");
        } else {
            System.out.println(" Blockchain corrompida!");
            System.exit(-1); // TODO exit
        }

        for (Block block : blockchain.getBlocks()) {
            if (block.isBlockFull()) {
                String data = block.getHash() + block.getId() + block.getNrTransacoes();
                for (Transacao transacao : block.getTransacoes()) {
                    data.concat(transacao.toString());
                }
                byte[] dataBytes = data.getBytes();
                byte[] signature = sign(dataBytes, serverPrivateKey);
                boolean validSignature = verify(dataBytes, signature, serverPublicKey);

                if (validSignature) {
                    System.out.println("Assinatura do bloco com id " + block.getId() + " eh valido");
                } else {
                    System.out.println("Assinatura do bloco com id " + block.getId() + " eh invalido");
                }
            }
        }
    }

    public static byte[] sign(byte[] data, PrivateKey privateKey) throws Exception {
        // Create the signature object
        Signature signature = Signature.getInstance("MD5withRSA");
        signature.initSign(privateKey);
        signature.update(data);

        // Generate the digital signature
        return signature.sign();
    }

    public static boolean verify(byte[] data, byte[] signature, PublicKey publicKey) throws Exception {
        // Create the signature object
        Signature sig = Signature.getInstance("MD5withRSA");
        sig.initVerify(publicKey);
        sig.update(data);

        // Verify the digital signature
        return sig.verify(signature);
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
            fillListaWines(brWine);
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
                fillMapForSale(brSale);
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
        private static PublicKey clientPublicKey = null;
        private static PrivateKey clientPrivateKey= null;
        private static Signature clientSignature = null;

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



                SSLSession sslSession = sslSocket.getSession();

                if (sslSession != null) {
                    javax.security.cert.Certificate[] chain = sslSession.getPeerCertificateChain();
                    if (chain != null) {
                        // Retrieve and verify the end-entity certificate from the chain
                        javax.security.cert.Certificate peerCertificate = chain[0];
                        peerCertificate.verify(peerCertificate.getPublicKey());
                    }
                }

                // streams
                outStream = new ObjectOutputStream(sslSocket.getOutputStream());
                inStream = new ObjectInputStream(sslSocket.getInputStream());

                String userID = null;
                Random random = new Random();
                long nonce = random.nextLong() & 0xFFFFFFFFFFFFFFL;
                boolean newUser = true;

                try {

                    userID = (String) inStream.readObject();

                    if (listaUts.size() != 0) {

                        for (Utilizador u : listaUts) {
                            if (u.getUserID().equals(userID)) {
                                ut = u;
                                newUser = false;
                            }
                        }
                    }

                    // authenticate user
                    if (newUser) {
                        // enviar nonce com flag de ut desconhecido
                        outStream.writeObject(nonce + ":newUser");

                        // receber nonce, assinatura e chave publica do certificado
                        long nonceFromClient = (long) inStream.readObject();
                        clientSignature = (Signature) inStream.readObject();
                        clientPublicKey = (PublicKey) inStream.readObject();

                        // verificar se nonce é o enviado e verificar assinatura com a chave publica
                        // TODO como conseguir os bytes da assinatura para a verificação
                        clientSignature.initVerify(clientPublicKey);
                        byte[] signatureBytes = clientSignature.sign();
                        boolean verifiedSignature = clientSignature.verify(signatureBytes);

                        if ((nonceFromClient == nonce) && verifiedSignature) {
                            outStream.writeObject("Registo e autenticacao bem sucedida! \n");
                        } else {
                            outStream.writeObject("Registo e autenticacao nao foi bem sucedida... \n");
                            System.exit(-1);
                        }

                        ut = new Utilizador(userID, 200);
                        listaUts.add(ut);

                    } else {
                        outStream.writeObject(nonce);

                        brAuthTxt = new BufferedReader(new FileReader(AUTHENTICATION_FILE_TXT));
                        String line;
                        String keyStorePath = "";
                        boolean done = false;

                        while (((line = brAuthTxt.readLine()) != null) && !done) {
                            if (line.contains(userID)) {
                                String[] splitLine = line.split(":");
                                keyStorePath = splitLine[1];
                                done = true;
                            }
                        }
                        brAuthTxt.close();

                        // receber assinatura do cliente e verificar com a
                        // chave publica do users.txt desse cliente
                        byte[] signedNonce = (byte[]) inStream.readObject();

                        KeyStore keystore = KeyStore.getInstance("JKS");
                        keystore.load(new FileInputStream(keyStorePath), null);

                        KeyStore.Entry entry = keystore.getEntry(userID + "kp", null); //TODO verificar key alias
                        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) entry;
                        clientPrivateKey = privateKeyEntry.getPrivateKey();

                        Certificate[] chain = (Certificate[]) ((KeyStore.PrivateKeyEntry) entry).getCertificateChain();

                        // Get the public key from the certificate from the keystore on users.txt
                        X509Certificate cert = (X509Certificate) chain[0];
                        PublicKey publicKey = cert.getPublicKey();

                        // verificar a assinatura do cliente com a chave publica
                        Signature verifier = Signature.getInstance("SHA256withRSA");
                        verifier.initVerify(publicKey);
                        verifier.update(String.valueOf(nonce).getBytes());
                        boolean validSignature = verifier.verify(signedNonce);

                        if (validSignature) {
                            outStream.writeObject("Autenticacao bem sucedida! \n");
                        } else {
                            outStream.writeObject("Autenticacao inválida! \n");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                boolean isNewFile = false;
                // create users.txt file in case it doesn't exist
                try {
                    brAuthTxt = new BufferedReader(new FileReader(AUTHENTICATION_FILE_TXT));
                } catch (FileNotFoundException e) {
                    // file doesn't exist, create it
                    try {
                        isNewFile = true;
                        FileWriter fw = new FileWriter(AUTHENTICATION_FILE_TXT);
                        fw.close();
                        brAuthTxt = new BufferedReader(new FileReader(AUTHENTICATION_FILE_TXT));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                // only decifer users.cif file if the file contains data
                int bytesRead = 0;
                if (!isNewFile) {

                    // decifer from file users.cif to file users.txt
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

                    fisAuthKey.close();
                    fosAuthTxt.close();
                    fisAuthCif.close();
                    cisAuth.close();
                }

                StringBuilder sb = new StringBuilder();
                String line = null;

                // read from users.txt
                while (((line = brAuthTxt.readLine()) != null)) {
                    sb.append(line + "\n");
                }
                brAuthTxt.close();

                bwAuthTxt = new BufferedWriter(new FileWriter(AUTHENTICATION_FILE_TXT));

                if (newUser) {
                    sb.append(userID + ":" + "ks" + userID + ".jks" + "\n"); // insert new user credentials
                }
                bwAuthTxt.write(sb.toString());
                bwAuthTxt.close();

                // read from users.txt and write to users.cif
                fisAuthTxt = new FileInputStream(AUTHENTICATION_FILE_TXT);
                fosAuthCif = new FileOutputStream(AUTHENTICATION_FILE_CIF);
                cosAuth = new CipherOutputStream(fosAuthCif, cipher);

                byte[] buffer = new byte[16];
                bytesRead = fisAuthTxt.read(buffer);
                while (bytesRead != -1) {
                    cosAuth.write(buffer, 0, bytesRead);
                    bytesRead = fisAuthTxt.read(buffer);
                }

                fisAuthCif.close();
                fosAuthCif.close();
                cosAuth.close();

                // write the key to the users.cif file on users.key file
                byte[] keyEncoded = key.getEncoded();

                fosAuthKey = new FileOutputStream(AUTHENTICATION_FILE_KEY);
                oos = new ObjectOutputStream(fosAuthKey);
                oos.writeObject(keyEncoded);
                oos.close();
                fosAuthKey.close();

                // updates the balance of every user
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
         * @param cipher
         * @param receiverId
         */
        private static void updateChat(String receiverId, byte[] encryptedMessage, Cipher cipher) {
            //TODO garantir confidencialidade entre chat de clientes
            try {
                byte[] hash = digestFile(CHAT_DIR + "user" + receiverId + ".cif");

                // Open the existing CIF file for reading
                File originalFile = new File(CHAT_DIR + "user" + receiverId + ".cif");
                FileInputStream fis = new FileInputStream(originalFile);

                // Create a temporary file for writing the modified CIF data
                File tempFile = File.createTempFile("modified_cif_file", ".cif");
                FileOutputStream fos = new FileOutputStream(tempFile);
                CipherOutputStream cos = new CipherOutputStream(fos, cipher);

                // Read the existing data from the input stream and write it to the CipherOutputStream
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    cos.write(buffer, 0, bytesRead);
                }

                if (isCorrupted(CHAT_DIR + "user" + receiverId + ".cif", hash)) { // TODO add readObject() in client
                    outStream.writeObject("Tudo ok com o ficheiro chat.txt" + "\n");
                } else {
                    outStream.writeObject("Ficheiro chat.txt corrupto! \n");
                }

                // Write the new data to the CipherOutputStream
                cos.write(encryptedMessage);

                // Close the streams
                fis.close();
                cos.close();

                // Replace the original file with the modified file
                if (originalFile.delete()) {
                    Files.move(tempFile.toPath(), originalFile.toPath());
                } else {
                    throw new IOException("Failed to delete the original .cif file");
                }
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
                throws Exception {
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

                            Transacao tSell = new Transacao(wine.getName(), Integer.parseInt(splitCommand[3]),
                                    Integer.parseInt(splitCommand[2]), ut.getUserID(), TransacaoType.SELL);

                            // verificar assinatura pelo cliente
                            clientSignature.update(tSell.toString().getBytes());
                            byte[] signatureBytes = clientSignature.sign();
                            boolean verifiedSignature = clientSignature.verify(signatureBytes);

                            if (verifiedSignature) {
                                blockchain.addTransacao(tSell, serverSignature);
                                outStream.writeObject("Transacao processada com sucesso! \n");
                            } else {
                                outStream.writeObject("Assinatura inválida! Transacao nao processada! \n");
                            }

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

                                Transacao tSell = new Transacao(wine.getName(), Integer.parseInt(splitCommand[3]),
                                        Integer.parseInt(splitCommand[2]), ut.getUserID(), TransacaoType.SELL);

                                // verificar assinatura pelo cliente
                                clientSignature.update(tSell.toString().getBytes());
                                byte[] signatureBytes = clientSignature.sign();
                                boolean verifiedSignature = clientSignature.verify(signatureBytes);

                                if (verifiedSignature) {
                                    blockchain.addTransacao(tSell, serverSignature);
                                    outStream.writeObject("Transacao processada com sucesso! \n");
                                } else {
                                    outStream.writeObject("Assinatura inválida! Transacao nao processada! \n");
                                }
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

                                            Transacao tBuy = new Transacao(sale.getWine().getName(), Integer.parseInt(splitCommand[3]),
                                                    sale.getValue(), ut.getUserID(), TransacaoType.BUY);

                                            // verificar assinatura pelo cliente
                                            clientSignature.update(tBuy.toString().getBytes());
                                            byte[] signatureBytes = clientSignature.sign();
                                            boolean verifiedSignature = clientSignature.verify(signatureBytes);

                                            if (verifiedSignature) {
                                                blockchain.addTransacao(tBuy, serverSignature);
                                                outStream.writeObject("Transacao processada com sucesso! \n");
                                            } else {
                                                outStream.writeObject("Assinatura inválida! Transacao nao processada! \n");
                                            }

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

                outStream.writeObject("> ");
                byte[] encryptedMessage = (byte[]) inStream.readObject();

                if (splitCommand[1].equals(ut.getUserID())) {
                    outStream.writeObject("Comando invalido! O <receiver> nao pode ser o proprio! \n");

                } else {
                    Utilizador receiver = null;
                    for (Utilizador u : TintolmarketServer.listaUts) {
                        if (u.getUserID().equals(splitCommand[1]) && !contains) {
                            contains = true;
                            receiver = u;
                        }
                    }

                    if (!contains) {
                        outStream.writeObject("O utilizador nao existe! \n");
                    } else {
                        try {
                            Cipher cipher = Cipher.getInstance("RSA");
                            cipher.init(Cipher.ENCRYPT_MODE, clientPublicKey); //TODO trocar para o receiver public key

                            updateChat(receiver.getUserID(), encryptedMessage, cipher);

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
                        File chatDIr = new File("chat");
                        File[] chatFiles = chatDIr.listFiles((CHAT_DIR, name) -> name.endsWith(".cif"));

                        Cipher cipher = Cipher.getInstance("RSA");
                        cipher.init(Cipher.DECRYPT_MODE, clientPrivateKey);

                        boolean empty = true;
                        String messages = "";

                        if (chatFiles != null) {
                            // Read each .cif file in chat directory
                            for (File chatFile : chatFiles) {
                                if (chatFile.getName().contains(ut.getUserID())) {
                                    empty = false;

                                    CipherInputStream cis = new CipherInputStream(new FileInputStream(chatFile), cipher);
                                    StringBuilder sb = new StringBuilder();
                                    BufferedReader reader = new BufferedReader(new InputStreamReader(cis, StandardCharsets.UTF_8));
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        sb.append(line);
                                        sb.append("\n");
                                    }

                                    messages = sb.toString();

                                    reader.close();
                                    cis.close();

                                    // delete the .cif file after reading it
                                    chatFile.delete();
                                }
                            }

                            if (empty) {
                                outStream.writeObject("Nao tem mensagens por ler! \n");
                            } else {
                                outStream.writeObject(messages + "\n"); // TODO create message with all messages
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            // list
            if (splitCommand[0].equals("list") || splitCommand[0].equals("l")) {
                isValid = true;

                if (splitCommand.length != 1) {
                    outStream.writeObject("Comando invalido! A operacao list nao necessita de argumentos. \n");

                } else {
                    StringBuilder sb = new StringBuilder();
                    for (Block block : blockchain.getBlocks()) {
                        for (Transacao transacao : block.getTransacoes()) {
                            sb.append(transacao.toString() + "\n\n");
                            sb.append("-------------------------------------- \n");
                        }
                    }
                    outStream.writeObject(sb.toString());
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

            File f = new File(SERVER_FILES_DIR + fileName);
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

            File fileToSend = new File(SERVER_FILES_DIR + fileName);
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
                     -> list
                     -> exit
                     Selecione um comando:\s""";
        }
    }
}