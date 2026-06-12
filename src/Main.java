import exception.AccessDeniedException;
import model.AccessRecord;
import model.DocumentType;
import model.FaceRecord;
import model.User;
import org.opencv.core.Mat;
import repository.AccessRecordRepository;
import repository.SQLiteUserRepository;
import repository.SQLiteAccessRecordRepository;
import repository.UserRepository;
import service.AccessControlService;
import service.DocumentCaptureService;
import service.FaceRecognitionService;
import service.HistoryService;
import service.RegistrationService;
import util.Configuration;
import util.Constants;
import vision.DocumentRecognizer;
import vision.FaceDetector;
import vision.FaceRecognizer;
import vision.ImageProcessor;
import vision.NationalIdScanner;

import java.io.File;
import java.time.LocalDate;
import java.util.Scanner;

public class Main {

    private static UserRepository userRepository;
    private static RegistrationService registrationService;
    private static FaceRecognitionService faceRecognitionService;
    private static DocumentCaptureService documentCaptureService;
    private static AccessControlService accessControlService;
    private static HistoryService historyService;
    private static FaceRecognizer faceRecognizer;
    private static DocumentRecognizer documentRecognizer;

    public static void main(String[] args) {

        Configuration.loadOpenCv();
        System.out.println("OpenCV 4.11 cargado correctamente.");

        initServices();

        int faces = faceRecognizer
                .enrollFromDirectory(Constants.PHOTOS_DIR);
        int docs = documentRecognizer
                .enrollFromDirectory(Constants.PHOTOS_DIR);
        System.out.println("Usuarios cargados: "
                + userRepository.findAll().size()
                + " | Rostros: " + faces
                + " | Documentos: " + docs);

        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.println();
            System.out.println("===== CONTROL DE ACCESO FACIAL =====");
            System.out.println("1. Registrar nuevo usuario");
            System.out.println("2. Ingresar por rostro");
            System.out.println("3. Ingresar por cedula (camara)");
            System.out.println("4. Ingresar por rostro + cedula");
            System.out.println("5. Ver historial de accesos");
            System.out.println("6. Ver usuarios registrados");
            System.out.println("7. Eliminar usuario");
            System.out.println("0. Salir");
            System.out.print("Opcion: ");

            String option = scanner.nextLine().trim();

            switch (option) {
                case "1":
                    registerUser(scanner);
                    break;
                case "2":
                    if (requireRegisteredUsers()) {
                        accessByFace();
                    }
                    break;
                case "3":
                    if (requireRegisteredUsers()) {
                        accessByDocument();
                    }
                    break;
                case "4":
                    if (requireRegisteredUsers()) {
                        accessByFaceAndDocument();
                    }
                    break;
                case "5":
                    showHistory();
                    break;
                case "6":
                    showUsers();
                    break;
                case "7":
                    if (requireRegisteredUsers()) {
                        deleteUser(scanner);
                    }
                    break;
                case "0":
                    exit = true;
                    break;
                default:
                    System.out.println("Opcion no valida.");
            }
        }

