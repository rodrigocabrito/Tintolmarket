package Tintolmarket.exceptions;

/*
 * @authors:
 *      Rodrigo Cabrito 54455
 *      João Costa 54482
 *      João Fraga 44837
 */

public class NotEnoughBudgetException extends Exception{
    
    private static final long serialVersionUID = 8533124557768260475L;

    public NotEnoughBudgetException(String errorMessage) {
        super(errorMessage);
    }
}
