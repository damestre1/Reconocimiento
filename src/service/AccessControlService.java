package service;

import exception.AccessDeniedException;
import exception.FaceNotDetectedException;
import exception.FaceNotRecognizedException;
import exception.InvalidDocumentException;
import exception.UserNotFoundException;
import model.AccessMethod;
import model.AccessResult;
import model.User;
import org.opencv.core.Mat;
import repository.UserRepository;
import vision.DocumentRecognizer;
import vision.FaceRecognizer;

public class AccessControlService {

    private final UserRepository userRepository;
    private final HistoryService historyService;
    private final FaceRecognizer faceRecognizer;
    private final DocumentRecognizer documentRecognizer;

    public AccessControlService(UserRepository userRepository,
                                HistoryService historyService,
                                FaceRecognizer faceRecognizer,
                                DocumentRecognizer documentRecognizer) {
        this.userRepository = userRepository;
        this.historyService = historyService;
        this.faceRecognizer = faceRecognizer;
        this.documentRecognizer = documentRecognizer;
    }

    /** 1) Acceso por reconocimiento facial. */
    public User accessByFace(Mat probeImage)
            throws AccessDeniedException {

        try {
            FaceRecognizer.RecognitionMatch match =
                    faceRecognizer.recognize(probeImage);

            User user = requireActiveUser(match.getNationalId());

            historyService.record(
                    user.getNationalId(),
                    AccessMethod.FACIAL,
                    AccessResult.PERMITIDO,
                    String.format("Confianza: %.2f", match.getConfidence())
            );

            return user;

        } catch (FaceNotDetectedException
                 | FaceNotRecognizedException
                 | UserNotFoundException e) {

            historyService.record(
                    null,
                    AccessMethod.FACIAL,
                    AccessResult.DENEGADO,
                    e.getMessage()
            );

            throw new AccessDeniedException(
                    "Acceso denegado: " + e.getMessage());
        }
    }

    /** 2) Acceso mostrando la cédula a la cámara. */
    public User accessByDocument(Mat cardProbe)
            throws AccessDeniedException {

        String matchedId = null;

        try {
            DocumentRecognizer.Match match =
                    documentRecognizer.recognize(cardProbe);
            matchedId = match.getNationalId();

            User user = requireActiveUser(matchedId);

            historyService.record(
                    user.getNationalId(),
                    AccessMethod.CEDULA,
                    AccessResult.PERMITIDO,
                    String.format("Doc: %.2f", match.getConfidence())
            );

            return user;

        } catch (InvalidDocumentException | UserNotFoundException e) {

            historyService.record(
                    matchedId,
                    AccessMethod.CEDULA,
                    AccessResult.DENEGADO,
                    e.getMessage()
            );

            throw new AccessDeniedException(
                    "Acceso denegado: " + e.getMessage());
        }
    }

    /** 3) Acceso por rostro + cédula (doble verificación con cámara). */
    public User accessByFaceAndDocument(Mat faceProbe, Mat cardProbe)
            throws AccessDeniedException {

        String matchedId = null;

        try {
            DocumentRecognizer.Match docMatch =
                    documentRecognizer.recognize(cardProbe);
            matchedId = docMatch.getNationalId();

            User user = requireActiveUser(matchedId);

            boolean verified =
                    faceRecognizer.verify(matchedId, faceProbe);
            if (!verified) {
                throw new FaceNotRecognizedException(
                        "El rostro no coincide con el titular de la cédula.");
            }

            historyService.record(
                    user.getNationalId(),
                    AccessMethod.FACIAL_Y_CEDULA,
                    AccessResult.PERMITIDO,
                    String.format("Doc: %.2f", docMatch.getConfidence())
            );

            return user;

        } catch (InvalidDocumentException
                 | UserNotFoundException
                 | FaceNotDetectedException
                 | FaceNotRecognizedException e) {

            historyService.record(
                    matchedId,
                    AccessMethod.FACIAL_Y_CEDULA,
                    AccessResult.DENEGADO,
                    e.getMessage()
            );

            throw new AccessDeniedException(
                    "Acceso denegado: " + e.getMessage());
        }
    }

    private User requireActiveUser(String nationalId)
            throws UserNotFoundException {

        User user = userRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new UserNotFoundException(
                        "No existe un usuario con la cédula "
                                + nationalId + "."));

        if (!user.isActive()) {
            throw new UserNotFoundException(
                    "El usuario con cédula " + nationalId
                            + " está inactivo.");
        }

        return user;
    }
}