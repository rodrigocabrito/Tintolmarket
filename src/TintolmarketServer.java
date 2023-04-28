import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
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
    private static final String SERVER_FILES_DIR = "./server_files/";
    private static final String KEYSTORE_DIR = "./keystores/";
    private static final String CERTIFICATES_DIR = "./certificates/";
    private static final String CHAT_DIR = "./chat/";
    private static final String CHAT_KEYS_DIR = "./chat_keys/";

    // server information
    private static PublicKey serverPublicKey = null;
    private static PrivateKey serverPrivateKey = null;
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
 /*
        int port = 12345;
        String cipherPassword = "olaadeus";
        String keyStoreFileName = "server_keyStore.jks";
        String keyStorePassword = "server_keyStore_passw";
*/
        String serverKeyAlias = "server_key_alias"; // from keyStore

        String keyStorePath = KEYSTORE_DIR + keyStoreFileName;
        String trustStorePath = KEYSTORE_DIR + "tintolmarket_trustStore.jks";
        String defaultPasswordTrustStore = "changeit";

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
        FileInputStream fis = new FileInputStream(CERTIFICATES_DIR + "server_certificate.cer");
        CertificateFactory cf = CertificateFactory.getInstance("X509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(fis);
        serverPublicKey = cert.getPublicKey();
        serverPrivateKey = (PrivateKey) keyStore.getKey(serverKeyAlias, keyStorePassword.toCharArray());

        Mac mac = Mac.getInstance("HmacSHA256");

        // Generate the key based on the password passeed by args[1]
        byte[] salt = { (byte) 0xc9, (byte) 0x36, (byte) 0x78, (byte) 0x99, (byte) 0x52, (byte) 0x3e, (byte) 0xea, (byte) 0xf2 };
        PBEKeySpec keySpec = new PBEKeySpec(cipherPassword.toCharArray(), salt, 1000); // passw, salt, iterations
        SecretKeyFactory kf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
        SecretKey key = kf.generateSecret(keySpec);
        mac.init(key);

        Cipher cipher = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        AlgorithmParameters paramsToDecrypt = null;

        // check if params.txt exists
        try {
            // if it does, read from it and create AlgorithmParameters object
            BufferedReader br = new BufferedReader(new FileReader("./params.txt"));
            br.close();

            byte[] encodedParams = Files.readAllBytes(Paths.get("./params.txt"));
            paramsToDecrypt = AlgorithmParameters.getInstance("PBEWithHmacSHA256AndAES_128");
            paramsToDecrypt.init(encodedParams);

        } catch (FileNotFoundException e) {
            // if it doesn't exist, create it and store the params in it

            byte[] params = cipher.getParameters().getEncoded();
            paramsToDecrypt = AlgorithmParameters.getInstance("PBEWithHmacSHA256AndAES_128");
            paramsToDecrypt.init(params);

            //store in a file and get later
            FileOutputStream fos = new FileOutputStream("./params.txt");
            fos.write(paramsToDecrypt.getEncoded());
            fos.close();
        }

        updateServerMemory();

        verifyIntegrityBlockchain(); //TODO check this (checked)

        System.setProperty("javax.net.ssl.keyStore", keyStorePath);
        System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);
        System.setProperty("javax.net.ssl.trustStore", trustStorePath);
        System.setProperty("javax.net.ssl.trustStorePassword", defaultPasswordTrustStore);

        // Create an SSL context and configure it to use the key and trust managers
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());

        // Create an SSL socket factory and set it as the default socket factory
        SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();
        try (SSLServerSocket serverSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(port)) {

            // Create a thread pool to handle multiple client connections concurrently
            try (ExecutorService executorService = Executors.newCachedThreadPool()) {

                while (true) {
                    System.out.println("Waiting for a connection...");
                    SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                    executorService.submit(new clientHandlerThread(clientSocket, cipher, key, paramsToDecrypt));
                }
            }
        }
        //serverSocket.close();
    }

    private static void verifyIntegrityBlockchain() throws Exception {
        blockchain.loadBlocks();

        if (blockchain.getLastBlock().getNrTransacoes() == 0) {
            if (blockchain.isChainValid()) {
                System.out.println(" Blockchain valida! \n");
            } else {
                System.out.println(" Blockchain corrompida! \n");
                System.exit(-1);
            }

            Signature serverSignature = Signature.getInstance("SHA256withRSA");
            serverSignature.initVerify(serverPublicKey);

            for (Block block : blockchain.getBlocks()) {
                if (block.isBlockFull()) {
                    StringBuilder sb = new StringBuilder();
                    String data = block.getHash() + block.getId() + block.getNrTransacoes();
                    sb.append(data);

                    for (Transacao transacao : block.getTransacoes()) {
                        sb.append(transacao.toString());
                    }

                    byte[] dataBytes = sb.toString().getBytes();
                    serverSignature.update(dataBytes);
                    byte[] signature = serverSignature.sign();
                    boolean validSignature = serverSignature.verify(signature);

                    if (validSignature) {
                        System.out.println("Assinatura do bloco com id " + block.getId() + " eh valido \n");
                    } else {
                        System.out.println("Assinatura do bloco com id " + block.getId() + " eh invalido \n");
                        System.exit(-1);
                    }
                }
            }
        } else {
            System.out.println(" Blockchain vazia! \n");
        }
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
        byte[] hash = digestFile(FORSALE_FILE_TXT);

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
                if (w.name().equals(splitLine[1])) {
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

    // TODO check this (talvez cagar nisto)
    private static boolean isCorrupted(String pathToFile, byte[] hashToCompare) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(Files.readAllBytes(Paths.get(pathToFile)));

        return !Arrays.equals(hashToCompare, hash);
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
        private final AlgorithmParameters params;
        private static Utilizador ut = null;
        private static PublicKey clientPublicKey = null;

        private static ObjectOutputStream outStream;
        private static ObjectInputStream inStream;

        private FileOutputStream fosAuthKey = null;
        private ObjectOutputStream oos = null;
        private FileInputStream fisAuthCif = null;
        private CipherInputStream cisAuthCif = null;


        clientHandlerThread(SSLSocket sslSocket, Cipher cipher, SecretKey key, AlgorithmParameters params) {
            this.sslSocket = sslSocket;
            this.cipher = cipher;
            this.key = key;
            this.params = params;
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

                try {
                    BufferedReader br = new BufferedReader(new FileReader(AUTHENTICATION_FILE_KEY));
                    br.close();
                } catch (FileNotFoundException e) {

                    // write the key to the users.cif file on users.key file
                    byte[] keyEncoded = key.getEncoded();
                    fosAuthKey = new FileOutputStream(AUTHENTICATION_FILE_KEY);
                    oos = new ObjectOutputStream(fosAuthKey);
                    oos.writeObject(keyEncoded);
                    oos.close();
                    fosAuthKey.close();
                }

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
                        // enviar nonce com ‘flag’ de ut desconhecido
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
                            outStream.writeObject("Autenticacao bem sucedida! \n");

                            ut = new Utilizador(userID, 200);
                            listaUts.add(ut);
                        } else {
                            outStream.writeObject("Erro na autenticacao...\n");
                        }

                    } else {
                        outStream.writeObject(Long.toString(nonce));

                        String userCertPath = null;
                        String line;

                        // decrypt
                        FileInputStream fisKey = new FileInputStream(AUTHENTICATION_FILE_KEY);
                        ObjectInputStream oisKey = new ObjectInputStream(fisKey);
                        byte[] keyEncoded2 = (byte[]) oisKey.readObject();
                        oisKey.close();

                        SecretKeySpec keySpec = new SecretKeySpec(keyEncoded2, "PBEWithHmacSHA256AndAES_128");
                        Cipher d = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
                        d.init(Cipher.DECRYPT_MODE, keySpec, params);

                        FileInputStream ffis = new FileInputStream(AUTHENTICATION_FILE_CIF);
                        FileOutputStream ffos = new FileOutputStream("temp.txt");
                        CipherInputStream cis;
                        cis = new CipherInputStream(ffis, d);
                        byte[] b1 = new byte[1024];
                        int i1;
                        while ((i1 = cis.read(b1)) != -1) {
                            ffos.write(b1, 0, i1);
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

                        File file = new File("./temp.txt");
                        file.delete();

                        // receber assinatura do cliente e verificar com a
                        // chave publica do users.txt desse cliente
                        byte[] signedNonce = (byte[]) inStream.readObject();

                        FileInputStream fis2 = new FileInputStream(CERTIFICATES_DIR + userCertPath);
                        CertificateFactory cf = CertificateFactory.getInstance("X509");
                        X509Certificate cert = (X509Certificate) cf.generateCertificate(fis2);

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
                            outStream.writeObject("Erro na autenticacao...\n");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                boolean isNewFile = false;
                // create users.cif file in case it doesn't exist
                try {
                    fisAuthCif = new FileInputStream((AUTHENTICATION_FILE_CIF));
                    cisAuthCif = new CipherInputStream(fisAuthCif, cipher);
                } catch (FileNotFoundException e) {
                    // file doesn't exist, create it
                    try {
                        isNewFile = true;
                        FileOutputStream fos = new FileOutputStream(AUTHENTICATION_FILE_CIF);
                        fos.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                // only update users.cif if it's a new user
                if (newUser) {
                    System.out.println("new user"); //teste
                    if (!isNewFile) {

                        // decrypt
                        FileInputStream fisKey = new FileInputStream(AUTHENTICATION_FILE_KEY);
                        ObjectInputStream oisKey = new ObjectInputStream(fisKey);
                        byte[] keyEncoded3 = (byte[]) oisKey.readObject();
                        oisKey.close();

                        SecretKeySpec keySpec = new SecretKeySpec(keyEncoded3, "PBEWithHmacSHA256AndAES_128");
                        Cipher d = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
                        d.init(Cipher.DECRYPT_MODE, keySpec, params);

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
                        try (FileOutputStream ffos = new FileOutputStream("temp.txt")) {
                            String toAdd = userID + ":" + "user" + userID + "_certificate.cer" + "\n";
                            ffos.write(toAdd.getBytes());
                        }
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

                    File file = new File("./temp.txt");
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
                    for (Integer j : wine.stars()) {
                        sb.append(j);
                        sb.append(";");
                    }
                    String stars = sb.toString();

                    // nome;imagem;star1;star2;star3...
                    bwWine.write(wine.name() + ";" + wine.image() + ";" + stars + "\n");
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
                                    bwSale.write(u.getUserID() + ";" + (sale.getWine()).name() + ";"
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
         * Updates the chat.cif file, appending the new interaction between users.
         * 
         * @param encryptedMessage
         * @param receiverId
         */
        private static void updateChat(int receiverId, byte[] encryptedMessage,
                                       Cipher cipherToEncrypt, PublicKey receiverPublicKey, Key sharedKey)
                throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException { //TODO

            // store the wrapped key in .txt file
            try {
                BufferedReader br = new BufferedReader((new FileReader(CHAT_KEYS_DIR + "user" + receiverId + "_chat_key.txt")));
                br.close();
            } catch (FileNotFoundException e) {
                // if file doesn't exist, create it

                // preparar o algoritmo de cifra para cifrar a chave secreta
                Cipher cipherWrap = Cipher.getInstance("RSA");
                cipherWrap.init(Cipher.WRAP_MODE, receiverPublicKey);
                // cifrar a chave secreta que queremos enviar
                byte[] wrappedKey = cipherWrap.wrap(sharedKey);

                FileOutputStream fos = new FileOutputStream(CHAT_KEYS_DIR + "user" + receiverId + "_chat_key.txt");
                fos.write(wrappedKey);
                fos.close();
            }

            // updating user_chatX.cif file of receiver user with id = X
            try {
                BufferedReader br = new BufferedReader(new FileReader(CHAT_DIR + "user" + receiverId + "_chat.cif"));
                br.close();

                // Open the existing .cif file for reading
                File originalFile = new File(CHAT_DIR + "user" + receiverId + "_chat.cif");
                FileInputStream fis = new FileInputStream(originalFile);

                // Create a temporary file for writing the modified .cif data
                File tempFile = File.createTempFile("modified_cif_file", ".cif");
                FileOutputStream fos = new FileOutputStream(tempFile);
                CipherOutputStream cos = new CipherOutputStream(fos, cipherToEncrypt);

                // Read the existing data and write it to the CipherOutputStream
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    cos.write(buffer, 0, bytesRead);
                }

                // Write the new data to the CipherOutputStream
                cos.write(encryptedMessage);

                // Close the streams
                cos.close();
                fis.close();
                fos.close();

                // Replace the original file with the modified file
                if (originalFile.delete()) {
                    Files.move(tempFile.toPath(), originalFile.toPath());
                } else {
                    throw new IOException("Failed to delete the original .cif file.");
                }

            } catch (FileNotFoundException e) {
                // if the file doesn't exist, create it

                // Create a file to write the message
                File file = new File(CHAT_DIR + "user" + receiverId + "_chat.cif");
                FileOutputStream fos = new FileOutputStream(file);
                CipherOutputStream cos = new CipherOutputStream(fos, cipherToEncrypt);

                // write the message to the CipherOutputStream
                cos.write(encryptedMessage);

                cos.close();
                fos.close();
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

                            if (w.name().equals(splitCommand[1])) {
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
                    outStream.writeObject("Erro!");
                    outStream.writeObject(
                            "Comando invalido! A operacao sell necessita de 3 argumentos <name> <image> <quantity>. \n");
                } else {

                    if (TintolmarketServer.listaWines.isEmpty()) {
                        outStream.writeObject("Erro!");
                        outStream.writeObject("O vinho que deseja vender nao existe! \n");

                    } else {
                        for (Wine w : TintolmarketServer.listaWines) {
                            if (w.name().equals(splitCommand[1]) && !contains) {
                                wine = w;
                                contains = true;
                            }
                        }

                        if (!contains) {
                            outStream.writeObject("Erro!");
                            outStream.writeObject("O vinho que deseja vender nao existe! \n");

                        } else if (TintolmarketServer.forSale.size() == 0) {
                            outStream.writeObject("Nova transacao!");

                            Transacao tSell = new Transacao(wine.name(), Integer.parseInt(splitCommand[3]),
                                    Integer.parseInt(splitCommand[2]), ut.getUserID(), TransacaoType.SELL);

                            // verificar assinatura pelo cliente

                            // get signature signed with tSell data
                            String data = "user" + ut.getUserID() + " pos a venda vinho!";
                            byte[] dataBytes = data.getBytes();

                            outStream.writeObject(dataBytes);

                            byte[] signedSellBytes = (byte[]) inStream.readObject();

                            FileInputStream fis = new FileInputStream(CERTIFICATES_DIR + "user" + ut.getUserID() + "_certificate.cer");
                            CertificateFactory cf = CertificateFactory.getInstance("X509");
                            X509Certificate cert = (X509Certificate) cf.generateCertificate(fis);
                            PublicKey clientPublicKey = cert.getPublicKey();

                            Signature clientSignature = Signature.getInstance("SHA256withRSA");
                            clientSignature.initVerify(clientPublicKey);

                            clientSignature.update(data.getBytes());
                            boolean verifiedSignature = clientSignature.verify(signedSellBytes);

                            if (verifiedSignature) {

                                Signature serverSignature = Signature.getInstance("SHA256withRSA");
                                serverSignature.initSign(serverPrivateKey);
                                blockchain.addTransacao(tSell, serverSignature);

                                ArrayList<Sale> sales = new ArrayList<>();
                                sales.add(new Sale(wine, Integer.parseInt(splitCommand[2]),
                                        Integer.parseInt(splitCommand[3])));
                                TintolmarketServer.forSale.put(ut, sales);
                            }
                            outStream.writeObject("Vinho colocado a venda! \n");

                        } else {
                            boolean updated = false;
                            for (HashMap.Entry<Utilizador,ArrayList<Sale>> entry : TintolmarketServer.forSale.entrySet()) {
                                Utilizador u = entry.getKey();

                                if (u.getUserID() == ut.getUserID()) {

                                    ArrayList<Sale> sales = entry.getValue();
                                    for (Sale sale : sales) {
                                        if (sale.getWine().name().equals(splitCommand[1])) { // caso o vinho ja
                                                                                                // esteja a venda(mesmo
                                                                                                // ut)
                                            outStream.writeObject("Venda para dar update!");
                                            sale.updateValue(Integer.parseInt(splitCommand[2])); // muda o preço
                                            sale.updateQuantity(Integer.parseInt(splitCommand[3])); // incrementa a
                                                                                                    // quantidade

                                            for (Block block : blockchain.getBlocks()) {
                                                for (Transacao transacao : block.getTransacoes()) {

                                                    if (transacao.getType() == TransacaoType.SELL) {
                                                        if (transacao.getVinhoName().equals(splitCommand[1])) {

                                                            transacao.updateValue(Integer.parseInt(splitCommand[2]));
                                                            transacao.updateQuantity(Integer.parseInt(splitCommand[3]));
                                                        }
                                                    }
                                                }
                                                blockchain.updateBlockFile(block);
                                            }

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
                                outStream.writeObject("Nova transacao!");

                                Transacao tSell = new Transacao(wine.name(), Integer.parseInt(splitCommand[3]),
                                        Integer.parseInt(splitCommand[2]), ut.getUserID(), TransacaoType.SELL);

                                // get signature signed with tSell data
                                String data = "user" + ut.getUserID() + " pos a venda vinho!";
                                byte[] dataBytes = data.getBytes();

                                outStream.writeObject(dataBytes);

                                byte[] signedSellBytes = (byte[]) inStream.readObject();

                                FileInputStream fis = new FileInputStream(CERTIFICATES_DIR + "user" + ut.getUserID() + "_certificate.cer");
                                CertificateFactory cf = CertificateFactory.getInstance("X509");
                                X509Certificate cert = (X509Certificate) cf.generateCertificate(fis);
                                PublicKey clientPublicKey = cert.getPublicKey();

                                Signature clientSignature = Signature.getInstance("SHA256withRSA");
                                clientSignature.initVerify(clientPublicKey);

                                clientSignature.update(data.getBytes());
                                boolean verifiedSignature = clientSignature.verify(signedSellBytes);

                                if (verifiedSignature) {

                                    Signature serverSignature = Signature.getInstance("SHA256withRSA");
                                    serverSignature.initSign(serverPrivateKey);
                                    blockchain.addTransacao(tSell, serverSignature);

                                    ArrayList<Sale> sales = TintolmarketServer.forSale.get(ut);

                                    if (sales == null) {
                                        sales = new ArrayList<>();
                                    }

                                    sales.add(new Sale(wine, Integer.parseInt(splitCommand[2]),
                                            Integer.parseInt(splitCommand[3])));
                                    TintolmarketServer.forSale.put(ut, sales);
                                }
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
                        if (wine.name().equals(splitCommand[1]) && !contains) {
                            contains = true;
                            w = wine;
                        }
                    }

                    if (!contains) {

                        outStream.writeObject("O vinho que deseja ver nao existe! \n");
                    } else {    

                        StringBuilder sb = new StringBuilder();
                        sb.append("Nome: " + w.name() + "\n");
                        sb.append("Imagem: " + w.image() + "\n");
                        sb.append("Classificacao media: " + w.getAvgRate() + "\n");

                        for (HashMap.Entry<Utilizador,ArrayList<Sale>> entry : TintolmarketServer.forSale.entrySet()) {
                            Utilizador u = entry.getKey();
                            ArrayList<Sale> sales = entry.getValue();
                            
                            for (Sale sale : sales) {
                                if (w.name().equals(sale.getWine().name()) && sale.getQuantity() > 0) {

                                    sb.append("-------------------------------------- \n");
                                    sb.append("Vendedor: " + u.getUserID() + "\n");
                                    sb.append("Preco unitario: " + sale.getValue() + "\n");
                                    sb.append("Quantidade: " + sale.getQuantity() + "\n");
                                }
                            }
                        }
                        sendFile(w.image());
                        outStream.writeObject(sb.toString());
                    }
                }
            }

            // buy
            if (splitCommand[0].equals("buy") || splitCommand[0].equals("b")) {
                isValid = true;
                boolean contains = false;

                if (splitCommand.length != 4) {
                    outStream.writeObject("Erro!");
                    outStream.writeObject(
                            "Comando invalido! A operacao buy necessita de 3 argumentos <wine> <seller> <quantity>. \n");

                } else if (Integer.parseInt(splitCommand[2]) == ut.getUserID()) {
                    outStream.writeObject("Erro!\n");
                    outStream.writeObject("Comando invalido! <seller> nao pode ser o proprio! \n");

                } else {
                    for (Wine wine : TintolmarketServer.listaWines) {
                        if (wine.name().equals(splitCommand[1]) && !contains) {
                            contains = true;
                        }
                    }

                    if (!contains) {
                        outStream.writeObject("Erro!");
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

                                    if (sale.getWine().name().equals(splitCommand[1])) {
                                        notFound = false;

                                        if (sale.getQuantity() < Integer.parseInt(splitCommand[3])) {
                                            outStream.writeObject("Erro!");
                                            outStream.writeObject(
                                                    "Nao existe quantidade suficiente deste vinho para venda! \n");

                                        } else if (ut.getBalance() < (sale.getValue()
                                                * (Integer.parseInt(splitCommand[3])))) {
                                            outStream.writeObject("Erro!\n");
                                            outStream.writeObject("Saldo insuficiente! \n");

                                        } else {

                                            outStream.writeObject("Nova transacao!");

                                            Transacao tBuy = new Transacao(sale.getWine().name(), Integer.parseInt(splitCommand[3]),
                                                    sale.getValue(), ut.getUserID(), TransacaoType.BUY);

                                            String data = "user" + ut.getUserID() + " pos a venda vinho!";
                                            byte[] dataBytes = data.getBytes();

                                            outStream.writeObject(dataBytes);

                                            byte[] signedSellBytes = (byte[]) inStream.readObject();
                                            System.out.println("bytes da compra assinados recebidos");

                                            FileInputStream fis = new FileInputStream(CERTIFICATES_DIR + "user" + ut.getUserID() + "_certificate.cer");
                                            CertificateFactory cf = CertificateFactory.getInstance("X509");
                                            X509Certificate cert = (X509Certificate) cf.generateCertificate(fis);
                                            PublicKey clientPublicKey = cert.getPublicKey();

                                            Signature clientSignature = Signature.getInstance("SHA256withRSA");
                                            clientSignature.initVerify(clientPublicKey);

                                            clientSignature.update(data.getBytes());
                                            boolean verifiedSignature = clientSignature.verify(signedSellBytes);

                                            if (verifiedSignature) {

                                                Signature serverSignature = Signature.getInstance("SHA256withRSA");
                                                serverSignature.initSign(serverPrivateKey);
                                                blockchain.addTransacao(tBuy, serverSignature);

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
                                            }
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
                        if (wine.name().equals(splitCommand[1]) && !contains) {
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
                            outStream.writeObject("All good!");

                            byte[] encryptedMessage = (byte[]) inStream.readObject();
                            PublicKey receiverPublicKey = (PublicKey) inStream.readObject();
                            Key sharedKey = (Key) inStream.readObject();

                            updateChat(receiver.getUserID(), encryptedMessage, cipher, receiverPublicKey, sharedKey);

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
                    outStream.writeObject("All good!");

                    try {

                        String keyStorePassword = (String) inStream.readObject();
                        String keyAlias = (String) inStream.readObject();

                        // get the client's keystore
                        FileInputStream kfile2 = new FileInputStream(KEYSTORE_DIR + "user" + ut.getUserID() + "_keystore.jks");
                        KeyStore keyStore = KeyStore.getInstance("JCEKS");
                        keyStore.load(kfile2, keyStorePassword.toCharArray());

                        // get wrappedKey from chat_keys file
                        FileInputStream kfile3 = new FileInputStream(CHAT_KEYS_DIR + "user" + ut.getUserID() + "_chat_key.txt");
                        byte[] wrappedKey = kfile3.readAllBytes();
                        kfile3.close();

                        PrivateKey clientPrivateKey = (PrivateKey) keyStore.getKey(keyAlias, keyStorePassword.toCharArray());

                        Cipher c = Cipher.getInstance("RSA");
                        c.init(Cipher.UNWRAP_MODE, clientPrivateKey);
                        Key unwrappedKey = c.unwrap(wrappedKey, "DESede", Cipher.SECRET_KEY);

                        // cipher for decrypt
                        c = Cipher.getInstance("DESede");
                        c.init(Cipher.DECRYPT_MODE, unwrappedKey);

                        // convert all .cif files in chat directory to a list
                        File chatDIr = new File(CHAT_DIR);
                        File[] chatFiles = chatDIr.listFiles((dir, name) -> name.endsWith(".cif"));

                        boolean hasMessages = false;
                        String messages = null;

                        if (chatFiles != null) {
                            // Read each .cif file in chat directory
                            for (File chatFile : chatFiles) {
                                if (chatFile.getName().contains(Integer.toString(ut.getUserID()))) {
                                    hasMessages = true;

                                    CipherInputStream cis = new CipherInputStream(new FileInputStream(chatFile), c);

                                    byte[] bytesMessages = cis.readAllBytes();
                                    messages = new String(c.doFinal(bytesMessages));

                                    cis.close();

                                    // delete the .cif file after reading it
                                    chatFile.delete(); //TODO verufy if deletes
                                }
                            }

                            if (!hasMessages) {
                                outStream.writeObject("Nao tem mensagens por ler! \n");
                            } else {
                                outStream.writeObject(messages + "\n");
                            }
                        } else {
                            outStream.writeObject("Nao tem mensagens por ler! \n");
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
                    if (blockchain.getBlocks().get(0).getNrTransacoes() != 0) { //check the nrTransacoes do 1 bloco
                        for (Block block : blockchain.getBlocks()) {

                            if (block.getTransacoes() != null) {
                                for (Transacao transacao : block.getTransacoes()) {

                                    sb.append(transacao.toString() + "\n\n");
                                    sb.append("-------------------------------------- \n");
                                }
                            }
                        }
                        outStream.writeObject(sb.toString());
                    } else {
                        sb.append("Nenhuma transacao na blockchain! \n");
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

        private PublicKey getCertificatePublicKey(int receiverID) throws IOException, CertificateException {
            FileInputStream fis = new FileInputStream(CERTIFICATES_DIR + "user" + receiverID + "_certificate.cer");
            CertificateFactory cf = CertificateFactory.getInstance("X509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(fis);
            return cert.getPublicKey();
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