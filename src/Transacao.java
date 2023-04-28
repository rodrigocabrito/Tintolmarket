import java.security.SignedObject;

public class Transacao {

    private final String vinhoName;
    private int quantity;
    private int value;
    private final int userId;
    private final TransacaoType type;
    private SignedObject assinatura;

    public Transacao(String vinhoName, int quantity, int value, int userId, TransacaoType type) {
        this.vinhoName = vinhoName;
        this.quantity = quantity;
        this.value = value;
        this.userId = userId;
        this.type = type;
    }

    @Override
    public String toString() {
        return " Transacao: " + type +
                "\n id do vinho: " + vinhoName +
                "\n unidades: " + quantity +
                "\n valor: " + value +
                "\n user: " + userId;
    }

    public void updateQuantity(int quantity) {
        this.quantity += quantity;
    }

    public void updateValue(int value) {
        this.value = value;
    }

    public TransacaoType getType() {
        return this.type;
    }

    public String getVinhoName() {
        return this.vinhoName;
    }
}
