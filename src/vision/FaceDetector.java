package vision;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import util.Constants;

public class FaceDetector {

    private final CascadeClassifier faceClassifier;
    private final CascadeClassifier eyesClassifier;
    private final ImageProcessor imageProcessor;

    public FaceDetector() {
        this(Constants.FACE_CASCADE_PATH, Constants.EYES_CASCADE_PATH);
    }

    public FaceDetector(String faceCascadePath, String eyesCascadePath) {
        this.faceClassifier = new CascadeClassifier(faceCascadePath);
        if (faceClassifier.empty()) {
            throw new IllegalStateException(
                    "No se pudo cargar el clasificador de rostros: "
                            + faceCascadePath);
        }

        this.eyesClassifier = new CascadeClassifier(eyesCascadePath);
        if (eyesClassifier.empty()) {
            throw new IllegalStateException(
                    "No se pudo cargar el clasificador de ojos: "
                            + eyesCascadePath);
        }

        this.imageProcessor = new ImageProcessor();
    }

    public Rect[] detectFaces(Mat frame) {
        Mat gray = imageProcessor.toGray(frame);
        Imgproc.equalizeHist(gray, gray);

        MatOfRect faces = new MatOfRect();
        faceClassifier.detectMultiScale(
                gray,
                faces,
                1.05,
                3,
                0,
                new Size(Constants.MIN_FACE_SIZE_PX, Constants.MIN_FACE_SIZE_PX),
                new Size()
        );

        gray.release();

        Rect[] result = faces.toArray();
        faces.release();
        return result;
    }

    public Rect[] detectEyes(Mat frame, Rect face) {
        Mat faceRegion = new Mat(frame, face);
        Mat gray = imageProcessor.toGray(faceRegion);
        Imgproc.equalizeHist(gray, gray);

        MatOfRect eyes = new MatOfRect();
        eyesClassifier.detectMultiScale(
                gray,
                eyes,
                1.1,
                3,
                0,
                new Size(18, 18),
                new Size()
        );

        gray.release();
        faceRegion.release();

        Rect[] result = eyes.toArray();
        eyes.release();
        return result;
    }

    /**
     * Basta con detectar UN ojo: el detector de ojos parpadea mucho
     * y exigir dos hacía la captura demasiado lenta.
     */
    public boolean eyesOpen(Mat frame, Rect face) {
        return detectEyes(frame, face).length >= 1;
    }

    public double tiltDegrees(Mat frame, Rect face) {
        Rect[] eyes = detectEyes(frame, face);
        if (eyes.length < 2) {
            return 0.0;
        }

        Rect left = eyes[0].x < eyes[1].x ? eyes[0] : eyes[1];
        Rect right = eyes[0].x < eyes[1].x ? eyes[1] : eyes[0];

        double leftCx = left.x + left.width / 2.0;
        double leftCy = left.y + left.height / 2.0;
        double rightCx = right.x + right.width / 2.0;
        double rightCy = right.y + right.height / 2.0;

        double deltaY = rightCy - leftCy;
        double deltaX = rightCx - leftCx;

        if (deltaX == 0) {
            return 90.0;
        }

        return Math.abs(Math.toDegrees(Math.atan2(deltaY, deltaX)));
    }

    public boolean isPartiallyVisible(Mat frame, Rect face) {
        int margin = Constants.EDGE_MARGIN_PX;
        return face.x <= margin
                || face.y <= margin
                || face.x + face.width >= frame.cols() - margin
                || face.y + face.height >= frame.rows() - margin;
    }

    public boolean isDistanceCorrect(Mat frame, Rect face) {
        double ratio = (double) face.width / frame.cols();
        return ratio >= Constants.MIN_FACE_RATIO
                && ratio <= Constants.MAX_FACE_RATIO;
    }

    public FaceValidationResult validateForCapture(Mat frame) {

        if (imageProcessor.isDark(frame)) {
            return FaceValidationResult.error(
                    "Iluminación insuficiente. Busque un lugar con más luz.");
        }

        Rect[] faces = detectFaces(frame);

        if (faces.length == 0) {
            return FaceValidationResult.error(
                    "No se detecta ningún rostro.");
        }

        if (faces.length > 1) {
            return FaceValidationResult.error(
                    "Se detectaron múltiples rostros. Debe haber solo una persona.");
        }

        Rect face = faces[0];

        if (isPartiallyVisible(frame, face)) {
            return FaceValidationResult.error(
                    "Rostro parcialmente visible. Centre su rostro en el recuadro.");
        }

        double ratio = (double) face.width / frame.cols();
        if (ratio < Constants.MIN_FACE_RATIO) {
            return FaceValidationResult.error(
                    "Está muy lejos de la cámara. Acérquese.");
        }
        if (ratio > Constants.MAX_FACE_RATIO) {
            return FaceValidationResult.error(
                    "Está muy cerca de la cámara. Aléjese.");
        }

        if (!eyesOpen(frame, face)) {
            return FaceValidationResult.error(
                    "Mantenga los ojos abiertos y mire a la cámara.");
        }

        if (tiltDegrees(frame, face) > Constants.MAX_TILT_DEGREES) {
            return FaceValidationResult.error(
                    "Inclinación excesiva. Mantenga la cabeza recta.");
        }

        return FaceValidationResult.ok(face);
    }
}