        System.out.println("Programa finalizado.");
        scanner.close();
        System.exit(0);
    }

    private static void initServices() {
        userRepository = new SQLiteUserRepository();
        AccessRecordRepository accessRepository =
                new SQLiteAccessRecordRepository();

        ImageProcessor imageProcessor = new ImageProcessor();
        FaceDetector faceDetector = new FaceDetector();
        faceRecognizer =
                new FaceRecognizer(faceDetector, imageProcessor);
        documentRecognizer = new DocumentRecognizer(imageProcessor);
        NationalIdScanner idScanner =
                new NationalIdScanner(faceDetector, imageProcessor);

        registrationService =
                new RegistrationService(userRepository);
        faceRecognitionService = new FaceRecognitionService(
                faceDetector, imageProcessor, faceRecognizer);
        documentCaptureService =
                new DocumentCaptureService(idScanner, imageProcessor);
        historyService = new HistoryService(accessRepository);
        accessControlService = new AccessControlService(
                userRepository, historyService,
                faceRecognizer, documentRecognizer);
    }

    private static boolean requireRegisteredUsers() {
        if (userRepository.findAll().isEmpty()) {
            System.out.println(
                    "No hay usuarios registrados. "
                            + "Debe registrar al menos un usuario (opcion 1) "
                            + "antes de usar esta opcion.");
            return false;
        }
        return true;
    }

    // ============ 1. REGISTRO COMPLETO ============
    private static void registerUser(Scanner scanner) {
        Mat faceProbe = null;
        Mat cardProbe = null;
        Mat backProbe = null;
        User user = null;

        try {
            System.out.println();
            System.out.println("--- PASO 1: Datos personales ---");

            System.out.print("Numero de cedula: ");
            String nationalId = scanner.nextLine().trim();

            System.out.print("Nombres: ");
            String firstName = scanner.nextLine().trim();

            System.out.print("Apellidos: ");
            String lastName = scanner.nextLine().trim();

            System.out.print("Correo electronico: ");
            String email = scanner.nextLine().trim();

            System.out.print("Telefono: ");
            String phone = scanner.nextLine().trim();

            user = registrationService.registerUser(
                    nationalId,
                    firstName,
                    lastName,
                    email,
                    phone,
                    DocumentType.CEDULA_CIUDADANIA,
                    LocalDate.now(),
                    null
            );

            System.out.println("Datos validados: " + user);

            // --- PASO 2: Rostro (con verificación de duplicado) ---
            System.out.println();
            System.out.println("--- PASO 2: Captura del rostro ---");
            System.out.println("La camara capturara sola tras el conteo 3, 2, 1.");

            faceProbe = faceRecognitionService.captureProbe();
            if (faceProbe == null) {
                rollback(user, "Captura de rostro cancelada.");
                return;
            }

            FaceRecognizer.RecognitionMatch faceDup =
                    faceRecognizer.tryRecognize(faceProbe);
            if (faceDup != null) {
                rollback(user, "Este rostro YA esta registrado con la cedula "
                        + faceDup.getNationalId() + ".");
                return;
            }

            String facePath = faceRecognitionService
                    .saveAndEnroll(user.getNationalId(), faceProbe);
            if (facePath == null) {
                rollback(user, "No se pudo guardar el rostro.");
                return;
            }

            user.setFaceRecord(
                    new FaceRecord(user.getNationalId(), facePath));
            userRepository.save(user);
            System.out.println("Rostro guardado: " + facePath);

            // --- PASO 3: Cédula frontal (con verificación de duplicado) ---
            System.out.println();
            System.out.println("--- PASO 3: Cedula (cara frontal) ---");

            cardProbe = documentCaptureService.captureFrontProbe();
            if (cardProbe == null) {
                rollbackWithFace(user,
                        "Captura del documento frontal cancelada.");
                return;
            }

            DocumentRecognizer.Match docDup =
                    documentRecognizer.tryRecognize(cardProbe);
            if (docDup != null) {
                rollbackWithFace(user,
                        "Esta cedula YA esta registrada con el usuario "
                                + docDup.getNationalId() + ".");
                return;
            }

            String frontPath = documentCaptureService.saveCard(
                    cardProbe, user.getNationalId(),
                    Constants.FRONT_SUFFIX);
            if (frontPath == null) {
                rollbackWithFace(user,
                        "No se pudo guardar el documento frontal.");
                return;
            }
            documentRecognizer.enroll(user.getNationalId(), cardProbe);
            System.out.println("Frontal guardada: " + frontPath);

            // --- PASO 4: Cédula posterior (no acepta la frontal repetida) ---
            System.out.println();
            System.out.println("--- PASO 4: Cedula (cara posterior) ---");

            backProbe = documentCaptureService
                    .captureBackProbe(user.getNationalId());
            if (backProbe == null) {
                rollbackComplete(user,
                        "Captura del documento posterior cancelada.");
                return;
            }

            String backPath = documentCaptureService.saveCard(
                    backProbe, user.getNationalId(),
                    Constants.BACK_SUFFIX);
            if (backPath == null) {
                rollbackComplete(user,
                        "No se pudo guardar el documento posterior.");
                return;
            }
            System.out.println("Posterior guardada: " + backPath);

            System.out.println();
            System.out.println("REGISTRO COMPLETO: "
                    + user.getFullName()
                    + " (cedula " + user.getNationalId() + ")");

        } catch (Exception e) {
            if (user != null) {
                rollbackComplete(user,
                        "No se pudo registrar: " + e.getMessage());
            } else {
                System.out.println("No se pudo registrar: " + e.getMessage());
            }
        } finally {
            if (faceProbe != null) faceProbe.release();
            if (cardProbe != null) cardProbe.release();
            if (backProbe != null) backProbe.release();
        }
    }

    /** Anula un registro que no llegó a guardar archivos. */
    private static void rollback(User user, String reason) {
        userRepository.deleteByNationalId(user.getNationalId());
        System.out.println(reason + " Registro anulado.");
    }

    /** Anula el registro y borra el rostro ya guardado. */
    private static void rollbackWithFace(User user, String reason) {
        faceRecognizer.remove(user.getNationalId());
        deletePhoto(user.getNationalId(), Constants.FACE_SUFFIX);
        rollback(user, reason);
    }

    /** Anula el registro y borra todos los archivos guardados. */
    private static void rollbackComplete(User user, String reason) {
        faceRecognizer.remove(user.getNationalId());
        documentRecognizer.remove(user.getNationalId());
        deletePhoto(user.getNationalId(), Constants.FACE_SUFFIX);
        deletePhoto(user.getNationalId(), Constants.FRONT_SUFFIX);
        deletePhoto(user.getNationalId(), Constants.BACK_SUFFIX);
        rollback(user, reason);
    }

    private static void deletePhoto(String nationalId, String suffix) {
        File file = new File(Constants.PHOTOS_DIR
                + File.separator + nationalId + suffix);
        if (file.exists() && !file.delete()) {
            System.out.println("Advertencia: no se pudo borrar "
                    + file.getPath());
        }
    }

    // ============ 2. INGRESO POR ROSTRO ============
    private static void accessByFace() {
        System.out.println("Mire a la camara. Capturara sola en 3, 2, 1.");

        Mat probe = faceRecognitionService.captureProbe();
        if (probe == null) {
            System.out.println("Verificacion cancelada.");
            return;
        }

        try {
            User user = accessControlService.accessByFace(probe);
            System.out.println("ACCESO PERMITIDO. Bienvenido(a), "
                    + user.getFullName() + ".");
        } catch (AccessDeniedException e) {
            System.out.println(e.getMessage());
        } finally {
            probe.release();
        }
    }

    // ============ 3. INGRESO POR CÉDULA (CÁMARA) ============
    private static void accessByDocument() {
        System.out.println("Muestre la cara frontal de su cedula a la camara.");

        Mat cardProbe = documentCaptureService.captureFrontProbe();
        if (cardProbe == null) {
            System.out.println("Verificacion cancelada.");
            return;
        }

        try {
            User user =
                    accessControlService.accessByDocument(cardProbe);
            System.out.println("ACCESO PERMITIDO. Bienvenido(a), "
                    + user.getFullName() + ".");
        } catch (AccessDeniedException e) {
            System.out.println(e.getMessage());
        } finally {
            cardProbe.release();
        }
    }

    // ============ 4. INGRESO POR ROSTRO + CÉDULA ============
    private static void accessByFaceAndDocument() {
        System.out.println("Paso 1 de 2: verificacion del rostro.");

        Mat faceProbe = faceRecognitionService.captureProbe();
        if (faceProbe == null) {
            System.out.println("Verificacion cancelada.");
            return;
        }

        System.out.println("Paso 2 de 2: muestre la cara frontal de su cedula.");

        Mat cardProbe = documentCaptureService.captureFrontProbe();
        if (cardProbe == null) {
            System.out.println("Verificacion cancelada.");
            faceProbe.release();
            return;
        }

        try {
            User user = accessControlService
                    .accessByFaceAndDocument(faceProbe, cardProbe);
            System.out.println("ACCESO PERMITIDO. Bienvenido(a), "
                    + user.getFullName() + ".");
        } catch (AccessDeniedException e) {
            System.out.println(e.getMessage());
        } finally {
            faceProbe.release();
            cardProbe.release();
        }
    }

    // ============ 5. HISTORIAL ============
    private static void showHistory() {
        System.out.println("--- Historial de accesos ---");
        if (historyService.getAll().isEmpty()) {
            System.out.println("(sin registros)");
            return;
        }
        for (AccessRecord record : historyService.getAll()) {
            System.out.println(record);
        }
    }

    // ============ 6. USUARIOS ============
    private static void showUsers() {
        System.out.println("--- Usuarios registrados ---");
        if (userRepository.findAll().isEmpty()) {
            System.out.println("(sin usuarios)");
            return;
        }
        for (User user : userRepository.findAll()) {
            System.out.println(user
                    + (user.hasFaceEnrolled() ? " [rostro OK]" : ""));
        }
    }

    // ============ 7. ELIMINAR USUARIO ============
    private static void deleteUser(Scanner scanner) {
        System.out.print("Numero de cedula del usuario a eliminar: ");
        String nationalId = scanner.nextLine().trim();

        User user = userRepository
                .findByNationalId(nationalId).orElse(null);

        if (user == null) {
            System.out.println(
                    "No existe un usuario con la cedula " + nationalId + ".");
            return;
        }

        System.out.println("Se eliminara a: " + user.getFullName()
                + " (cedula " + nationalId + ") junto con sus fotos.");
        System.out.print("Esta seguro? (S/N): ");
        String confirm = scanner.nextLine().trim();

        if (!confirm.equalsIgnoreCase("S")) {
            System.out.println("Eliminacion cancelada.");
            return;
        }

        userRepository.deleteByNationalId(nationalId);
        faceRecognizer.remove(nationalId);
        documentRecognizer.remove(nationalId);
        deletePhoto(nationalId, Constants.FACE_SUFFIX);
        deletePhoto(nationalId, Constants.FRONT_SUFFIX);
        deletePhoto(nationalId, Constants.BACK_SUFFIX);

        System.out.println("Usuario eliminado correctamente.");
    }
}
