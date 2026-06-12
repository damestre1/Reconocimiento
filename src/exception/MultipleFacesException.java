package exception;

public class MultipleFacesException extends Exception {

    public MultipleFacesException(String message) {
        super(message);
    }

    public MultipleFacesException(String message, Throwable cause) {
        super(message, cause);
    }
}
