package exception;

public class FaceNotDetectedException extends Exception {

    public FaceNotDetectedException(String message) {
        super(message);
    }

    public FaceNotDetectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
