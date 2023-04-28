import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Block {

    private String hash;
    private final long id;
    private long nrTransacoes;
    private final List<Transacao> transacoes;
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

    public String calculateHash() {
        StringBuilder sb = new StringBuilder();
        for (Transacao transacao : transacoes) {
            sb.append(transacao.toString());
        }

        String dataToHash = sb + assinatura.toString();
        String hash = null;

        //TODO verificar metodo de fazer calculateHash
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

    public void addTransacao(Transacao transacao) {
        this.transacoes.add(transacao);
        this.nrTransacoes++;
    }

    public void closeBlock(Signature assinatura) {
        this.assinatura = assinatura;
    }

    public byte[] toByteArray() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        try {
            // Serialize the block data to the byte array
            dos.writeUTF(hash);
            dos.writeLong(id);
            dos.writeLong(nrTransacoes);
            for (Transacao transacao : transacoes) {
                dos.writeUTF(transacao.toString());
            }
            if (assinatura != null) {
                dos.writeUTF(assinatura.toString());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return bos.toByteArray();
    }

    public static Block fromByteArray(byte[] data) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(in);
        Block block = null;
        try {
            block = (Block) ois.readObject();
        } catch (ClassNotFoundException e) {
            // Handle the exception
        }
        ois.close();
        return block;
    }

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
}
