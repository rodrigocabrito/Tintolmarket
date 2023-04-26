import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


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

    // file paths
    private static final String AUTHENTICATION_FILE_CIF = "./data_bases/users.cif";
    private static final String AUTHENTICATION_FILE_KEY = "./data_bases/users.key";
    private static final String BALANCE_FILE_TXT = "./data_bases/balance.txt";
    private static final String WINES_FILE_TXT = "./data_bases/wines.txt";
    private static final String FORSALE_FILE_TXT = "./data_bases/forSale.txt";

    // directories
    private static final String SERVER_FILES_DIR = "./serverFiles/";
    private static final String KEYSTORE_DIR = "./keystores/";
    private static final String CERTIFICATES_DIR = "./certificates/";
    private static final String CHAT_DIR = "./chat/";

    // server information
    private static PublicKey serverPublicKey = null;
    private static PrivateKey serverPrivateKey = null;
    private static Signature serverSignature = null;
    private static KeyStore trustStore = null;

    // server memory
    private static final Blockchain blockchain = new Blockchain();
    private static final ArrayList<Utilizador> listaUts = new ArrayList<>();
    private static final ArrayList<Wine> listaWines = new ArrayList<>();
    private static final HashMap<Utilizador, ArrayList<Sale>> forSale = new HashMap<>();

    public static void main(String[] args) throws Exception {

        int port;
        String cipherPassword;
        String keyStoreFileName;
        String keyStorePassword;

        if (args.length == 4) {
            port = Integer.parseInt(args[0]);
            cipherPassword = args[1];
            keyStoreFileName = args[2];
            keyStorePassword = args[3];
        } else {
            port = 12345;
            cipherPassword = args[0];
            keyStoreFileName = args[1];
            keyStorePassword = args[2];
        }

        String serverKeyAlias = "server_key_alias"; // from keyStore
        String serverCertAlias = "server_cert_alias"; // from trustStore

        String keyStorePath = KEYSTORE_DIR + keyStoreFileName;
        String trustStorePath = KEYSTORE_DIR + "tintolmarket_trustStore.jks";
        String defaultPasswordTrustStore = "changeit"; //TODO idk

        // get a trustStore containing the client's trusted certificates (if needed)
        FileInputStream kfile = new FileInputStream(trustStorePath);
        trustStore = KeyStore.getInstance("JCEKS");
        trustStore.load(kfile, defaultPasswordTrustStore.toCharArray());

        // get keystore from args
        FileInputStream kfile2 = new FileInputStream(keyStorePath);
        KeyStore keyStore = KeyStore.getInstance("JCEKS");
        keyStore.load(kfile2, keyStorePassword.toCharArray());

        // Create a KeyManagerFactory to extract the server's private key and certificate from the keystore
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());

        // Create a TrustManagerFactory to extract the client's public key from the truststore
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        // get self certificate and keys
        java.security.cert.Certificate cert = keyStore.getCertificate(serverKeyAlias);
        serverPublicKey = cert.getPublicKey();
        serverPrivateKey = (PrivateKey) keyStore.getKey(serverKeyAlias, keyStorePassword.toCharArray());
        serverSignature = Signature.getInstance("MD5withRSA");
        serverSignature.initSign(serverPrivateKey);

        Mac mac = Mac.getInstance("HmacSHA256");

        // TODO verificar salt e iterationCount param da funcao PBEKeySpec (20)
        // Generate the key based on the password passeed by args[1]
        byte[] salt = { (byte) 0xc9, (byte) 0x36, (byte) 0x78, (byte) 0x99, (byte) 0x52, (byte) 0x3e, (byte) 0xea, (byte) 0xf2 };
        PBEKeySpec keySpec = new PBEKeySpec(cipherPassword.toCharArray(), salt, 1000); // passw, salt, iterations
        SecretKeyFactory kf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
        SecretKey key = kf.generateSecret(keySpec);
        mac.init(key);

        Cipher cipher = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        updateServerMemory();

        verifyIntegrityBlockchain(); //TODO check this

        System.setProperty("javax.net.ssl.keyStore", keyStorePath);
        System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);
        System.setProperty("javax.net.ssl.trustStore", trustStorePath);
        System.setProperty("javax.net.ssl.trustStorePassword", defaultPasswordTrustStore);

        // Create an SSL context and configure it to use the key and trust managers
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());

        // Create an SSL socket factory and set it as the default socket factory
        SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();
        SSLServerSocket serverSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(port);

        // Create a thread pool to handle multiple client connections concurrently
        ExecutorService executorService = Executors.newCachedThreadPool();

        while (true) {
            System.out.println("Waiting for a connection...");
            SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
            executorService.submit(new clientHandlerThread(clientSocket, cipher, key));
        }
        //serverSocket.close();
    }

    private static void verifyIntegrityBlockchain() throws Exception {
        blockchain.loadBlocks();

        if (blockchain.getBlocks().size() != 0) {
            if (blockchain.isChainValid()) {
                System.out.println(" Blockchain valida! \n");
            } else {
                System.out.println(" Blockchain corrompida! \n"); // TODO exit?
            }

            for (Block block : blockchain.getBlocks()) {
                if (block.isBlockFull()) {
                    StringBuilder sb = new StringBuilder();
                    String data = block.getHash() + block.getId() + block.getNrTransacoes();
                    sb.append(data);
                    for (Transacao transacao : block.getTransacoes()) {
                        sb.append(transacao.toString());
                    }
                    byte[] dataBytes = sb.toString().getBytes();
                    byte[] signature = sign(dataBytes);
                    boolean validSignature = verify(dataBytes, signature);

                    if (validSignature) {
                        System.out.println("Assinatura do bloco com id " + block.getId() + " eh valido \n");
                    } else {
                        System.out.println("Assinatura do bloco com id " + block.getId() + " eh invalido \n"); // TODO exit?
                    }
                }
            }
        }
    }

    private static byte[] sign(byte[] data) throws Exception {
        serverSignature.update(data);

        // Generate the digital signature
        return serverSignature.sign();
    }

    private static boolean verify(byte[] data, byte[] signature) throws Exception {
        serverSignature.update(data);

        // Verify the digital signature
        return serverSignature.verify(signature);
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
            listaUts.add(new Utilizador(Integer.parseInt(lineSplit[0]), Integer.parseInt(lineSplit[1])));
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
                if (u.getUserID() == (Integer.parseInt(splitLine[0]))) {
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

    // TODO check this
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

        private BufferedWriter bwAuthTxt = null; //TODO remove
        private FileOutputStream fosAuthKey = null;
        private FileInputStream fisAuthTxt = null; //TODO remove
        private FileOutputStream fosAuthCif = null;
        private CipherOutputStream cosAuthCif = null;
        private ObjectOutputStream oos = null;

        private BufferedReader brAuthTxt = null; //TODO remove
        private FileInputStream fisAuthKey = null;
        private FileOutputStream fosAuthTxt = null; //TODO remove
        private FileInputStream fisAuthCif = null;
        private CipherInputStream cisAuthCif = null;
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

                int userID = 0;
                boolean newUser = true;
                Random random = new Random();
                long nonce = random.nextLong() & 0xFFFFFFFFFFFFFFL;

                // for decryption
                //cipher.doFinal("Ola Joana!".getBytes());
                byte[] params = cipher.getParameters().getEncoded();
                AlgorithmParameters p = AlgorithmParameters.getInstance("PBEWithHmacSHA256AndAES_128");
                p.init(params);

                // write the key to the users.cif file on users.key file
                byte[] keyEncoded = key.getEncoded();
                fosAuthKey = new FileOutputStream(AUTHENTICATION_FILE_KEY);
                oos = new ObjectOutputStream(fosAuthKey);
                oos.writeObject(keyEncoded);
                oos.close();
                fosAuthKey.close();

                try {

                    userID = (int) inStream.readObject();

                    // check if the user is already known to the server
                    if (listaUts.size() != 0) {
                        for (Utilizador u : listaUts) {
                            if (u.getUserID() == userID) {
                                ut = u;
                                newUser = false;
                            }
                        }
                    }

                    // authenticate user
                    if (newUser) {
                        // enviar nonce com flag de ut desconhecido
                        outStream.writeObject(nonce + ":newUser");

                        // receber nonce, assinatura e certificado do cliente
                        long nonceFromClient = (long) inStream.readObject();
                        byte[] signatureBytes = (byte[]) inStream.readObject();
                        X509Certificate certificate = (X509Certificate) inStream.readObject();
                        clientPublicKey = certificate.getPublicKey();

                        // verificar assinatura do cliente
                        Signature signature = Signature.getInstance("SHA256withRSA");
                        signature.initVerify(clientPublicKey); // clientPublicKey is the public key of the client stored on the server
                        signature.update("Hello, server!".getBytes());
                        boolean verifiedSignature = signature.verify(signatureBytes);

                        // verificar se nonce é o enviado && assinatura verificada
                        if ((nonceFromClient == nonce) && verifiedSignature) {
                            outStream.writeObject("Registo e autenticacao bem sucedida! \n");

                            ut = new Utilizador(userID, 200);
                            listaUts.add(ut);
                        } else {
                            outStream.writeObject("Registo e autenticacao nao foi bem sucedida... \n");
                            System.exit(-1); //TODO is it supose to exit?
                        }

                    } else { //TODO
                        outStream.writeObject(Long.toString(nonce));

                        String userCertPath = null;
                        String keyAlias = "user" + userID + "_key_alias";
                        String line;

                        // decrypt
                        FileInputStream fisKey = new FileInputStream(AUTHENTICATION_FILE_KEY);
                        ObjectInputStream oisKey = new ObjectInputStream(fisKey);
                        //byte[] keyEncoded2 = (byte[]) oisKey.readObject();
                        byte[] keyEncoded2 = key.getEncoded();
                        oisKey.close();

                        SecretKeySpec keySpec = new SecretKeySpec(keyEncoded2, "PBEWithHmacSHA256AndAES_128");
                        Cipher d = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
                        d.init(Cipher.DECRYPT_MODE, keySpec, p);

                        FileInputStream ffis = new FileInputStream(AUTHENTICATION_FILE_CIF);
                        FileOutputStream ffos = new FileOutputStream("temp.txt");
                        CipherInputStream cis;
                        cis = new CipherInputStream(ffis, d);
                        byte[] b1 = new byte[1024];

                        int i1 = 0;
                        System.out.println("Este é o i1 fora do while: " + i1);
                        while ((i1 = cis.read(b1)) != -1) {
                            ffos.write(b1, 0, i1);
                            System.out.println("i1 after read: " + i1);
                        }

                        // Read each line from temp.txt (users.cif)
                        BufferedReader br = new BufferedReader(new FileReader("temp.txt"));
                        boolean done = false;
                        while (((line = br.readLine()) != null) && !done) {
                            String[] splitLine = line.split(":");
                            if (splitLine[0].equals(Integer.toString(userID))) {
                                userCertPath = splitLine[1];
                                done = true;
                            }
                        }
                        br.close();
                        System.out.println(done);

                        //Encrypt
                        FileInputStream fis;
                        FileOutputStream fos;
                        CipherOutputStream cos;

                        fis = new FileInputStream("temp.txt");
                        fos = new FileOutputStream(AUTHENTICATION_FILE_CIF);

                        cos = new CipherOutputStream(fos, cipher);
                        byte[] b = new byte[1024];
                        int i = fis.read(b);
                        while (i != -1) {
                            cos.write(b, 0, i);
                            i = fis.read(b);
                        }

                        cos.close();
                        fis.close();
                        fos.close();

                        File file = new File("temp.txt");
                        file.delete();

                        // receber assinatura do cliente e verificar com a
                        // chave publica do users.txt desse cliente
                        byte[] signedNonce = (byte[]) inStream.readObject();

                        //KeyStore keystore = KeyStore.getInstance("JCEKS");
                        //keystore.load(new FileInputStream(keyStorePath), null);

                        //KeyStore.Entry entry = trustStore.getEntry(keyAlias, null); //TODO mudar para getKeyEntry
                        //KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) entry;
                        //clientPrivateKey = privateKeyEntry.getPrivateKey();

                        FileInputStream fis2 = new FileInputStream(CERTIFICATES_DIR + userCertPath);
                        CertificateFactory cf = CertificateFactory.getInstance("X509");
                        X509Certificate cert = (X509Certificate) cf.generateCertificate(fis2);
                        //java.security.cert.Certificate[] chain = ((KeyStore.PrivateKeyEntry) entry).getCertificateChain();
                        //java.security.cert.Certificate[] chain2 = trustStore.getCertificateChain();
                        // Get the public key from the certificate from the keystore on users.txt
                        PublicKey publicKey = cert.getPublicKey();

                        // verificar a assinatura do cliente com a chave publica
                        Signature verifier = Signature.getInstance("SHA256withRSA");
                        verifier.initVerify(publicKey);
                        verifier.update(String.valueOf(nonce).getBytes());
                        boolean validSignature = verifier.verify(signedNonce);

                        if (validSignature) {
                            outStream.writeObject("Autenticacao bem sucedida! \n");
                        } else {
                            outStream.writeObject("Autenticacao inválida! \n"); // TODO supose to exit?
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                boolean isNewFile = false;
                // create users.cif file in case it doesn't exist
                try {
                    System.out.println("ficheiro existe");
                    fisAuthCif = new FileInputStream((AUTHENTICATION_FILE_CIF));
                    cisAuthCif = new CipherInputStream(fisAuthCif, cipher);
                } catch (FileNotFoundException e) {
                    // file doesn't exist, create it
                    try {
                        isNewFile = true;
                        FileWriter fw = new FileWriter(AUTHENTICATION_FILE_CIF);
                        fw.close();
                        fisAuthCif = new FileInputStream((AUTHENTICATION_FILE_CIF));
                        cisAuthCif = new CipherInputStream(fisAuthCif, cipher);
                        System.out.println("ficheiro nao existia");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                StringBuilder sb = new StringBuilder();
                String line = null;

                // only update users.cif if it's a new user
                if (newUser) {
                    /*
                    BufferedWriter bw = new BufferedWriter(new FileWriter(AUTHENTICATION_FILE_CIF));
                    bw.write(userID + ":" + "user" + userID + "_certificate.cer" + "\n");

                    bw.close();
                    */
                    /*
                    // Create CipherInputStream to read encrypted .cif file
                    FileInputStream fis = new FileInputStream(AUTHENTICATION_FILE_CIF);
                    CipherInputStream cis = new CipherInputStream(fis, cipher);

                    // Read each line from the CipherInputStream
                    BufferedReader br = new BufferedReader(new InputStreamReader(cis));

                    //TODO check talk (use same method)
                    while (((line = br.readLine()) != null)) {
                        sb.append(line + "\n");
                    }
                    br.close();

                    sb.append(userID + ":" + "user" + userID + "_certificate.cer" + "\n");

                    fosAuthCif = new FileOutputStream(AUTHENTICATION_FILE_CIF);
                    cosAuthCif = new CipherOutputStream(fosAuthCif, cipher);

                    //update cif file with new user
                    cosAuthCif.write(sb.toString().getBytes());
                    cosAuthCif.close();
                    */

                    if (!isNewFile) {

                        // decrypt
                        FileInputStream fisKey = new FileInputStream(AUTHENTICATION_FILE_KEY);
                        ObjectInputStream oisKey = new ObjectInputStream(fisKey);
                        //byte[] keyEncoded3 = (byte[]) oisKey.readObject();
                        byte[] keyEncoded3 = key.getEncoded();
                        oisKey.close();

                        SecretKeySpec keySpec = new SecretKeySpec(keyEncoded3, "PBEWithHmacSHA256AndAES_128");
                        Cipher d = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
                        d.init(Cipher.DECRYPT_MODE, keySpec, p);

                        FileInputStream ffis = new FileInputStream(AUTHENTICATION_FILE_CIF);
                        FileOutputStream ffos = new FileOutputStream("temp.txt");
                        CipherInputStream cis;
                        cis = new CipherInputStream(ffis, d);
                        byte[] b1 = new byte[1024];
                        int i1 = cis.read(b1);
                        while (i1 != -1) {
                            ffos.write(b1, 0, i1);
                            i1 = cis.read(b1);
                        }

                        String toAdd = userID + ":" + "user" + userID + "_certificate.cer" + "\n";
                        ffos.write(toAdd.getBytes());

                        fisKey.close();
                        ffos.close();
                        ffis.close();
                        cis.close();
                    } else {
                        FileOutputStream ffos = new FileOutputStream("temp.txt");
                        String toAdd = userID + ":" + "user" + userID + "_certificate.cer" + "\n";
                        ffos.write(toAdd.getBytes());
                    }

                    // Encrypt
                    FileInputStream fis;
                    FileOutputStream fos;
                    CipherOutputStream cos;

                    fis = new FileInputStream("temp.txt");
                    fos = new FileOutputStream(AUTHENTICATION_FILE_CIF);

                    cos = new CipherOutputStream(fos, cipher);
                    byte[] b = new byte[1024];
                    int i = fis.read(b);
                    while (i != -1) {
                        cos.write(b, 0, i);
                        i = fis.read(b);
                    }

                    cos.close();
                    fis.close();
                    fos.close();

                    File file = new File("temp.txt");
                    file.delete();
                }


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
         * @param encryptedMessage
         * @param receiverId
         */
        private static void updateChat(int receiverId, byte[] encryptedMessage, Cipher cipherToEncrypt) { //TODO probably wrong
            try {
                byte[] hash = digestFile(CHAT_DIR + "user" + receiverId + ".cif");

                // Open the existing .cif file for reading
                File originalFile = new File(CHAT_DIR + "user" + receiverId + ".cif");
                FileInputStream fis = new FileInputStream(originalFile);

                // Create a temporary file for writing the modified .cif data
                File tempFile = File.createTempFile("modified_cif_file", ".cif");
                FileOutputStream fos = new FileOutputStream(tempFile);
                CipherOutputStream cos = new CipherOutputStream(fos, cipherToEncrypt);

                // Read the existing data from the input stream and write it to the CipherOutputStream
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    cos.write(buffer, 0, bytesRead);
                }

                if (isCorrupted(CHAT_DIR + "user" + receiverId + ".cif", hash)) {
                    System.out.println("Tudo ok com o ficheiro user " + receiverId + ".cif \n");
                } else {
                    System.out.println("Ficheiro chat.cif corrupto! \n");
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
                    throw new IOException("Failed to delete the original .cif file.");
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
                            // TODO client envia assinatura assinada com "venda" e aqui verifica-se como foi feito na cena do nonce
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

                                if (u.getUserID() == ut.getUserID()) {

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

                } else if (Integer.parseInt(splitCommand[2]) == ut.getUserID()) {
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
                            
                            if (utSeller.getUserID() == (Integer.parseInt(splitCommand[2])) && !sold) {// find seller
                                
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

                if (Integer.parseInt(splitCommand[1]) == ut.getUserID()) {
                    outStream.writeObject("Comando invalido! O <receiver> nao pode ser o proprio! \n");

                } else {
                    Utilizador receiver = null;
                    for (Utilizador u : TintolmarketServer.listaUts) {
                        if (u.getUserID() == (Integer.parseInt(splitCommand[1])) && !contains) {
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
                        cipher.init(Cipher.DECRYPT_MODE, clientPrivateKey); //TODO client envia a propria privateKey

                        boolean empty = true;
                        String messages = "";

                        if (chatFiles != null) {
                            // Read each .cif file in chat directory
                            for (File chatFile : chatFiles) {
                                if (chatFile.getName().contains(Integer.toString(ut.getUserID()))) {
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
                    if (blockchain.getBlocks() != null) {
                        for (Block block : blockchain.getBlocks()) {

                            if (block.getTransacoes() != null) {
                                for (Transacao transacao : block.getTransacoes()) {

                                    sb.append(transacao.toString() + "\n\n");
                                    sb.append("-------------------------------------- \n");
                                }
                            }
                        }
                        outStream.writeObject(sb.toString());
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