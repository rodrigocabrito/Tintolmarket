import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
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

    public Block(String hash, long id, long nrTransacoes, List<Transacao> transacoes, Signature assinatura) {
        this.hash = hash;
        this.id = id;
        this.nrTransacoes = nrTransacoes;
        this.transacoes = transacoes;
        this.assinatura = assinatura;
    }

    public String calculateHash() {
        StringBuilder sb = new StringBuilder();
        for (Transacao transacao : transacoes) {
            sb.append(transacao.toStringBlkFile());
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

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        ObjectOutputStream oos = new ObjectOutputStream(bos);

        try {
            // Serialize the block data to the byte array
            dos.writeUTF(hash);
            dos.writeLong(id);
            dos.writeLong(nrTransacoes);
            for (Transacao transacao : transacoes) {
                dos.writeUTF(transacao.toStringBlkFile());
            }
            if (assinatura != null) {
                oos.writeObject(assinatura);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return bos.toByteArray();
    }

    public static Block fromByteArray(byte[] data, PrivateKey serverPrivateKey) throws IOException, InvalidKeyException, NoSuchAlgorithmException {

        // Create an input stream to read the block data
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bis);
        ObjectInputStream ois = new ObjectInputStream(bis);

        // Read the block data from the input stream and create a Block object
        String hash = dis.readUTF();
        long id = dis.readLong();
        long nrTransacoes = dis.readLong();
        List<Transacao> transacoes = new ArrayList<>();
        for (int i = 0; i < nrTransacoes; i++) {
            String transacaoString = dis.readUTF();
            transacoes.add(Transacao.fromString(transacaoString));
        }

        //Signature serverSignature = Signature.getInstance("SHA256withRSA");
        //serverSignature.initSign(serverPrivateKey);
        Signature serverSignature = null;
        if (nrTransacoes == 5) {
            Signature assinatura = null;

            serverSignature = Signature.getInstance("SHA256withRSA");
            serverSignature.initSign(serverPrivateKey);
            try {
                assinatura = (Signature) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                // The signature is not present in the block data
            }
        }

        /*
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bis);

        String hash = dis.readUTF();
        long id = dis.readLong();
        long nrTransacoes = dis.readLong();
        List<Transacao> transacoes = new ArrayList<>();
        int i = 0;
        while (i < nrTransacoes) {
            String transacaoString = dis.readUTF();
            Transacao transacao = Transacao.fromString(transacaoString);
            transacoes.add(transacao);
            i++;
        }

        Signature serverSignature = Signature.getInstance("SHA256withRSA");
        serverSignature.initSign(serverPrivateKey);
        */
        return new Block(hash, id, nrTransacoes, transacoes, serverSignature);
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
