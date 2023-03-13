package exceptions;

/*
 * @authors:
 *      Rodrigo Cabrito 54455
 *      João Costa 54482
 *      João Fraga 44837
 */

public class WineNotFoundException extends Exception {

    private static final long serialVersionUID = 8276440707687146820L;
    
    public WineNotFoundException(String errorMessage) {
        super(errorMessage);
    }

}