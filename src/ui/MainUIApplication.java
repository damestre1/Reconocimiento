package ui;

import exception.AccessDeniedException;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import model.AccessRecord;
import model.DocumentType;
import model.FaceRecord;
import model.User;
import org.opencv.core.Mat;
import repository.AccessRecordRepository;
import repository.FileUserRepository;
import repository.InMemoryAccessRecordRepository;
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
import java.util.List;

/**
 * VERSIÓN QUE ABRE LA CÁMARA CORRECTAMENTE
 * Basada en Main.java original - Captura síncrona
 * Las capturas de cámara SE ABREN AHORA
 */
public class MainUIApplication extends Application {

    private UserRepository userRepository;
    private RegistrationService registrationService;
    private FaceRecognitionService faceRecognitionService;
    private DocumentCaptureService documentCaptureService;
    private AccessControlService accessControlService;
    private HistoryService historyService;
    private FaceRecognizer faceRecognizer;
    private DocumentRecognizer documentRecognizer;
    private AccessRecordRepository accessRepository;

    private Stage primaryStage;
    private BorderPane rootPane;
    private Label statusLabel;
    private Label userCountLabel;

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;

        // Cargar OpenCV PRIMERO
        Configuration.loadOpenCv();
        System.out.println("✓ OpenCV cargado correctamente");

        initServices();

        // Encargar rostros y documentos
        int faces = faceRecognizer.enrollFromDirectory(Constants.PHOTOS_DIR);
        int docs = documentRecognizer.enrollFromDirectory(Constants.PHOTOS_DIR);
        System.out.println("✓ Rostros cargados: " + faces + " | Documentos: " + docs);

        stage.setTitle("Sistema de Control de Acceso Facial");
        stage.setWidth(1200);
        stage.setHeight(800);
        stage.setMinWidth(900);
        stage.setMinHeight(600);

        rootPane = new BorderPane();
        rootPane.setStyle("-fx-background-color: #f5f5f5;");
        rootPane.setTop(createHeader());
        rootPane.setCenter(createMainMenuPanel());
        rootPane.setBottom(createFooter());

        Scene scene = new Scene(rootPane);
        stage.setScene(scene);
        stage.show();

