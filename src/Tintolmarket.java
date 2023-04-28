import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Scanner;

/**
 * Tintolmarket class. Represents a client to access the TintolmarketServer server.
 * @author Rodrigo Cabrito 54455
 * @author João Costa 54482
 * @author João Fraga 44837
 */

public class Tintolmarket {

    private static final String CLIENT_DIR = "./client_files/";
    private static final String KEYSTORE_DIR = "./keystores/";
    private static final String CERTIFICATE_DIR = "./certificates/";

    private static PrivateKey privateKey = null;
    private static KeyStore trustStore = null;
    private static java.security.cert.Certificate cert = null;

    public static void main(String[] args) {
        
        try {

            String trustStoreFileName = args[1];
            String keyStoreFileName = args[2];
            String keyStorePassword = args[3];
            int userID = Integer.parseInt(args[4]);

            String keyAlias = "user" + userID + "_key_alias";
            String defaultPasswordTrustStore = "changeit";

            String keyStorePath = KEYSTORE_DIR + keyStoreFileName;
            String trustStorePath = KEYSTORE_DIR + trustStoreFileName;

            // get the trustStore containing the server's trusted certificate from args
            trustStore = KeyStore.getInstance("JCEKS");
            trustStore.load(new FileInputStream(trustStorePath), defaultPasswordTrustStore.toCharArray());

            // get the keystore from args
            KeyStore keyStore = KeyStore.getInstance("JCEKS");
            keyStore.load(new FileInputStream(keyStorePath), keyStorePassword.toCharArray());

            // Create a KeyManagerFactory to extract the client's private key and certificate from the keystore
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            // Create an SSL context and configure it to use the key and trust managers
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());

            // get self certificate and keys
            cert = keyStore.getCertificate(keyAlias);
            privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyStorePassword.toCharArray());

