package exceptions;

/*
 * @authors:
 *      Rodrigo Cabrito 54455
 *      João Costa 54482
 *      João Fraga 44837
 */

public class UtilizadorNotFoundException extends Exception{
    
    private static final long serialVersionUID = -9104023789880842897L;

    public UtilizadorNotFoundException(String errorMessage) {
        super(errorMessage);
    }
}
