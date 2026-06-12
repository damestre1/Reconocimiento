package vision;

import exception.InvalidDocumentException;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import util.Constants;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Identifica a qué usuario pertenece una cédula comparando la cara
 * frontal capturada contra las guardadas en photos/cedula_front.jpg,
 * usando correlación normalizada de píxeles.
 */
public class DocumentRecognizer {

    private final ImageProcessor imageProcessor;
    private final Map<String, Mat> gallery = new ConcurrentHashMap<>();

    public DocumentRecognizer(ImageProcessor imageProcessor) {
        this.imageProcessor = imageProcessor;
    }

    public void enroll(String nationalId, Mat frontImage) {
        gallery.put(nationalId, normalize(frontImage));
    }

    public void enrollFromFile(String nationalId, String imagePath) {
        Mat image = imageProcessor.load(imagePath);
        if (!image.empty()) {
            enroll(nationalId, image);
        }
        image.release();
    }

    public int enrollFromDirectory(String directory) {
        File dir = new File(directory);
        File[] files = dir.listFiles(
                (d, name) -> name.endsWith(Constants.FRONT_SUFFIX));

        if (files == null) {
            return 0;
        }

        int enrolled = 0;
        for (File file : files) {
            String nationalId = file.getName()
                    .replace(Constants.FRONT_SUFFIX, "");
            enrollFromFile(nationalId, file.getAbsolutePath());
            enrolled++;
        }
        return enrolled;
    }


    /**
     * Igual que recognize pero retorna null si no hay coincidencia
     * (o si no hay documentos registrados), sin lanzar excepción.
     */
    public Match tryRecognize(Mat frontProbe) {
        try {
            return recognize(frontProbe);
        } catch (InvalidDocumentException e) {
            return null;
        }
    }

    public void remove(String nationalId) {
        Mat removed = gallery.remove(nationalId);
        if (removed != null) {
            removed.release();
        }
    }

    public Match recognize(Mat frontProbe)
            throws InvalidDocumentException {

        if (gallery.isEmpty()) {
            throw new InvalidDocumentException(
                    "No hay documentos registrados para comparar.");
        }

        Mat probe = normalize(frontProbe);

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
                "[DocumentRecognizer] mejor coincidencia: %s (%.2f, umbral %.2f)%n",
                bestId, bestScore, Constants.DOC_RECOGNITION_THRESHOLD);

        if (bestId == null
                || bestScore < Constants.DOC_RECOGNITION_THRESHOLD) {
            throw new InvalidDocumentException(
                    "La cédula no coincide con ningún usuario registrado.");
        }

        return new Match(bestId, bestScore);
    }

    private double similarity(Mat a, Mat b) {
        Mat result = new Mat();
        Imgproc.matchTemplate(a, b, result,
                Imgproc.TM_CCOEFF_NORMED);
        double score = Core.minMaxLoc(result).maxVal;
        result.release();
        return score;
    }

    private Mat normalize(Mat image) {
        Mat gray = imageProcessor.toGray(image);
        Mat resized = new Mat();
        Imgproc.resize(gray, resized, new Size(320, 202));
        Imgproc.equalizeHist(resized, resized);
        gray.release();
        return resized;
    }

    public static class Match {

        private final String nationalId;
        private final double confidence;

        public Match(String nationalId, double confidence) {
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