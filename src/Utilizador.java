

/*
 * @authors:
 *      Rodrigo Cabrito 54455
 *      João Costa 54482
 *      João Fraga 44837
 */

public class Utilizador {
    
    private final int id;
    private int balance;
    
    public Utilizador(int id, int balance) {
        this.id = id;
        this.balance = balance;
    }

    public void updateWallet(int change) {
        this.balance += change;
    }

    public int getUserID() {
        return this.id;
    }

    public int getBalance() {
        return this.balance;
    }
}
