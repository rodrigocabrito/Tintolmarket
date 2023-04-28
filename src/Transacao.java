import java.security.SignedObject;

public class Transacao {

    private final String vinhoName;
    private int quantity;
    private int value;
    private final int userId;
    private final TransacaoType type;

    public Transacao(String vinhoName, int quantity, int value, int userId, TransacaoType type) {
        this.vinhoName = vinhoName;
        this.quantity = quantity;
        this.value = value;
        this.userId = userId;
        this.type = type;
    }

    public static Transacao fromString(String str) {
        String[] split = str.split(",");

        TransacaoType type = TransacaoType.valueOf(split[0]);
        String name = split[1];
        int quantity = Integer.parseInt(split[2]);
        int value = Integer.parseInt(split[3]);
        int userID = Integer.parseInt(split[4]);

        return new Transacao(name, quantity, value, userID, type);
    }

    public String toStringBlkFile() {
        return type + "," + vinhoName + "," + quantity + "," + value + "," + userId;
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
