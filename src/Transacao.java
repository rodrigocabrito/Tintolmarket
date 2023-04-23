public class Transacao {

    private String vinhoName;
    private int nrUnidades;
    private int valor;
    private String userId;

    public Transacao(String vinhoName, int nrUnidades, int valor, String userId) {
        this.vinhoName = vinhoName;
        this.nrUnidades = nrUnidades;
        this.valor = valor;
        this.userId = userId;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
