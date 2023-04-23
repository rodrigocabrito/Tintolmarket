import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.util.List;

public class Block {

    private final long id;
    private long nrTransacoes;
    private List<Transacao> transacoes;
    private Signature assinatura;
    private String hash;


    public Block(long id) {
        this.id = id;
        this.nrTransacoes = 0;

        if (id == 1) {
            this.hash = "0000000000000000000";
        }
    }

    public String calculateHash() {
        StringBuilder sb = new StringBuilder();
        for (Transacao transacao : transacoes) {
            sb.append(transacao.toString());
        }

        String dataToHash = sb.toString() + assinatura.toString();
        String hash = null;

        //TODO verificar metdo de fazer calculateHash
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
        this.hash = calculateHash();
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

    public void setNrTransacoes(long nrTransacoes) {
        this.nrTransacoes = nrTransacoes;
    }

    public List<Transacao> getTransacoes() {
        return transacoes;
    }

    public void setTransacoes(List<Transacao> transacoes) {
        this.transacoes = transacoes;
    }

    public Signature getAssinatura() {
        return assinatura;
    }

    public void setAssinatura(Signature assinatura) {
        this.assinatura = assinatura;
    }
}
