package domain;

/*
 * @authors:
 *      Rodrigo Cabrito 54455
 *      João Costa 54482
 *      João Fraga 44837
 */

public class Utilizador {
    
    private String id = null;
    private int balance = 0;
    
    public Utilizador(String id, int balance) {
        this.id = id;
        this.balance = balance;
    }

    public void updateWallet(int change) {
        this.balance += change;
    }

    public String getUserID() {
        return this.id;
    }

    public int getBalance() {
        return this.balance;
    }
}
