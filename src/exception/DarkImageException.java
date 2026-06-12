package exception;

public class DarkImageException extends Exception {

    public DarkImageException(String message) {
        super(message);
    }

    public DarkImageException(String message, Throwable cause) {
        super(message, cause);
    }
}
