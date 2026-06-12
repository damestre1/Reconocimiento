package service;

import exception.BlurryImageException;
import exception.DarkImageException;
import exception.InvalidDocumentException;
import exception.InvalidNationalIdException;
import org.opencv.core.Mat;
import util.Constants;
import util.ValidationUtils;
import vision.ImageProcessor;
import vision.NationalIdScanner;
import vision.NationalIdScanner.DocumentSide;

import java.io.File;

public class NationalIdService {

    private final NationalIdScanner scanner;
    private final ImageProcessor imageProcessor;

    public NationalIdService(NationalIdScanner scanner,
                             ImageProcessor imageProcessor) {
        this.scanner = scanner;
        this.imageProcessor = imageProcessor;
    }

    public String processFrontImage(String nationalId, String sourceImagePath)
            throws InvalidNationalIdException,
            InvalidDocumentException,
            BlurryImageException,
            DarkImageException {

        return processSide(
                nationalId,
                sourceImagePath,
                DocumentSide.FRONT,
                Constants.FRONT_SUFFIX
        );
    }

    public String processBackImage(String nationalId, String sourceImagePath)
            throws InvalidNationalIdException,
            InvalidDocumentException,
            BlurryImageException,
            DarkImageException {

        return processSide(
                nationalId,
                sourceImagePath,
                DocumentSide.BACK,
                Constants.BACK_SUFFIX
        );
    }

    private String processSide(String nationalId,
                               String sourceImagePath,
                               DocumentSide side,
                               String suffix)
            throws InvalidNationalIdException,
            InvalidDocumentException,
            BlurryImageException,
            DarkImageException {

        String id = ValidationUtils.validateNationalId(nationalId);

        Mat image = imageProcessor.load(sourceImagePath);
        try {
            scanner.validateDocumentImage(image, side);

            String destination = Constants.PHOTOS_DIR
                    + File.separator
                    + id
                    + suffix;

            if (!imageProcessor.save(image, destination)) {
                throw new InvalidDocumentException(
                        "No se pudo guardar la imagen del documento.");
            }

            return destination;
        } finally {
            image.release();
        }
    }
}
