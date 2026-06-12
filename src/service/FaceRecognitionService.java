package service;

import exception.FaceNotDetectedException;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import util.Constants;
import vision.CameraCapture;
import vision.FaceDetector;
import vision.FaceRecognizer;
import vision.FaceValidationResult;
import vision.ImageProcessor;
import vision.OverlayRenderer;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.concurrent.CountDownLatch;

public class FaceRecognitionService {

    private final FaceDetector faceDetector;
    private final ImageProcessor imageProcessor;
    private final FaceRecognizer faceRecognizer;

    private volatile boolean running;
    private volatile Mat capturedFrame;

    private JFrame window;
    private JLabel videoLabel;

    public FaceRecognitionService(FaceDetector faceDetector,
                                  ImageProcessor imageProcessor,
                                  FaceRecognizer faceRecognizer) {
        this.faceDetector = faceDetector;
        this.imageProcessor = imageProcessor;
        this.faceRecognizer = faceRecognizer;
    }

    /**
     * Captura para REGISTRO con conteo regresivo automático.
     */
    public String captureFace(String nationalId)
            throws FaceNotDetectedException {

        Mat frame = captureValidatedFrame(
                "Registro facial  |  Cedula " + nationalId);

        if (frame == null) {
            return null;
        }

        String path = Constants.PHOTOS_DIR
                + File.separator
                + nationalId
                + Constants.FACE_SUFFIX;

        boolean saved = imageProcessor.save(frame, path);
        frame.release();

        if (!saved) {
            return null;
        }

        faceRecognizer.enrollFromFile(nationalId, path);
        return path;
    }


    /**
     * Guarda un rostro ya capturado y lo enrola para reconocimiento.
     * Se usa en el registro, DESPUÉS de verificar duplicados.
     */
    public String saveAndEnroll(String nationalId, Mat face)
            throws FaceNotDetectedException {

        String path = Constants.PHOTOS_DIR
                + File.separator
                + nationalId
                + Constants.FACE_SUFFIX;

        if (!imageProcessor.save(face, path)) {
            return null;
        }

        faceRecognizer.enrollFromFile(nationalId, path);
        return path;
    }

    /**
     * Captura para INGRESO con conteo regresivo automático.
     */
    public Mat captureProbe() {
        return captureValidatedFrame("Verificacion facial");
    }

    private Mat captureValidatedFrame(String title) {

        CameraCapture camera =
                new CameraCapture(Constants.CAMERA_INDEX);

        if (!camera.isOpened()) {
            throw new IllegalStateException(
                    "No se pudo abrir la cámara.");
        }

        camera.stabilize(Constants.STABILIZATION_FRAMES, 50);

        running = true;
        capturedFrame = null;

        CountDownLatch finished = new CountDownLatch(1);

        SwingUtilities.invokeLater(() -> createWindow(title));

        Thread loop = new Thread(
                () -> captureLoop(camera, title, finished),
                "face-capture-loop");
        loop.start();

        try {
            finished.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
        }

        return capturedFrame;
    }

    private void createWindow(String title) {
        window = new JFrame(title);
        videoLabel = new JLabel();
        window.getContentPane().add(videoLabel);
        OverlayRenderer.styleWindow(window, videoLabel);
        window.setSize(700, 580);
        window.setLocationRelativeTo(null);
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                running = false;
            }
        });

        window.setVisible(true);
    }

    private void captureLoop(CameraCapture camera,
                             String title,
                             CountDownLatch finished) {

        Mat frame = new Mat();
        Mat cleanFrame = new Mat();
        long countdownStart = -1;
        int badFrames = 0;

        try {
            while (running) {

                if (!camera.readFrame(frame)) {
                    continue;
                }

                // Quitar el modo espejo
                Core.flip(frame, frame, 1);
                frame.copyTo(cleanFrame);

                Rect guide = guideRect(frame);

                FaceValidationResult validation =
                        faceDetector.validateForCapture(frame);

                if (validation.isValid()
                        && !insideGuide(validation.getFace(), guide)) {
                    validation = FaceValidationResult.error(
                            "Ubique su rostro dentro del recuadro.");
                }

                boolean ready = validation.isValid();

                // ---- Conteo regresivo con margen de tolerancia ----
                // Un fotograma malo momentáneo (parpadeo del detector,
                // movimiento leve) NO reinicia el conteo: solo se
                // reinicia tras varios fotogramas malos seguidos.
                if (ready) {
                    badFrames = 0;
                    if (countdownStart < 0) {
                        countdownStart = System.currentTimeMillis();
                    }
                } else if (countdownStart >= 0) {
                    badFrames++;
                    if (badFrames > Constants.COUNTDOWN_GRACE_FRAMES) {
                        countdownStart = -1;
                        badFrames = 0;
                    }
                }

                int secondsLeft = 0;
                boolean counting = countdownStart >= 0;
                if (counting) {
                    long elapsed =
                            System.currentTimeMillis() - countdownStart;
                    secondsLeft = Constants.COUNTDOWN_SECONDS
                            - (int) (elapsed / 1000);

                    if (secondsLeft <= 0 && ready) {
                        capturedFrame = cleanFrame.submat(guide).clone();
                        running = false;
                    }
                }

                // ---- Interfaz ----
                OverlayRenderer.darkenOutside(frame, cleanFrame, guide);
                OverlayRenderer.drawCornerGuide(frame, guide,
                        ready ? OverlayRenderer.OK : OverlayRenderer.WHITE);
                OverlayRenderer.drawTopBanner(frame,
                        "Ubique su rostro dentro del recuadro");

                String status = ready
                        ? "Perfecto, no se mueva..."
                        : validation.getMessage();
                OverlayRenderer.drawBottomBanner(frame, status, ready);

                if (counting && secondsLeft > 0) {
                    OverlayRenderer.drawCountdown(frame, secondsLeft);
                }

                updateWindow(frame);

                try {
                    Thread.sleep(Constants.FRAME_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    running = false;
                }
            }
        } finally {
            camera.release();
            frame.release();
            cleanFrame.release();

            SwingUtilities.invokeLater(() -> {
                if (window != null) {
                    window.dispose();
                }
            });

            finished.countDown();
        }
    }

    private Rect guideRect(Mat frame) {
        int width = frame.cols();
        int height = frame.rows();

        int guideWidth = (int) (width * 0.45);
        int guideHeight = (int) (height * 0.70);
        int x = (width - guideWidth) / 2;
        int y = (height - guideHeight) / 2;

        return new Rect(x, y, guideWidth, guideHeight);
    }

    private boolean insideGuide(Rect face, Rect guide) {
        int tolerance = Constants.GUIDE_TOLERANCE_PX;
        return face.x >= guide.x - tolerance
                && face.y >= guide.y - tolerance
                && face.x + face.width <= guide.x + guide.width + tolerance
                && face.y + face.height <= guide.y + guide.height + tolerance;
    }

    private void updateWindow(Mat frame) {
        if (videoLabel == null) {
            return;
        }

        java.awt.image.BufferedImage image =
                OverlayRenderer.toBufferedImage(frame);

        SwingUtilities.invokeLater(() -> {
            videoLabel.setIcon(new ImageIcon(image));
            videoLabel.repaint();
        });
    }
}