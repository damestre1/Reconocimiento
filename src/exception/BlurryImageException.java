package exception;

public class BlurryImageException extends Exception {

    public BlurryImageException(String message) {
        super(message);
    }

    public BlurryImageException(String message, Throwable cause) {
        super(message, cause);
    }
}
