package vision;

import exception.BlurryImageException;
import exception.DarkImageException;
import exception.InvalidDocumentException;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.QRCodeDetector;
import util.Constants;

/**
 * Validador de la cédula digital colombiana basado en sus marcas
 * físicas reales:
 *
 * FRONTAL:
 *  - Fondo mayormente blanco.
 *  - Encabezado ROJO "REPÚBLICA DE COLOMBIA" en la franja superior.
 *  - Bandera tricolor (amarillo/azul/rojo apilados) en la parte alta.
 *  - Exactamente una foto del titular, a la izquierda.
 *
 * POSTERIOR:
 *  - Sin foto del titular.
 *  - Código QR REAL detectado con QRCodeDetector de OpenCV.
 *  - Zona MRZ (líneas con <<<) en la franja inferior.
 *
 * Imprime métricas en consola cada 2 segundos para poder calibrar
 * los umbrales de Constants con las tarjetas reales.
 */
public class NationalIdScanner {

    public enum DocumentSide {
        FRONT,
        BACK
    }

    private final FaceDetector faceDetector;
    private final ImageProcessor imageProcessor;
    private final QRCodeDetector qrDetector = new QRCodeDetector();

    private long lastLog = 0;

    public NationalIdScanner(FaceDetector faceDetector,
                             ImageProcessor imageProcessor) {
        this.faceDetector = faceDetector;
        this.imageProcessor = imageProcessor;
    }

    public String checkDocument(Mat card, DocumentSide expectedSide) {

        if (card == null || card.empty()) {
            return "La imagen está vacía.";
        }

        if (imageProcessor.brightness(card)
                < Constants.DOC_MIN_BRIGHTNESS) {
            return "Imagen muy oscura. Busque mas luz.";
        }

        if (imageProcessor.blurVariance(card)
                < Constants.DOC_MIN_BLUR) {
            return "Imagen borrosa. Mantenga la cedula quieta.";
        }

        if (imageProcessor.edgeDensity(card)
                < Constants.DOC_MIN_EDGE_DENSITY) {
            return "No se detecta una cedula en el recuadro.";
        }

        Rect[] faces = faceDetector.detectFaces(card);

        for (Rect face : faces) {
            double widthRatio = (double) face.width / card.cols();
            if (widthRatio > Constants.DOC_MAX_CARD_FACE_RATIO) {
                return "Eso parece su rostro. Muestre la cedula.";
            }
        }

        if (expectedSide == DocumentSide.FRONT) {
            return checkFrontSide(card, faces);
        }

        return checkBackSide(card, faces);
    }

    // ==================== FRONTAL ====================
    /**
     * La foto del titular (una sola, a la izquierda) es OBLIGATORIA.
     * De las 3 marcas de color (fondo blanco, encabezado rojo,
     * bandera) basta cumplir CARD_FRONT_MIN_MARKS: así una cámara
     * de baja calidad que "lava" un color no bloquea la cédula real,
     * pero otras tarjetas (que cumplen 0 o 1 marca) siguen afuera.
     */
    private String checkFrontSide(Mat card, Rect[] faces) {

        double whiteness = imageProcessor.whitenessRatio(card);

        Rect headerRect = new Rect(
                0, 0, card.cols(), (int) (card.rows() * 0.25));
        Mat header = card.submat(headerRect);
        double red = imageProcessor.redRatio(header);
        header.release();

        boolean flag = hasColombianFlag(card);

        logFront(whiteness, red, flag, faces.length);

        // --- Foto del titular: obligatoria ---
        if (faces.length == 0) {
            return "No se ve la foto del titular. Esto no parece una cedula.";
        }

        if (faces.length > 1) {
            return "Se ven varias fotos. Esto no parece una cedula.";
        }

        Rect photo = faces[0];

        double heightRatio = (double) photo.height / card.rows();
        if (heightRatio < Constants.CARD_PHOTO_MIN_HEIGHT_RATIO
                || heightRatio > Constants.CARD_PHOTO_MAX_HEIGHT_RATIO) {
            return "La foto del documento no tiene el tamano de una cedula.";
        }

        double centerX = (photo.x + photo.width / 2.0) / card.cols();
        if (centerX > Constants.CARD_PHOTO_MAX_CENTER_X) {
            return "Esto no parece una cedula (la foto debe ir a la izquierda).";
        }

        // --- Marcas de color: basta cumplir N de 3 ---
        int marks = 0;
        if (whiteness >= Constants.CARD_WHITE_MIN_RATIO) {
            marks++;
        }
        if (red >= Constants.CARD_RED_HEADER_MIN_RATIO) {
            marks++;
        }
        if (flag) {
            marks++;
        }

        if (marks < Constants.CARD_FRONT_MIN_MARKS) {
            return "Esto no parece una cedula colombiana. "
                    + "Mejore la luz e intente de nuevo.";
        }

        return null;
    }

