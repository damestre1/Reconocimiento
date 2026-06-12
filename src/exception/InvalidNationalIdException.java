package exception;

public class InvalidNationalIdException extends Exception {

    public InvalidNationalIdException(String message) {
        super(message);
    }

    public InvalidNationalIdException(String message, Throwable cause) {
        super(message, cause);
    }
}
