package vision;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

/**
 * Dibuja la interfaz visual sobre el video de la cámara:
 * máscara oscura, esquinas guía, barras superior e inferior
 * y el conteo regresivo.
 */
public final class OverlayRenderer {

    public static final Scalar OK = new Scalar(120, 220, 80);
    public static final Scalar ERROR = new Scalar(70, 70, 235);
    public static final Scalar WHITE = new Scalar(245, 245, 245);
    private static final Scalar BANNER_BG = new Scalar(20, 16, 12);

    private OverlayRenderer() {
    }

    public static void styleWindow(JFrame window, JLabel videoLabel) {
        window.getContentPane().setBackground(new Color(14, 16, 22));
        videoLabel.setHorizontalAlignment(JLabel.CENTER);
        videoLabel.setVerticalAlignment(JLabel.CENTER);
        videoLabel.setBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10));
        window.setResizable(false);
    }

    /** Oscurece todo lo que está fuera del recuadro guía. */
    public static void darkenOutside(Mat frame, Mat clean, Rect guide) {
        Core.multiply(frame, new Scalar(0.22, 0.22, 0.22), frame);
        Mat src = clean.submat(guide);
        Mat dst = frame.submat(guide);
        src.copyTo(dst);
        src.release();
        dst.release();
    }

    /** Esquinas tipo escáner en lugar de un rectángulo completo. */
    public static void drawCornerGuide(Mat frame, Rect g, Scalar color) {
        int len = Math.min(g.width, g.height) / 5;
        int t = 4;

        Point tl = new Point(g.x, g.y);
        Point tr = new Point(g.x + g.width, g.y);
        Point bl = new Point(g.x, g.y + g.height);
        Point br = new Point(g.x + g.width, g.y + g.height);

        // Superior izquierda
        line(frame, tl, new Point(tl.x + len, tl.y), color, t);
        line(frame, tl, new Point(tl.x, tl.y + len), color, t);
        // Superior derecha
        line(frame, tr, new Point(tr.x - len, tr.y), color, t);
        line(frame, tr, new Point(tr.x, tr.y + len), color, t);
        // Inferior izquierda
        line(frame, bl, new Point(bl.x + len, bl.y), color, t);
        line(frame, bl, new Point(bl.x, bl.y - len), color, t);
        // Inferior derecha
        line(frame, br, new Point(br.x - len, br.y), color, t);
        line(frame, br, new Point(br.x, br.y - len), color, t);

        // Línea fina completa, muy sutil
        Imgproc.rectangle(frame, tl, br,
                new Scalar(color.val[0] * 0.5,
                        color.val[1] * 0.5,
                        color.val[2] * 0.5),
                1, Imgproc.LINE_AA);
    }

    /** Barra superior con el título / instrucción principal. */
    public static void drawTopBanner(Mat frame, String text) {
        banner(frame, new Rect(0, 0, frame.cols(), 48));
        Imgproc.putText(frame, text,
                new Point(18, 31),
                Imgproc.FONT_HERSHEY_SIMPLEX,
                0.62, WHITE, 2, Imgproc.LINE_AA);
    }

    /** Barra inferior de estado con indicador de color. */
    public static void drawBottomBanner(Mat frame,
                                        String text,
                                        boolean ready) {
        int h = frame.rows();
        banner(frame, new Rect(0, h - 46, frame.cols(), 46));

        Scalar color = ready ? OK : ERROR;

        Imgproc.circle(frame,
                new Point(24, h - 23), 8, color, -1, Imgproc.LINE_AA);

        Imgproc.putText(frame, text,
                new Point(44, h - 16),
                Imgproc.FONT_HERSHEY_SIMPLEX,
                0.56, color, 2, Imgproc.LINE_AA);
    }

    /** Número grande del conteo regresivo en el centro. */
    public static void drawCountdown(Mat frame, int secondsLeft) {
        Point center = new Point(frame.cols() / 2.0, frame.rows() / 2.0);

        Mat overlay = frame.clone();
        Imgproc.circle(overlay, center, 58,
                new Scalar(20, 16, 12), -1, Imgproc.LINE_AA);
        Core.addWeighted(overlay, 0.55, frame, 0.45, 0, frame);
        overlay.release();

        Imgproc.circle(frame, center, 58, OK, 3, Imgproc.LINE_AA);

        String number = String.valueOf(secondsLeft);
        Imgproc.putText(frame, number,
                new Point(center.x - 22, center.y + 25),
                Imgproc.FONT_HERSHEY_SIMPLEX,
                2.4, WHITE, 6, Imgproc.LINE_AA);
    }

    public static BufferedImage toBufferedImage(Mat mat) {
        int type = (mat.channels() == 1)
                ? BufferedImage.TYPE_BYTE_GRAY
                : BufferedImage.TYPE_3BYTE_BGR;

        BufferedImage image = new BufferedImage(
                mat.cols(), mat.rows(), type);

        byte[] data = ((DataBufferByte) image.getRaster()
                .getDataBuffer()).getData();

        mat.get(0, 0, data);
        return image;
    }

    private static void banner(Mat frame, Rect r) {
        Mat overlay = frame.clone();
        Imgproc.rectangle(overlay,
                new Point(r.x, r.y),
                new Point(r.x + r.width, r.y + r.height),
                BANNER_BG, -1);
        Core.addWeighted(overlay, 0.62, frame, 0.38, 0, frame);
        overlay.release();
    }

    private static void line(Mat frame, Point a, Point b,
                             Scalar color, int thickness) {
        Imgproc.line(frame, a, b, color, thickness, Imgproc.LINE_AA);
    }
}
