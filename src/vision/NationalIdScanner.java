package vision;

import exception.BlurryImageException;
import exception.DarkImageException;
import exception.InvalidDocumentException;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import util.Constants;

/**
 * Validador de la cédula digital colombiana basado en sus marcas
 * reales:
 *
 * FRONTAL: encabezado rojo "REPÚBLICA DE COLOMBIA" en la franja
 * superior + foto del titular a la izquierda.
 *
 * POSTERIOR: código QR en el cuadrante superior derecho + zona MRZ
 * (texto con <<<) en la franja inferior, y sin foto del titular.
 */
public class NationalIdScanner {

    public enum DocumentSide {
        FRONT,
        BACK
    }

    private final FaceDetector faceDetector;
    private final ImageProcessor imageProcessor;

    public NationalIdScanner(FaceDetector faceDetector,
                             ImageProcessor imageProcessor) {
        this.faceDetector = faceDetector;
        this.imageProcessor = imageProcessor;
    }

    /**
     * Chequeo en vivo del recorte del recuadro guía.
     *
     * @return null si es válida, o el mensaje del problema.
     */
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

        // Un rostro grande = es la persona, no el documento.
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

    /**
     * FRONTAL de la cédula colombiana:
     * 1. Encabezado ROJO "REPÚBLICA DE COLOMBIA" en la franja superior.
     * 2. Exactamente UNA foto de titular.
     * 3. Foto a la IZQUIERDA y con tamaño típico.
     * Una tarjeta bancaria u otra tarjeta no cumple estas tres a la vez.
     */
    private String checkFrontSide(Mat card, Rect[] faces) {

        // 1. Encabezado rojo en el 25% superior
        Rect headerRect = new Rect(
                0, 0, card.cols(), (int) (card.rows() * 0.25));
        Mat header = card.submat(headerRect);
        double red = imageProcessor.redRatio(header);
        header.release();

        if (red < Constants.CARD_RED_HEADER_MIN_RATIO) {
            return "No se ve el encabezado de la cedula. "
                    + "Esto no parece una cedula colombiana.";
        }

        // 2. Exactamente una foto de titular
        if (faces.length == 0) {
            return "No se ve la foto del titular. Esto no parece una cedula.";
        }

        if (faces.length > 1) {
            return "Se ven varias fotos. Esto no parece una cedula.";
        }

        // 3. Tamaño y posición de la foto
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

        return null;
    }

    /**
     * POSTERIOR de la cédula colombiana:
     * 1. SIN foto del titular.
     * 2. Código QR en el cuadrante superior derecho.
     * 3. Zona MRZ (líneas con <<<) en la franja inferior.
     */
    private String checkBackSide(Mat card, Rect[] faces) {

        if (faces.length > 0) {
            return "Esa es la cara frontal. Voltee la cedula.";
        }

        // 2. Código QR: cuadrante superior derecho con alta densidad
        Rect qrRect = new Rect(
                (int) (card.cols() * 0.58),
                0,
                (int) (card.cols() * 0.42),
                (int) (card.rows() * 0.50));
        Mat qrZone = card.submat(qrRect);
        double qrDensity = imageProcessor.edgeDensity(qrZone);
        qrZone.release();

        if (qrDensity < Constants.CARD_QR_MIN_DENSITY) {
            return "No se ve el codigo QR. Esto no parece la "
                    + "parte posterior de una cedula.";
        }

        // 3. Zona MRZ: franja inferior con texto denso
        Rect mrzRect = new Rect(
                0,
                (int) (card.rows() * 0.62),
                card.cols(),
                (int) (card.rows() * 0.38));
        Mat mrzZone = card.submat(mrzRect);
        double mrzDensity = imageProcessor.edgeDensity(mrzZone);
        mrzZone.release();

        if (mrzDensity < Constants.CARD_MRZ_MIN_DENSITY) {
            return "No se ve la zona de texto inferior. Esto no "
                    + "parece la parte posterior de una cedula.";
        }

        return null;
    }

    /**
     * Compara la captura actual con la cara frontal ya guardada.
     */
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

    /** Validación con excepciones, para imágenes desde archivo. */
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
}