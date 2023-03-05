package Tintolmarket.domain;

/*
 * @authors:
 *      Rodrigo Cabrito 54455
 *      João Costa 54482
 *      João Fraga 44837
 */

public class Utilizador {
    
    private String id = null;
    private int wallet = 200;

    public Utilizador(String id) {
        this.id = id;
    }

    public void updateWallet(int change) {
        this.wallet += change;
    }

    public String getUserID() {
        return this.id;
    }

    public int getBalance() {
        return this.wallet;
    }
}
