package service;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import util.Constants;
import vision.CameraCapture;
import vision.ImageProcessor;
import vision.NationalIdScanner;
import vision.NationalIdScanner.DocumentSide;
import vision.OverlayRenderer;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.concurrent.CountDownLatch;

public class DocumentCaptureService {

    // Proporción real de una cédula (85.6 mm x 54 mm)
    private static final double CARD_ASPECT = 1.585;

    private final NationalIdScanner scanner;
    private final ImageProcessor imageProcessor;

    private volatile boolean running;
    private volatile Mat capturedCard;

    private JFrame window;
    private JLabel videoLabel;

    public DocumentCaptureService(NationalIdScanner scanner,
                                  ImageProcessor imageProcessor) {
        this.scanner = scanner;
        this.imageProcessor = imageProcessor;
    }

    public Mat captureFrontProbe() {
        return capture(DocumentSide.FRONT,
                "Encaje la cara FRONTAL de la cedula",
                null);
    }

    public Mat captureBackProbe(String nationalId) {

        Mat frontRef = null;
        String frontPath = Constants.PHOTOS_DIR
                + File.separator
                + nationalId
                + Constants.FRONT_SUFFIX;

        Mat front = imageProcessor.load(frontPath);
        if (!front.empty()) {
            frontRef = scanner.normalizeCard(front);
        }
        front.release();

        Mat result = capture(DocumentSide.BACK,
                "Encaje la cara POSTERIOR de la cedula",
                frontRef);

        if (frontRef != null) {
            frontRef.release();
        }

        return result;
    }

    public String saveCard(Mat card, String nationalId, String suffix) {
        String path = Constants.PHOTOS_DIR
                + File.separator
                + nationalId
                + suffix;

        return imageProcessor.save(card, path) ? path : null;
    }

    private Mat capture(DocumentSide side,
                        String instruction,
                        Mat frontReference) {

        CameraCapture camera =
                new CameraCapture(Constants.CAMERA_INDEX);

        if (!camera.isOpened()) {
            throw new IllegalStateException(
                    "No se pudo abrir la cámara.");
        }

        camera.stabilize(Constants.STABILIZATION_FRAMES, 50);

        running = true;
        capturedCard = null;

        CountDownLatch finished = new CountDownLatch(1);

        SwingUtilities.invokeLater(() ->
                createWindow("Captura de cedula"));

        Thread loop = new Thread(
                () -> captureLoop(camera, side, instruction,
                        frontReference, finished),
                "document-capture-loop");
        loop.start();

        try {
            finished.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
        }

        return capturedCard;
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
                             DocumentSide side,
                             String instruction,
                             Mat frontReference,
                             CountDownLatch finished) {

        Mat raw = new Mat();
        Mat cleanFrame = new Mat();     // SIN espejo: validar y guardar
        Mat display = new Mat();        // CON espejo: solo para mostrar
        Mat displayClean = new Mat();
        long countdownStart = -1;
        int badFrames = 0;

        try {
            while (running) {

                if (!camera.readFrame(raw)) {
                    continue;
                }

                // El documento se valida y guarda SIN espejo, para que
                // la foto del titular quede a la izquierda y el texto
                // sea legible, tal como en la cédula real.
                raw.copyTo(cleanFrame);

                // La pantalla sí se muestra en espejo, porque así
                // mover la cédula resulta intuitivo para la persona.
                Core.flip(raw, display, 1);
                display.copyTo(displayClean);

                Rect guide = cardGuideRect(cleanFrame);

                // Validación en vivo sobre la imagen SIN espejo
                Mat card = cleanFrame.submat(guide);
                String error = scanner.checkDocument(card, side);

                if (error == null
                        && side == DocumentSide.BACK
                        && frontReference != null
                        && scanner.isSameAsFront(card, frontReference)) {
                    error = "Esa es la cara FRONTAL. Voltee la cedula.";
                }

                boolean ready = (error == null);

                // ---- Conteo con margen de tolerancia ----
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
                        capturedCard = card.clone();
                        running = false;
                    }
                }
                card.release();

                // ---- Interfaz (sobre la imagen en espejo) ----
                OverlayRenderer.darkenOutside(display, displayClean, guide);
                OverlayRenderer.drawCornerGuide(display, guide,
                        ready ? OverlayRenderer.OK : OverlayRenderer.WHITE);
                OverlayRenderer.drawTopBanner(display, instruction);

                String status = ready
                        ? "Perfecto, mantenga la cedula quieta..."
                        : error;
                OverlayRenderer.drawBottomBanner(display, status, ready);

                if (counting && secondsLeft > 0 && running) {
                    OverlayRenderer.drawCountdown(display, secondsLeft);
                }

                updateWindow(display);

                try {
                    Thread.sleep(Constants.FRAME_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    running = false;
                }
            }
        } finally {
            camera.release();
            raw.release();
            cleanFrame.release();
            display.release();
            displayClean.release();

            SwingUtilities.invokeLater(() -> {
                if (window != null) {
                    window.dispose();
                }
            });

            finished.countDown();
        }
    }

    private Rect cardGuideRect(Mat frame) {
        int width = frame.cols();
        int height = frame.rows();

        int guideWidth = (int) (width * 0.82);
        int guideHeight = (int) (guideWidth / CARD_ASPECT);

        if (guideHeight > height * 0.85) {
            guideHeight = (int) (height * 0.85);
            guideWidth = (int) (guideHeight * CARD_ASPECT);
        }

        int x = (width - guideWidth) / 2;
        int y = (height - guideHeight) / 2;

        return new Rect(x, y, guideWidth, guideHeight);
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