    /**
     * Busca la bandera de Colombia en la franja superior: una zona
     * con amarillo encima de azul encima de rojo, apilados.
     */
    private boolean hasColombianFlag(Mat card) {
        Rect stripRect = new Rect(
                0, 0, card.cols(), (int) (card.rows() * 0.30));
        Mat strip = card.submat(stripRect);

        Mat small = new Mat();
        Imgproc.resize(strip, small, new Size(240, 60));
        strip.release();

        int windowWidth = 24;
        int step = 8;
        boolean found = false;

        for (int x = 0; x + windowWidth <= small.cols() && !found; x += step) {
            Mat top = small.submat(0, 20, x, x + windowWidth);
            Mat middle = small.submat(20, 40, x, x + windowWidth);
            Mat bottom = small.submat(40, 60, x, x + windowWidth);

            double yellow = imageProcessor.yellowRatio(top);
            double blue = imageProcessor.blueRatio(middle);
            double red = imageProcessor.redRatio(bottom);

            top.release();
            middle.release();
            bottom.release();

            if (yellow >= 0.18 && blue >= 0.14 && red >= 0.14) {
                found = true;
            }
        }

        small.release();
        return found;
    }

    // ==================== POSTERIOR ====================
    private String checkBackSide(Mat card, Rect[] faces) {

        if (faces.length > 0) {
            return "Esa es la cara frontal. Voltee la cedula.";
        }

        // Código QR REAL: detector de OpenCV, no una heurística.
        Mat points = new Mat();
        boolean qrFound = qrDetector.detect(card, points);
        points.release();

        // Zona MRZ: franja inferior con texto denso.
        Rect mrzRect = new Rect(
                0,
                (int) (card.rows() * 0.62),
                card.cols(),
                (int) (card.rows() * 0.38));
        Mat mrzZone = card.submat(mrzRect);
        double mrzDensity = imageProcessor.edgeDensity(mrzZone);
        mrzZone.release();

        logBack(qrFound, mrzDensity);

        if (!qrFound) {
            return "No se detecta el codigo QR de la cedula.";
        }

        if (mrzDensity < Constants.CARD_MRZ_MIN_DENSITY) {
            return "No se ve la zona de texto inferior de la cedula.";
        }

        return null;
    }

    // ==================== AUXILIARES ====================
    public boolean isSameAsFront(Mat card, Mat normalizedFrontRef) {
        if (normalizedFrontRef == null || normalizedFrontRef.empty()) {
            return false;
        }

        Mat probe = normalizeCard(card);
        double score = similarity(probe, normalizedFrontRef);
        probe.release();

        return score >= Constants.DOC_SAME_SIDE_THRESHOLD;
    }

    public Mat normalizeCard(Mat image) {
        Mat gray = imageProcessor.toGray(image);
        Mat resized = new Mat();
        Imgproc.resize(gray, resized, new Size(320, 202));
        Imgproc.equalizeHist(resized, resized);
        gray.release();
        return resized;
    }

    private double similarity(Mat a, Mat b) {
        Mat result = new Mat();
        Imgproc.matchTemplate(a, b, result,
                Imgproc.TM_CCOEFF_NORMED);
        double score = Core.minMaxLoc(result).maxVal;
        result.release();
        return score;
    }

    public void validateDocumentImage(Mat image, DocumentSide expectedSide)
            throws InvalidDocumentException,
            BlurryImageException,
            DarkImageException {

        if (image == null || image.empty()) {
            throw new InvalidDocumentException(
                    "La imagen del documento está vacía o no se pudo leer.");
        }

        if (imageProcessor.brightness(image)
                < Constants.DOC_MIN_BRIGHTNESS) {
            throw new DarkImageException(
                    "La imagen del documento está demasiado oscura.");
        }

        if (imageProcessor.blurVariance(image)
                < Constants.DOC_MIN_BLUR) {
            throw new BlurryImageException(
                    "La imagen del documento está borrosa. Capture de nuevo.");
        }

        String error = checkDocument(image, expectedSide);
        if (error != null) {
            throw new InvalidDocumentException(error);
        }
    }

    private void logFront(double whiteness, double red,
                          boolean flag, int faces) {
        long now = System.currentTimeMillis();
        if (now - lastLog < 2000) {
            return;
        }
        lastLog = now;
        System.out.printf(
                "[Scanner FRONTAL] blanco=%.2f (min %.2f) | "
                        + "rojo=%.3f (min %.3f) | bandera=%s | fotos=%d%n",
                whiteness, Constants.CARD_WHITE_MIN_RATIO,
                red, Constants.CARD_RED_HEADER_MIN_RATIO,
                flag ? "SI" : "NO", faces);
    }

    private void logBack(boolean qr, double mrz) {
        long now = System.currentTimeMillis();
        if (now - lastLog < 2000) {
            return;
        }
        lastLog = now;
        System.out.printf(
                "[Scanner POSTERIOR] QR=%s | mrz=%.3f (min %.3f)%n",
                qr ? "SI" : "NO",
                mrz, Constants.CARD_MRZ_MIN_DENSITY);
    }
}