        updateUserCount();
    }

    private void initServices() {
        userRepository = new FileUserRepository();
        accessRepository = new InMemoryAccessRecordRepository();

        ImageProcessor imageProcessor = new ImageProcessor();
        FaceDetector faceDetector = new FaceDetector();
        faceRecognizer = new FaceRecognizer(faceDetector, imageProcessor);
        documentRecognizer = new DocumentRecognizer(imageProcessor);
        NationalIdScanner idScanner = new NationalIdScanner(faceDetector, imageProcessor);

        registrationService = new RegistrationService(userRepository);
        faceRecognitionService = new FaceRecognitionService(faceDetector, imageProcessor, faceRecognizer);
        documentCaptureService = new DocumentCaptureService(idScanner, imageProcessor);
        historyService = new HistoryService(accessRepository);
        accessControlService = new AccessControlService(userRepository, historyService, faceRecognizer, documentRecognizer);
    }

    private VBox createHeader() {
        VBox header = new VBox();
        header.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, #1a1a2e, #16213e); -fx-padding: 20;");
        header.setSpacing(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("🔐 Sistema de Control de Acceso Facial");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.WHITE);

        Label subtitleLabel = new Label("Reconocimiento biométrico avanzado");
        subtitleLabel.setFont(Font.font("Segoe UI", 14));
        subtitleLabel.setTextFill(Color.web("#b0b0b0"));

        header.getChildren().addAll(titleLabel, subtitleLabel);
        return header;
    }

    private ScrollPane createMainMenuPanel() {
        VBox mainMenu = new VBox(30);
        mainMenu.setPadding(new Insets(40));
        mainMenu.setStyle("-fx-background-color: #f5f5f5;");

        Label welcomeLabel = new Label("Bienvenido al Sistema de Acceso Facial");
        welcomeLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        welcomeLabel.setTextFill(Color.web("#1a1a2e"));

        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(30);
        grid.setPadding(new Insets(20));

        grid.add(createMenuCard("📝 Registrar Usuario", "Registra un nuevo usuario con rostro y cédula", () -> openRegisterUserWindow()), 0, 0);
        grid.add(createMenuCard("👤 Acceso por Rostro", "Verifica acceso usando reconocimiento facial", () -> openAccessByFaceWindow()), 1, 0);
        grid.add(createMenuCard("🆔 Acceso por Cédula", "Verifica acceso escaneando la cédula", () -> openAccessByDocumentWindow()), 2, 0);
        grid.add(createMenuCard("🔒 Doble Verificación", "Rostro + cédula para máxima seguridad", () -> openAccessByFaceAndDocumentWindow()), 0, 1);
        grid.add(createMenuCard("📊 Ver Historial", "Consulta el registro de accesos", () -> openHistoryWindow()), 1, 1);
        grid.add(createMenuCard("👥 Usuarios Registrados", "Lista de todos los usuarios del sistema", () -> openUsersWindow()), 2, 1);
        grid.add(createMenuCard("🗑️ Eliminar Usuario", "Elimina un usuario del sistema", () -> openDeleteUserWindow()), 0, 2);

        mainMenu.getChildren().addAll(welcomeLabel, grid);
        ScrollPane scrollPane = new ScrollPane(mainMenu);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 0;");
        return scrollPane;
    }

    private VBox createMenuCard(String title, String description, Runnable action) {
        VBox card = new VBox(15);
        card.setPadding(new Insets(25));
        card.setStyle("-fx-background-color: white; -fx-border-radius: 8; -fx-border-color: #e0e0e0; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2);");
        card.setPrefSize(250, 150);
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setAlignment(Pos.TOP_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.web("#1a1a2e"));
        titleLabel.setWrapText(true);

        Label descLabel = new Label(description);
        descLabel.setFont(Font.font("Segoe UI", 12));
        descLabel.setTextFill(Color.web("#666666"));
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(250);

        card.getChildren().addAll(titleLabel, descLabel);

        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: #f9f9f9; -fx-border-radius: 8; -fx-border-color: #0066cc; -fx-border-width: 2; -fx-effect: dropshadow(gaussian, rgba(0,102,204,0.3), 8, 0, 0, 4);");
        });

        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: white; -fx-border-radius: 8; -fx-border-color: #e0e0e0; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2);");
        });

        card.setOnMouseClicked(e -> action.run());

        return card;
    }

    private HBox createFooter() {
        HBox footer = new HBox(20);
        footer.setPadding(new Insets(15, 20, 15, 20));
        footer.setStyle("-fx-background-color: #2a2a3e; -fx-border-color: #1a1a2e;");
        footer.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label("Estado: Sistema listo");
        statusLabel.setTextFill(Color.web("#90EE90"));
        statusLabel.setFont(Font.font("Segoe UI", 12));

        Separator separator = new Separator();
        separator.setOrientation(javafx.geometry.Orientation.VERTICAL);

        userCountLabel = new Label("Usuarios: 0");
        userCountLabel.setTextFill(Color.WHITE);
        userCountLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));

        footer.getChildren().addAll(statusLabel, separator, userCountLabel);
        return footer;
    }

    private void updateUserCount() {
        int count = userRepository.findAll().size();
        userCountLabel.setText("Usuarios registrados: " + count);
    }

    private void updateStatus(String message, boolean success) {
        statusLabel.setText(message);
        statusLabel.setTextFill(success ? Color.web("#90EE90") : Color.web("#FF6B6B"));
    }

    // ========== REGISTRO DE USUARIO ==========
    private void openRegisterUserWindow() {
        Stage window = new Stage();
        window.setTitle("Registrar Nuevo Usuario");
        window.setWidth(600);
        window.setHeight(400);
        window.initOwner(primaryStage);

        VBox container = new VBox(20);
        container.setPadding(new Insets(30));
        container.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Registro Completo de Usuario");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        titleLabel.setTextFill(Color.web("#1a1a2e"));

        VBox formVBox = new VBox(15);
        formVBox.setPadding(new Insets(10));

        TextField cedulaField = createStyledTextField("Número de cédula");
        TextField nombreField = createStyledTextField("Nombres");
        TextField apellidoField = createStyledTextField("Apellidos");
        TextField emailField = createStyledTextField("Correo electrónico");
        TextField telefonoField = createStyledTextField("Teléfono");

        formVBox.getChildren().addAll(
                new Label("Información Personal:"),
                cedulaField, nombreField, apellidoField, emailField, telefonoField
        );

        ScrollPane scrollPane = new ScrollPane(formVBox);
        scrollPane.setFitToWidth(true);

        Button registerButton = createStyledButton("Iniciar Registro");
        registerButton.setStyle("-fx-padding: 12; -fx-font-size: 14; -fx-background-color: #0066cc; -fx-text-fill: white; -fx-font-weight: bold;");

        Button cancelButton = createStyledButton("Cancelar");
        cancelButton.setOnAction(e -> window.close());

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.getChildren().addAll(cancelButton, registerButton);

        registerButton.setOnAction(e -> {
            String cedula = cedulaField.getText().trim();
            String nombre = nombreField.getText().trim();
            String apellido = apellidoField.getText().trim();
            String email = emailField.getText().trim();
            String telefono = telefonoField.getText().trim();

            if (cedula.isEmpty() || nombre.isEmpty() || apellido.isEmpty()) {
                showAlert("Error", "Los campos de cédula, nombre y apellido son obligatorios");
                return;
            }

            try {
                // Crear usuario
                User user = registrationService.registerUser(
                        cedula, nombre, apellido, email, telefono,
                        DocumentType.CEDULA_CIUDADANIA,
                        LocalDate.now(), null
                );

                System.out.println("\n--- PASO 1: Datos personales guardados ---");
                System.out.println("Usuario: " + user.getFullName());

                // PASO 2: Captura del rostro
                System.out.println("\n--- PASO 2: Captura del rostro ---");
                System.out.println("Mire a la cámara. Se capturará en 3, 2, 1...");
                updateStatus("Preparando captura de rostro...", true);

                Mat faceProbe = faceRecognitionService.captureProbe();
                if (faceProbe == null) {
                    rollback(user, "Captura de rostro cancelada.");
                    window.close();
                    return;
                }

                // Verificar rostro duplicado
                FaceRecognizer.RecognitionMatch faceDup = faceRecognizer.tryRecognize(faceProbe);
                if (faceDup != null) {
                    rollback(user, "Este rostro YA esta registrado con la cedula " + faceDup.getNationalId() + ".");
                    window.close();
                    return;
                }

                // Guardar rostro
                String facePath = faceRecognitionService.saveAndEnroll(user.getNationalId(), faceProbe);
                if (facePath == null) {
                    rollback(user, "No se pudo guardar el rostro.");
                    window.close();
                    return;
                }

                user.setFaceRecord(new FaceRecord(user.getNationalId(), facePath));
                System.out.println("Rostro guardado: " + facePath);

                // PASO 3: Captura de cédula frontal
                System.out.println("\n--- PASO 3: Cedula (cara frontal) ---");
                System.out.println("Muestre la cara frontal de su cedula a la camara.");
                updateStatus("Preparando captura de cédula frontal...", true);

                Mat cardProbe = documentCaptureService.captureFrontProbe();
                if (cardProbe == null) {
                    rollbackComplete(user, "Captura del documento frontal cancelada.");
                    window.close();
                    return;
                }

                String frontPath = documentCaptureService.saveCard(cardProbe, user.getNationalId(), Constants.FRONT_SUFFIX);
                if (frontPath == null) {
                    rollbackWithFace(user, "No se pudo guardar el documento frontal.");
                    window.close();
                    return;
                }
                documentRecognizer.enroll(user.getNationalId(), cardProbe);
                System.out.println("Frontal guardada: " + frontPath);

                // PASO 4: Captura de cédula posterior
                System.out.println("\n--- PASO 4: Cedula (cara posterior) ---");
                System.out.println("Muestre la cara posterior de su cedula a la camara.");
                updateStatus("Preparando captura de cédula posterior...", true);

                Mat backProbe = documentCaptureService.captureBackProbe(user.getNationalId());
                if (backProbe == null) {
                    rollbackComplete(user, "Captura del documento posterior cancelada.");
                    window.close();
                    return;
                }

                String backPath = documentCaptureService.saveCard(backProbe, user.getNationalId(), Constants.BACK_SUFFIX);
                if (backPath == null) {
                    rollbackComplete(user, "No se pudo guardar el documento posterior.");
                    window.close();
                    return;
                }
                System.out.println("Posterior guardada: " + backPath);

                // Éxito
                System.out.println("\nREGISTRO COMPLETO: " + user.getFullName() + " (cedula " + user.getNationalId() + ")");

                showAlert("✓ Registro Exitoso",
                        "Usuario " + user.getFullName() + " registrado correctamente.\n\n" +
                                "Rostro: ✓\n" +
                                "Cédula Frontal: ✓\n" +
                                "Cédula Posterior: ✓");

                updateStatus("Registro completado: " + user.getFullName(), true);
                updateUserCount();
                window.close();

            } catch (Exception ex) {
                System.err.println("Error: " + ex.getMessage());
                ex.printStackTrace();
                showAlert("Error", "No se pudo registrar: " + ex.getMessage());
                updateStatus("Error en registro", false);
            }
        });

        container.getChildren().addAll(titleLabel, scrollPane, buttonBox);

        Scene scene = new Scene(container);
        window.setScene(scene);
        window.showAndWait();
    }

    // ========== ACCESO POR ROSTRO ==========
    private void openAccessByFaceWindow() {
        Stage window = new Stage();
        window.setTitle("Acceso por Rostro");
        window.setWidth(500);
        window.setHeight(350);
        window.initOwner(primaryStage);

        VBox container = new VBox(20);
        container.setPadding(new Insets(30));
        container.setAlignment(Pos.CENTER);
        container.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Verificación por Rostro");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        titleLabel.setTextFill(Color.web("#1a1a2e"));

        Label instructionLabel = new Label("Mire hacia la cámara.\nLa captura se realizará automáticamente en 3 segundos.");
        instructionLabel.setFont(Font.font("Segoe UI", 14));
        instructionLabel.setTextFill(Color.web("#666666"));
        instructionLabel.setWrapText(true);

        Button captureButton = createStyledButton("Iniciar Captura");
        captureButton.setStyle("-fx-padding: 15; -fx-font-size: 14; -fx-background-color: #00aa44; -fx-text-fill: white; -fx-font-weight: bold;");

        Button cancelButton = createStyledButton("Cancelar");
        cancelButton.setOnAction(e -> window.close());

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(captureButton, cancelButton);

        captureButton.setOnAction(e -> {
            try {
                System.out.println("Mire a la camara. Capturara sola en 3, 2, 1.");
                updateStatus("Capturando rostro...", true);

                Mat probe = faceRecognitionService.captureProbe();
                if (probe == null) {
                    System.out.println("Verificacion cancelada.");
                    updateStatus("Captura cancelada", false);
                    return;
                }

                User user = accessControlService.accessByFace(probe);
                probe.release();

                System.out.println("ACCESO PERMITIDO. Bienvenido(a), " + user.getFullName() + ".");
                showAlert("✓ Acceso Permitido", "¡Bienvenido(a), " + user.getFullName() + "!");
                updateStatus("Acceso permitido: " + user.getFullName(), true);
                window.close();

            } catch (AccessDeniedException ex) {
                System.out.println(ex.getMessage());
                showAlert("✗ Acceso Denegado", ex.getMessage());
                updateStatus("Acceso denegado", false);
            } catch (Exception ex) {
                System.err.println("Error: " + ex.getMessage());
                ex.printStackTrace();
                showAlert("Error", ex.getMessage());
                updateStatus("Error", false);
            }
        });

        container.getChildren().addAll(titleLabel, instructionLabel, buttonBox);

        Scene scene = new Scene(container);
        window.setScene(scene);
        window.showAndWait();
    }

    // ========== ACCESO POR DOCUMENTO ==========
    private void openAccessByDocumentWindow() {
        Stage window = new Stage();
        window.setTitle("Acceso por Cédula");
        window.setWidth(500);
        window.setHeight(350);
        window.initOwner(primaryStage);

        VBox container = new VBox(20);
        container.setPadding(new Insets(30));
        container.setAlignment(Pos.CENTER);
        container.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Verificación por Cédula");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        titleLabel.setTextFill(Color.web("#1a1a2e"));

        Label instructionLabel = new Label("Muestre la cara frontal de su cédula a la cámara.\nLa captura se realizará automáticamente.");
        instructionLabel.setFont(Font.font("Segoe UI", 14));
        instructionLabel.setTextFill(Color.web("#666666"));
        instructionLabel.setWrapText(true);

        Button captureButton = createStyledButton("Iniciar Captura");
        captureButton.setStyle("-fx-padding: 15; -fx-font-size: 14; -fx-background-color: #0066cc; -fx-text-fill: white; -fx-font-weight: bold;");

        Button cancelButton = createStyledButton("Cancelar");
        cancelButton.setOnAction(e -> window.close());

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(captureButton, cancelButton);

        captureButton.setOnAction(e -> {
            try {
                System.out.println("Muestre la cara frontal de su cedula a la camara.");
                updateStatus("Capturando documento...", true);

                Mat cardProbe = documentCaptureService.captureFrontProbe();
                if (cardProbe == null) {
                    System.out.println("Verificacion cancelada.");
                    updateStatus("Captura cancelada", false);
                    return;
                }

                User user = accessControlService.accessByDocument(cardProbe);
                cardProbe.release();

                System.out.println("ACCESO PERMITIDO. Bienvenido(a), " + user.getFullName() + ".");
                showAlert("✓ Acceso Permitido", "¡Bienvenido(a), " + user.getFullName() + "!");
                updateStatus("Acceso permitido: " + user.getFullName(), true);
                window.close();

            } catch (AccessDeniedException ex) {
                System.out.println(ex.getMessage());
                showAlert("✗ Acceso Denegado", ex.getMessage());
                updateStatus("Acceso denegado", false);
            } catch (Exception ex) {
                System.err.println("Error: " + ex.getMessage());
                ex.printStackTrace();
                showAlert("Error", ex.getMessage());
                updateStatus("Error", false);
            }
        });

        container.getChildren().addAll(titleLabel, instructionLabel, buttonBox);

        Scene scene = new Scene(container);
        window.setScene(scene);
        window.showAndWait();
    }

    // ========== DOBLE VERIFICACIÓN ==========
    private void openAccessByFaceAndDocumentWindow() {
        Stage window = new Stage();
        window.setTitle("Doble Verificación");
        window.setWidth(500);
        window.setHeight(400);
        window.initOwner(primaryStage);

        VBox container = new VBox(20);
        container.setPadding(new Insets(30));
        container.setAlignment(Pos.TOP_CENTER);
        container.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Doble Verificación (Rostro + Cédula)");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        titleLabel.setTextFill(Color.web("#1a1a2e"));

        VBox stepContainer = new VBox(15);
        stepContainer.getChildren().addAll(
                createStepBox("Paso 1: Captura del Rostro", "Mire hacia la cámara"),
                createStepBox("Paso 2: Captura de Cédula", "Muestre la cara frontal de su cédula")
        );

        Button startButton = createStyledButton("Iniciar Verificación");
        startButton.setStyle("-fx-padding: 15; -fx-font-size: 14; -fx-background-color: #ff6b35; -fx-text-fill: white; -fx-font-weight: bold;");

        Button cancelButton = createStyledButton("Cancelar");
        cancelButton.setOnAction(e -> window.close());

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(cancelButton, startButton);

        startButton.setOnAction(e -> {
            try {
                System.out.println("Paso 1 de 2: verificacion del rostro.");
                updateStatus("Paso 1: Capturando rostro...", true);

                Mat faceProbe = faceRecognitionService.captureProbe();
                if (faceProbe == null) {
                    System.out.println("Verificacion cancelada.");
                    updateStatus("Captura cancelada", false);
                    return;
                }

                System.out.println("Paso 2 de 2: muestre la cara frontal de su cedula.");
                updateStatus("Paso 2: Capturando documento...", true);

                Mat cardProbe = documentCaptureService.captureFrontProbe();
                if (cardProbe == null) {
                    System.out.println("Verificacion cancelada.");
                    faceProbe.release();
                    updateStatus("Captura cancelada", false);
                    return;
                }

                User user = accessControlService.accessByFaceAndDocument(faceProbe, cardProbe);
                faceProbe.release();
                cardProbe.release();

                System.out.println("ACCESO PERMITIDO. Bienvenido(a), " + user.getFullName() + ".");
                showAlert("✓ Acceso Permitido", "¡Bienvenido(a), " + user.getFullName() + "!\nDoble verificación exitosa.");
                updateStatus("Acceso permitido (doble verificación): " + user.getFullName(), true);
                window.close();

            } catch (AccessDeniedException ex) {
                System.out.println(ex.getMessage());
                showAlert("✗ Acceso Denegado", ex.getMessage());
                updateStatus("Acceso denegado", false);
            } catch (Exception ex) {
                System.err.println("Error: " + ex.getMessage());
                ex.printStackTrace();
                showAlert("Error", ex.getMessage());
                updateStatus("Error", false);
            }
        });

        container.getChildren().addAll(titleLabel, stepContainer, buttonBox);

        Scene scene = new Scene(container);
        window.setScene(scene);
        window.showAndWait();
    }

    // ========== VER HISTORIAL ==========
    private void openHistoryWindow() {
        Stage window = new Stage();
        window.setTitle("Historial de Accesos");
        window.setWidth(700);
        window.setHeight(600);
        window.initOwner(primaryStage);

        VBox container = new VBox(20);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Historial de Accesos");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        titleLabel.setTextFill(Color.web("#1a1a2e"));

        VBox historyBox = new VBox(8);
        historyBox.setPadding(new Insets(15));
        historyBox.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1;");

        List<AccessRecord> records = historyService.getAll();

        if (records.isEmpty()) {
            Label emptyLabel = new Label("No hay registros de acceso");
            emptyLabel.setFont(Font.font("Segoe UI", 14));
            emptyLabel.setTextFill(Color.web("#999999"));
            historyBox.getChildren().add(emptyLabel);
        } else {
            for (AccessRecord record : records) {
                HBox recordRow = new HBox(10);
                recordRow.setPadding(new Insets(12));
                recordRow.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0; -fx-background-color: #f9f9f9;");

                Label recordLabel = new Label(record.toString());
                recordLabel.setFont(Font.font("Segoe UI", 11));
                recordLabel.setTextFill(Color.web("#333333"));
                recordLabel.setWrapText(true);
                HBox.setHgrow(recordLabel, Priority.ALWAYS);

                recordRow.getChildren().add(recordLabel);
                historyBox.getChildren().add(recordRow);
            }
        }

        ScrollPane scrollPane = new ScrollPane(historyBox);
        scrollPane.setFitToWidth(true);

        Button closeButton = createStyledButton("Cerrar");
        closeButton.setOnAction(e -> window.close());

        container.getChildren().addAll(titleLabel, scrollPane, closeButton);

        Scene scene = new Scene(container);
        window.setScene(scene);
        window.showAndWait();
    }

    // ========== VER USUARIOS ==========
    private void openUsersWindow() {
        Stage window = new Stage();
        window.setTitle("Usuarios Registrados");
        window.setWidth(700);
        window.setHeight(600);
        window.initOwner(primaryStage);

        VBox container = new VBox(20);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Usuarios Registrados en el Sistema");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        titleLabel.setTextFill(Color.web("#1a1a2e"));

        VBox usersList = new VBox(10);
        usersList.setPadding(new Insets(10));

        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            Label emptyLabel = new Label("No hay usuarios registrados");
            emptyLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #999999;");
            usersList.getChildren().add(emptyLabel);
        } else {
            for (User user : users) {
                HBox userRow = new HBox(15);
                userRow.setPadding(new Insets(15));
                userRow.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0; -fx-background-color: #f9f9f9;");

                Label nameLabel = new Label(user.getFullName());
                nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
                nameLabel.setTextFill(Color.web("#1a1a2e"));

                Label cedulaLabel = new Label("Cédula: " + user.getNationalId());
                cedulaLabel.setFont(Font.font("Segoe UI", 12));
                cedulaLabel.setTextFill(Color.web("#666666"));

                VBox infoVBox = new VBox(5);
                infoVBox.getChildren().addAll(nameLabel, cedulaLabel);
                HBox.setHgrow(infoVBox, Priority.ALWAYS);

                Label statusLabel = new Label(user.hasFaceEnrolled() ? "✓ Registrado" : "✗ Incompleto");
                statusLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
                statusLabel.setTextFill(user.hasFaceEnrolled() ? Color.web("#00aa44") : Color.web("#ff6b6b"));

                userRow.getChildren().addAll(infoVBox, statusLabel);
                usersList.getChildren().add(userRow);
            }
        }

        ScrollPane scrollPane = new ScrollPane(usersList);
        scrollPane.setFitToWidth(true);

        Button closeButton = createStyledButton("Cerrar");
        closeButton.setOnAction(e -> window.close());

        container.getChildren().addAll(titleLabel, scrollPane, closeButton);

        Scene scene = new Scene(container);
        window.setScene(scene);
        window.showAndWait();
    }

    // ========== ELIMINAR USUARIO ==========
    private void openDeleteUserWindow() {
        Stage window = new Stage();
        window.setTitle("Eliminar Usuario");
        window.setWidth(500);
        window.setHeight(350);
        window.initOwner(primaryStage);

        VBox container = new VBox(20);
        container.setPadding(new Insets(30));
        container.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Eliminar Usuario");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        titleLabel.setTextFill(Color.web("#1a1a2e"));

        Label instructionLabel = new Label("Ingrese el número de cédula del usuario a eliminar:");
        instructionLabel.setFont(Font.font("Segoe UI", 12));

        TextField cedulaField = createStyledTextField("Número de cédula");

        Button deleteButton = createStyledButton("Eliminar Usuario");
        deleteButton.setStyle("-fx-padding: 12; -fx-font-size: 14; -fx-background-color: #ff4444; -fx-text-fill: white; -fx-font-weight: bold;");

        Button cancelButton = createStyledButton("Cancelar");
        cancelButton.setOnAction(e -> window.close());

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.getChildren().addAll(cancelButton, deleteButton);

        deleteButton.setOnAction(e -> {
            String cedula = cedulaField.getText().trim();
            if (cedula.isEmpty()) {
                showAlert("Error", "Por favor ingrese un número de cédula");
                return;
            }

            User user = userRepository.findByNationalId(cedula).orElse(null);
            if (user == null) {
                showAlert("Error", "No existe usuario con cédula: " + cedula);
                return;
            }

            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirmar eliminación");
            confirmAlert.setHeaderText("¿Está seguro?");
            confirmAlert.setContentText("Se eliminará a " + user.getFullName() + " y todos sus datos biométricos.");

            if (confirmAlert.showAndWait().get() == ButtonType.OK) {
                try {
                    userRepository.deleteByNationalId(cedula);
                    faceRecognizer.remove(cedula);
                    documentRecognizer.remove(cedula);
                    deletePhoto(cedula, Constants.FACE_SUFFIX);
                    deletePhoto(cedula, Constants.FRONT_SUFFIX);
                    deletePhoto(cedula, Constants.BACK_SUFFIX);

                    showAlert("Éxito", "Usuario eliminado correctamente");
                    updateUserCount();
                    updateStatus("Usuario eliminado", true);
                    window.close();
                } catch (Exception ex) {
                    showAlert("Error", "No se pudo eliminar: " + ex.getMessage());
                    updateStatus("Error al eliminar", false);
                }
            }
        });

        container.getChildren().addAll(titleLabel, instructionLabel, cedulaField, buttonBox);

        Scene scene = new Scene(container);
        window.setScene(scene);
        window.showAndWait();
    }

    // ========== UTILIDADES ==========

    private TextField createStyledTextField(String promptText) {
        TextField field = new TextField();
        field.setPromptText(promptText);
        field.setStyle("-fx-padding: 12; -fx-font-size: 12; -fx-border-color: #ddd; -fx-border-radius: 4;");
        field.setPrefHeight(40);
        return field;
    }

    private Button createStyledButton(String text) {
        Button button = new Button(text);
        button.setStyle("-fx-padding: 10; -fx-font-size: 12; -fx-border-radius: 4;");
        button.setCursor(javafx.scene.Cursor.HAND);
        return button;
    }

    private VBox createStepBox(String title, String description) {
        VBox stepBox = new VBox(5);
        stepBox.setPadding(new Insets(15));
        stepBox.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 4; -fx-background-color: #f9f9f9;");

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        titleLabel.setTextFill(Color.web("#1a1a2e"));

        Label descLabel = new Label(description);
        descLabel.setFont(Font.font("Segoe UI", 12));
        descLabel.setTextFill(Color.web("#666666"));

        stepBox.getChildren().addAll(titleLabel, descLabel);
        return stepBox;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Métodos de rollback del Main.java original
    private void rollback(User user, String reason) {
        userRepository.deleteByNationalId(user.getNationalId());
        System.out.println(reason + " Registro anulado.");
    }

    private void rollbackWithFace(User user, String reason) {
        faceRecognizer.remove(user.getNationalId());
        deletePhoto(user.getNationalId(), Constants.FACE_SUFFIX);
        rollback(user, reason);
    }

    private void rollbackComplete(User user, String reason) {
        faceRecognizer.remove(user.getNationalId());
        documentRecognizer.remove(user.getNationalId());
        deletePhoto(user.getNationalId(), Constants.FACE_SUFFIX);
        deletePhoto(user.getNationalId(), Constants.FRONT_SUFFIX);
        deletePhoto(user.getNationalId(), Constants.BACK_SUFFIX);
        rollback(user, reason);
    }

    private void deletePhoto(String nationalId, String suffix) {
        File file = new File(Constants.PHOTOS_DIR + File.separator + nationalId + suffix);
        if (file.exists() && !file.delete()) {
            System.out.println("Advertencia: no se pudo borrar " + file.getPath());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}