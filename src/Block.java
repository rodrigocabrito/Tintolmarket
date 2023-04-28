import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Block class. Represents a block in the blockchain.
 * @author Rodrigo Cabrito 54455
 * @author João Costa 54482
 * @author João Fraga 44837
 */

public class Block {

    private String hash;
    private final long id;
    private long nrTransacoes;
    private final List<Transacao> transacoes;
    private byte[] bytesAssinatura;
    private Signature assinatura;


    public Block(long id) {
        this.id = id;
        this.nrTransacoes = 0;
        this.transacoes = new ArrayList<>();

        if (id == 1) {
            this.hash = Arrays.toString(new byte[32]);
        } else {
            this.hash = null;
        }
    }

    public Block(String hash, long id, long nrTransacoes, List<Transacao> transacoes, Signature assinatura) {
        this.hash = hash;
        this.id = id;
        this.nrTransacoes = nrTransacoes;
        this.transacoes = transacoes;
        this.assinatura = assinatura;
    }

    /**
     * Adds a transaction to the block.
     * @param transacao given transaction to add
     */
    public void addTransacao(Transacao transacao) {
        this.transacoes.add(transacao);
        this.nrTransacoes++;
    }

    /**
     * Closes the block, storing the block's data, signed by the server.
     * @param bytesAssinatura given block's data signed by the server
     * @throws SignatureException if an error occurs while signing the block.
     */
    public void closeBlock(byte[] bytesAssinatura) throws SignatureException {
        this.bytesAssinatura = bytesAssinatura;
    }

    /**
     * Calculates the hash of the block.
     * @return a String containing the block's hash.
     */
    public String calculateHash() {
        StringBuilder sb = new StringBuilder();
        sb.append(hash);
        sb.append(id);
        sb.append(nrTransacoes);
        for (Transacao transacao : transacoes) {
            sb.append(transacao.toString());
        }

        String dataToHash = sb.toString();
        String hash = null;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(dataToHash.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            hash = builder.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hash;
    }

    /**
     * Converts a block to an array of bytes.
     * @return the bytes representing the block.
     * @throws IOException if an error occurs while converting the block.
     */
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        try {
            // Serialize the block data to the byte array
            dos.writeUTF(hash);
            dos.writeLong(id);
            dos.writeLong(nrTransacoes);
            for (Transacao transacao : transacoes) {
                dos.writeUTF(transacao.toStringBlkFile());
            }
            if (bytesAssinatura != null) {
                dos.write(bytesAssinatura);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return bos.toByteArray();
    }

    /**
     * Converts an array of bytes to a block.
     * @return the block represented by the bytes.
     * @throws IOException if an error occurs while converting the array of bytes.
     */
    public static Block fromByteArray(byte[] data, PrivateKey serverPrivateKey) throws IOException, InvalidKeyException, NoSuchAlgorithmException, SignatureException {

        // Create an input stream to read the block data
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bis);

        // Read the block data from the input stream and create a Block object
        String hash = dis.readUTF();
        long id = dis.readLong();
        long nrTransacoes = dis.readLong();
        List<Transacao> transacoes = new ArrayList<>();
        for (int i = 0; i < nrTransacoes; i++) {
            String transacaoString = dis.readUTF();
            transacoes.add(Transacao.fromString(transacaoString));
        }

        Signature serverSignature = null;

        if (nrTransacoes == 5) {

            serverSignature = Signature.getInstance("SHA256withRSA");
            serverSignature.initSign(serverPrivateKey);
        }
        return new Block(hash, id, nrTransacoes, transacoes, serverSignature);
    }

    /**
     * Checks if the block is full.
     * @return {@code true} if the block is full, {@code false} otherwise.
     */
    public boolean isBlockFull() {
        return this.nrTransacoes == 5;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getHash() {
        return hash;
    }

    public long getId() {
        return id;
    }

    public long getNrTransacoes() {
        return nrTransacoes;
    }

    public List<Transacao> getTransacoes() {
        return transacoes;
    }

    public byte[] getSignatureBytes() {
        return bytesAssinatura;
    }

    public void setSignatureBytes(byte[] bytesAssinatura) {
        this.bytesAssinatura = bytesAssinatura;
    }

    public Signature getSignature() {
        return assinatura;
    }
}
