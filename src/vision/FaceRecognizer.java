package vision;

import exception.FaceNotDetectedException;
import exception.FaceNotRecognizedException;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import util.Constants;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reconocedor facial por correlación normalizada de píxeles
 * (matchTemplate TM_CCOEFF_NORMED) sobre rostros normalizados.
 * No requiere el módulo contrib de OpenCV.
 */
public class FaceRecognizer {

    private final FaceDetector faceDetector;
    private final ImageProcessor imageProcessor;
    private final Map<String, Mat> gallery = new ConcurrentHashMap<>();

    public FaceRecognizer(FaceDetector faceDetector,
                          ImageProcessor imageProcessor) {
        this.faceDetector = faceDetector;
        this.imageProcessor = imageProcessor;
    }

    public void enroll(String nationalId, Mat faceImage)
            throws FaceNotDetectedException {
        gallery.put(nationalId, extractNormalizedFace(faceImage));
    }

    public void enrollFromFile(String nationalId, String imagePath)
            throws FaceNotDetectedException {
        Mat image = imageProcessor.load(imagePath);
        if (image.empty()) {
            throw new FaceNotDetectedException(
                    "No se pudo leer la imagen: " + imagePath);
        }
        enroll(nationalId, image);
        image.release();
    }

    public int enrollFromDirectory(String directory) {
        File dir = new File(directory);
        File[] files = dir.listFiles(
                (d, name) -> name.endsWith(Constants.FACE_SUFFIX));

        if (files == null) {
            return 0;
        }

        int enrolled = 0;
        for (File file : files) {
            String nationalId = file.getName()
                    .replace(Constants.FACE_SUFFIX, "");
            try {
                enrollFromFile(nationalId, file.getAbsolutePath());
                enrolled++;
            } catch (FaceNotDetectedException ignored) {
                // Imagen sin rostro detectable: se omite.
            }
        }
        return enrolled;
    }

    public boolean isEnrolled(String nationalId) {
        return gallery.containsKey(nationalId);
    }


    /**
     * Igual que recognize pero retorna null si no hay coincidencia,
     * en lugar de lanzar excepción. Útil para detectar duplicados.
     */
    public RecognitionMatch tryRecognize(Mat probeImage)
            throws FaceNotDetectedException {
        try {
            return recognize(probeImage);
        } catch (FaceNotRecognizedException e) {
            return null;
        }
    }

    public void remove(String nationalId) {
        Mat removed = gallery.remove(nationalId);
        if (removed != null) {
            removed.release();
        }
    }

    public RecognitionMatch recognize(Mat probeImage)
            throws FaceNotDetectedException, FaceNotRecognizedException {

        Mat probe = extractNormalizedFace(probeImage);

        String bestId = null;
        double bestScore = -1.0;

        for (Map.Entry<String, Mat> entry : gallery.entrySet()) {
            double score = similarity(probe, entry.getValue());
            if (score > bestScore) {
                bestScore = score;
                bestId = entry.getKey();
            }
        }

        probe.release();

        System.out.printf(
                "[FaceRecognizer] mejor coincidencia: %s (%.2f, umbral %.2f)%n",
                bestId, bestScore, Constants.RECOGNITION_THRESHOLD);

        if (bestId == null
                || bestScore < Constants.RECOGNITION_THRESHOLD) {
            throw new FaceNotRecognizedException(
                    "El rostro no coincide con ningún usuario registrado.");
        }

        return new RecognitionMatch(bestId, bestScore);
    }

    public boolean verify(String nationalId, Mat probeImage)
            throws FaceNotDetectedException {

        Mat reference = gallery.get(nationalId);
        if (reference == null) {
            return false;
        }

        Mat probe = extractNormalizedFace(probeImage);
        double score = similarity(probe, reference);
        probe.release();

        System.out.printf(
                "[FaceRecognizer] verificacion %s: %.2f (umbral %.2f)%n",
                nationalId, score, Constants.RECOGNITION_THRESHOLD);

        return score >= Constants.RECOGNITION_THRESHOLD;
    }

    /**
     * Correlación normalizada de píxeles entre dos imágenes del
     * mismo tamaño. Devuelve un valor entre -1 y 1.
     */
    private double similarity(Mat a, Mat b) {
        Mat result = new Mat();
        Imgproc.matchTemplate(a, b, result,
                Imgproc.TM_CCOEFF_NORMED);
        double score = Core.minMaxLoc(result).maxVal;
        result.release();
        return score;
    }

    private Mat extractNormalizedFace(Mat image)
            throws FaceNotDetectedException {

        Rect[] faces = faceDetector.detectFaces(image);
        if (faces.length == 0) {
            throw new FaceNotDetectedException(
                    "No se detectó rostro en la imagen.");
        }

        Rect largest = faces[0];
        for (Rect face : faces) {
            if (face.area() > largest.area()) {
                largest = face;
            }
        }

        Mat region = new Mat(image, largest);
        Mat normalized = imageProcessor.normalizeFace(region);
        region.release();
        return normalized;
    }

    public static class RecognitionMatch {

        private final String nationalId;
        private final double confidence;

        public RecognitionMatch(String nationalId, double confidence) {
            this.nationalId = nationalId;
            this.confidence = confidence;
        }

        public String getNationalId() {
            return nationalId;
        }

        public double getConfidence() {
            return confidence;
        }
    }
}