
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.util.Date;
import java.util.Scanner;

import javax.net.SocketFactory;
import javax.net.ssl.*;
import javax.security.cert.X509Certificate;

/*
 * @authors:
 *      Rodrigo Cabrito 54455
 *      João Costa 54482
 *      João Fraga 44837
 */

public class Tintolmarket {

    private static final String CLIENT_DIR = "./clientFiles/";
    private static final String CERTIFICATES_DIR = "./certificates/";

    public static void main(String[] args) {
        
        try {

            String trustStorePath = args[1];
            String keyStorePath = args[2];
            String keyStorePassword = args[3];
            String userID = args[4];

            String certificateAlias = "";
            String keyAlias = "";
            String keyPassword = "";

            // get the keystore from args
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(new FileInputStream(keyStorePath), keyStorePassword.toCharArray());

            // get the trustStore containing the server's trusted certificate from args
            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(new FileInputStream(trustStorePath), null);

            // get self certificate and keys
            Certificate cert = (Certificate) keyStore.getCertificate(certificateAlias);
            PublicKey publicKey = cert.getPublicKey();
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyPassword.toCharArray());

            // create certificate
            // TODO maybe remove this
            FileInputStream fis = new FileInputStream(CERTIFICATES_DIR + "user" + userID + ".cer");
            CertificateFactory cf = CertificateFactory.getInstance("X509");
            Certificate certificate = (Certificate) cf.generateCertificate(fis);

            // save the keystore to a file
            // TODO maybe remove this
            FileOutputStream fos = new FileOutputStream("ks" + userID + ".jks");
            keyStore.store(fos, keyStorePassword.toCharArray());
            fos.close();

            System.setProperty("javax.net.ssl.trustStore", trustStorePath);
            System.setProperty("javax.net.ssl.keyStore", keyStorePath);
            System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);

            SSLSocket clientSocketSSL = null;
            SocketFactory socketFactory = SSLSocketFactory.getDefault( );

            String[] ipPort = args[0].split(":");
            if (ipPort.length == 1) {
                clientSocketSSL = (SSLSocket) socketFactory.createSocket(args[0], 12345);
            } else {
                clientSocketSSL = (SSLSocket) socketFactory.createSocket(ipPort[0], Integer.parseInt(ipPort[1]));
            }

            SSLSession session = clientSocketSSL.getSession( );

            // verify the certificate from server
            if (session != null) {
                javax.security.cert.Certificate[] chain = session.getPeerCertificateChain();
                if (chain != null) {
                    // Retrieve and verify the end-entity certificate from the chain
                    javax.security.cert.Certificate peerCertificate = chain[0];
                    peerCertificate.verify(peerCertificate.getPublicKey());
                }
            }

            Scanner sc = new Scanner(System.in);

            //streams

            ObjectInputStream inStream = new ObjectInputStream(clientSocketSSL.getInputStream());
            ObjectOutputStream outStream = new ObjectOutputStream(clientSocketSSL.getOutputStream());

            //authentication
            outStream.writeObject(userID);

            boolean newUser = false;
            long nonce = 0;

            String answer = (String) inStream.readObject();
            String[] split = answer.split(":");

            if (split.length == 2) {
                nonce = Integer.parseInt(split[0]);
                newUser = true;
            } else {
                nonce = Integer.parseInt(split[0]);
            }

            // create signature from private key
            Signature signature = Signature.getInstance("MD5withRSA");
            signature.initSign(privateKey);

            if (newUser) {

                outStream.writeObject(nonce);
                outStream.writeObject(signature);
                outStream.writeObject(publicKey);

            } else {
                // signing of nonce
                signature.update(String.valueOf(nonce).getBytes());
                byte[] signedNonce = signature.sign();

                // send nonce signed with signature
                outStream.writeObject(signedNonce);
            }

            //answer authenticated/not authenticated
            System.out.println(inStream.readObject());

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

                } else {
                    outStream.writeObject(command); //send command
                }

                //print powering off
                if (command.equals("exit")) {
                    System.out.println("\n" + inStream.readObject());
                    sc.close();
                    clientSocketSSL.close();
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
