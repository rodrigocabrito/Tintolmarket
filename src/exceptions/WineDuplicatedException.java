package Tintolmarket.exceptions;

/*
 * @authors:
 *      Rodrigo Cabrito 54455
 *      João Costa 54482
 *      João Fraga 44837
 */

public class WineDuplicatedException extends Exception {

    private static final long serialVersionUID = 5266277139245156833L;
    
    public WineDuplicatedException(String errorMessage) {
        super(errorMessage);
    }

}