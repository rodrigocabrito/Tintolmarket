public class Transacao {

    private String vinhoName;
    private int nrUnidades;
    private int valor;
    private int userId;
    private final TransacaoType type;

    public Transacao(String vinhoName, int nrUnidades, int valor, int userId, TransacaoType type) {
        this.vinhoName = vinhoName;
        this.nrUnidades = nrUnidades;
        this.valor = valor;
        this.userId = userId;
        this.type = type;
    }

    public String getVinhoName() {
        return vinhoName;
    }

    public void setVinhoName(String vinhoName) {
        this.vinhoName = vinhoName;
    }

    public int getNrUnidades() {
        return nrUnidades;
    }

    public void setNrUnidades(int nrUnidades) {
        this.nrUnidades = nrUnidades;
    }

    public int getValor() {
        return valor;
    }

    public void setValor(int valor) {
        this.valor = valor;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return " Transacao: " + type +
                "\n id do vinho: " + vinhoName +
                "\n unidades: " + nrUnidades +
                "\n valor: " + valor +
                "\n user: " + userId;
    }
}
