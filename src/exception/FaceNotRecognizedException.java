package exception;

public class FaceNotRecognizedException extends Exception {

    public FaceNotRecognizedException(String message) {
        super(message);
    }

    public FaceNotRecognizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