            System.setProperty("javax.net.ssl.keyStore", keyStorePath);
            System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);
            System.setProperty("javax.net.ssl.trustStore", trustStorePath);
            System.setProperty("javax.net.ssl.trustStorePassword", defaultPasswordTrustStore);

            // Create an SSL socket factory and set it as the default socket factory
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            SSLSocket socket = null;

            String host = "localhost";
            socket = (SSLSocket) sslSocketFactory.createSocket(host, 12345);

            socket.startHandshake();

            Scanner sc = new Scanner(System.in);
            //streams

            ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());

            //authentication
            outStream.writeObject(userID);

            boolean newUser = false;
            long nonce = 0;

            String answer = (String) inStream.readObject();
            String[] split = answer.split(":"); //split flag
            nonce = Long.parseLong(split[0]);

            if (split.length == 2) {
                newUser = true;
            }

            if (newUser) {

                // adicionar certificate do client a trustStore
                trustStore.setCertificateEntry("user" + userID + "_cert", cert);

                //send nonce
                outStream.writeObject(nonce);

                // Generate the signature to sign the data
                byte[] data = "Hello, server!".getBytes();
                Signature signature = Signature.getInstance("SHA256withRSA");
                signature.initSign(privateKey);
                signature.update(data);
                byte[] signatureBytes = signature.sign();
                outStream.writeObject(signatureBytes);

                //send certificate
                outStream.writeObject(cert);

            } else {
                // signing of nonce
                Signature signature = Signature.getInstance("SHA256withRSA");
                signature.initSign(privateKey);
                signature.update(String.valueOf(nonce).getBytes());
                byte[] signedNonce = signature.sign();

                // send nonce signed with signature
                outStream.writeObject(signedNonce);
            }

            //answer authenticated/not authenticated
            String authAnswer = (String) inStream.readObject();
            System.out.println(authAnswer);

            // exit if authentication failed
            if (authAnswer .equals("Erro na autenticacao...")) {
                sc.close();
                socket.close();
                System.exit(-1);
            }

            while(true) {

                //print menu
                System.out.print(inStream.readObject());

                // get command from system in
                String command = sc.nextLine();

                String[] splitCommand = command.split(" ");

                if (splitCommand[0].equals("add") || splitCommand[0].equals("a")) {

                    outStream.writeObject(command); //send command

                    BufferedReader br = new BufferedReader(new FileReader("./data_bases/wines.txt"));
                    String line;
                    boolean exists = false;

                    //check if wine to be added exists in wine.txt
                    while ((line = br.readLine()) != null && !exists) {
                        if (line.contains(splitCommand[1])) {
                            exists = true;
                        }
                    }
                    br.close();

                    //only send image file if wine doesn't exist
                    if(!exists) {

                        File fileToSend = new File(CLIENT_DIR + splitCommand[2]);
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
                } else if (splitCommand[0].equals("view") || splitCommand[0].equals("v")) {

                    outStream.writeObject(command); //send command

                    int fileSize;
                    int bytesRead;
                    byte[] buffer = new byte[1024];

                    BufferedReader br = new BufferedReader(new FileReader("./data_bases/wines.txt"));
                    String line;
                    String lineForSplit = null;
                    boolean exists = false;

                    //check if wine to be shown exists in wine.txt
                    while ((line = br.readLine()) != null && !exists) {
                        if (line.contains(splitCommand[1])) {
                            exists = true;
                            lineForSplit = line;
                        }
                    }
                    br.close();

                    if (exists) {
                        String[] splitLine = lineForSplit.split(";");//wineName;wineImage

                        File f = new File(CLIENT_DIR +  splitLine[1]);
                        FileOutputStream fileOutputStream = new FileOutputStream(f);
                        OutputStream outputFile = new BufferedOutputStream(fileOutputStream);

                        try {
                            fileSize = (int) inStream.readObject();
                            int totalsize = fileSize;

                            //receive file
                            while (totalsize > 0 ) {
                                if(totalsize >= 1024) {
                                    bytesRead = inStream.read(buffer,0,1024);
                                    System.out.println(bytesRead);
                                } else {
                                    bytesRead = inStream.read(buffer,0,totalsize);
                                    System.out.println(bytesRead);
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
                } else if (splitCommand[0].equals("talk") || splitCommand[0].equals("t")) {
                    outStream.writeObject(command); //send command

                    if (inStream.readObject().equals("All good!")) {

                        // get the public key from the client to send msg (trustStore)
                        FileInputStream fis = new FileInputStream(CERTIFICATE_DIR + "user" + userID + "_certificate.cert");
                        CertificateFactory cf = CertificateFactory.getInstance("X509");
                        X509Certificate cert = (X509Certificate) cf.generateCertificate(fis);
                        PublicKey receiverPublicKey = cert.getPublicKey();

                        System.out.print("> ");
                        String txt = sc.nextLine();

                        String message = "Mensagem de user" + userID + ": " + txt + "\n";

                        KeyGenerator kg = KeyGenerator.getInstance("DESede");
                        Key sharedKey = kg.generateKey();

                        Cipher cipher = Cipher.getInstance("DESede");
                        cipher.init(Cipher.ENCRYPT_MODE, sharedKey);

                        byte[] encryptedMessage = cipher.doFinal(message.getBytes());

                        outStream.writeObject(encryptedMessage);    //send encrypted msg
                        outStream.writeObject(receiverPublicKey);   // send public key
                        outStream.writeObject(sharedKey);           // send shared key
                    }


                } else if (splitCommand[0].equals("sell") || splitCommand[0].equals("s") ||
                                    splitCommand[0].equals("buy") || splitCommand[0].equals("b")){
                    outStream.writeObject(command); //send command

                    Signature signature = Signature.getInstance("SHA256withRSA");
                    signature.initSign(privateKey);

                    String answer2 = (String) inStream.readObject();

                   if (answer2.equals("Nova transacao!") || answer2.equals("Venda para dar update!")) {
                       byte[] transactionBytes = (byte[]) inStream.readObject();
                       signature.update(transactionBytes);
                       byte[] signedSellBytes = signature.sign();

                       outStream.writeObject(signedSellBytes);
                   }

                } else if (splitCommand[0].equals("read") || splitCommand[0].equals("r")){

                    outStream.writeObject(command); //send command

                    if (inStream.readObject().equals("All good!")) {

                        outStream.writeObject(keyStorePassword);    //send keystore password
                        outStream.writeObject(keyAlias);            //send key alias
                    }
                } else {
                    outStream.writeObject(command); //send command
                }

                //print powering off
                if (command.equals("exit")) {
                    System.out.println("\n" + inStream.readObject());
                    sc.close();
                    socket.close();
                    System.exit(0);
                } else {
                    //print answer
                    System.out.println("\n" + inStream.readObject());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
