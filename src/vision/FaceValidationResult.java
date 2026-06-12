package vision;

import org.opencv.core.Rect;

public class FaceValidationResult {

    private final boolean valid;
    private final String message;
    private final Rect face;

    private FaceValidationResult(boolean valid, String message, Rect face) {
        this.valid = valid;
        this.message = message;
        this.face = face;
    }

    public static FaceValidationResult ok(Rect face) {
        return new FaceValidationResult(
                true, "Rostro válido. Presione S para capturar.", face);
    }

    public static FaceValidationResult error(String message) {
        return new FaceValidationResult(false, message, null);
    }

    public boolean isValid() {
        return valid;
    }

    public String getMessage() {
        return message;
    }

    public Rect getFace() {
        return face;
    }
}
