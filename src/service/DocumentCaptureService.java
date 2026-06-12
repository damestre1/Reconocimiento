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

    /**
     * Captura la cara FRONTAL sin guardarla (el llamador decide
     * guardar con saveCard tras verificar duplicados).
     */
    public Mat captureFrontProbe() {
        return capture(DocumentSide.FRONT,
                "Encaje la cara FRONTAL de la cedula",
                null);
    }

    /**
     * Captura la cara POSTERIOR. Compara cada captura contra la
     * frontal ya guardada del usuario: si es la misma cara, la
     * rechaza y pide voltear el documento.
     */
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

    /** Guarda una captura como photos/cedula_sufijo.jpg. */
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

                Rect guide = cardGuideRect(frame);

                // Validación en vivo
                Mat card = cleanFrame.submat(guide);
                String error = scanner.checkDocument(card, side);

                // La posterior no puede ser la frontal repetida.
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

                // ---- Interfaz ----
                OverlayRenderer.darkenOutside(frame, cleanFrame, guide);
                OverlayRenderer.drawCornerGuide(frame, guide,
                        ready ? OverlayRenderer.OK : OverlayRenderer.WHITE);
                OverlayRenderer.drawTopBanner(frame, instruction);

                String status = ready
                        ? "Perfecto, mantenga la cedula quieta..."
                        : error;
                OverlayRenderer.drawBottomBanner(frame, status, ready);

                if (counting && secondsLeft > 0 && running) {
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