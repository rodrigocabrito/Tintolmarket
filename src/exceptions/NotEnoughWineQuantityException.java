package exceptions;

/*
 * @authors:
 *      Rodrigo Cabrito 54455
 *      João Costa 54482
 *      João Fraga 44837
 */

public class NotEnoughWineQuantityException extends Exception{
    
    private static final long serialVersionUID = -4153474037436591729L;

    public NotEnoughWineQuantityException(String errorMessage) {
        super(errorMessage);
    }
}
