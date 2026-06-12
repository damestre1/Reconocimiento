package vision;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import util.Constants;

import java.io.File;

public class ImageProcessor {

    public Mat toGray(Mat source) {
        Mat gray = new Mat();
        if (source.channels() == 1) {
            source.copyTo(gray);
        } else {
            Imgproc.cvtColor(source, gray, Imgproc.COLOR_BGR2GRAY);
        }
        return gray;
    }

    public double brightness(Mat source) {
        Mat gray = toGray(source);
        double mean = Core.mean(gray).val[0];
        gray.release();
        return mean;
    }

    public boolean isDark(Mat source) {
        return brightness(source) < Constants.MIN_BRIGHTNESS;
    }

    public double blurVariance(Mat source) {
        Mat gray = toGray(source);
        Mat laplacian = new Mat();
        Imgproc.Laplacian(gray, laplacian, CvType.CV_64F);

        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stdDev = new MatOfDouble();
        Core.meanStdDev(laplacian, mean, stdDev);

        double variance = Math.pow(stdDev.get(0, 0)[0], 2);

        gray.release();
        laplacian.release();
        mean.release();
        stdDev.release();

        return variance;
    }

    public boolean isBlurry(Mat source) {
        return blurVariance(source) < Constants.MIN_BLUR_VARIANCE;
    }

    /** Proporción de píxeles que son borde (0.0 - 1.0). */
    public double edgeDensity(Mat source) {
        Mat gray = toGray(source);
        Mat edges = new Mat();
        Imgproc.Canny(gray, edges, 60, 160);

        double density = (double) Core.countNonZero(edges)
                / (edges.cols() * edges.rows());

        gray.release();
        edges.release();
        return density;
    }

    /**
     * Proporción de píxeles dentro de un rango HSV (0.0 - 1.0).
     * H: 0-180, S: 0-255, V: 0-255.
     */
    public double colorRatio(Mat source,
                             Scalar lowerHsv,
                             Scalar upperHsv) {
        Mat hsv = new Mat();
        Imgproc.cvtColor(source, hsv, Imgproc.COLOR_BGR2HSV);

        Mat mask = new Mat();
        Core.inRange(hsv, lowerHsv, upperHsv, mask);

        double ratio = (double) Core.countNonZero(mask)
                / (mask.cols() * mask.rows());

        hsv.release();
        mask.release();
        return ratio;
    }

    /**
     * Rojo: ocupa dos rangos de matiz en HSV. Rangos amplios y con
     * saturación baja para tolerar cámaras de baja calidad que
     * "lavan" los colores.
     */
    public double redRatio(Mat source) {
        return colorRatio(source,
                new Scalar(0, 45, 50), new Scalar(12, 255, 255))
                + colorRatio(source,
                new Scalar(166, 45, 50), new Scalar(180, 255, 255));
    }

    public double yellowRatio(Mat source) {
        return colorRatio(source,
                new Scalar(15, 50, 90), new Scalar(38, 255, 255));
    }

    public double blueRatio(Mat source) {
        return colorRatio(source,
                new Scalar(90, 45, 45), new Scalar(135, 255, 255));
    }

    /**
     * Proporción de píxeles claros y poco saturados (fondo blanco).
     * La cédula digital es mayormente blanca; una tarjeta bancaria
     * oscura o una licencia verdosa no lo son.
     */
    public double whitenessRatio(Mat source) {
        Mat hsv = new Mat();
        Imgproc.cvtColor(source, hsv, Imgproc.COLOR_BGR2HSV);

        Mat mask = new Mat();
        Core.inRange(hsv,
                new Scalar(0, 0, 150),
                new Scalar(180, 60, 255), mask);

        double ratio = (double) Core.countNonZero(mask)
                / (mask.cols() * mask.rows());

        hsv.release();
        mask.release();
        return ratio;
    }

    public boolean hasMinimumSize(Mat source) {
        return source.cols() >= Constants.MIN_IMAGE_WIDTH
                && source.rows() >= Constants.MIN_IMAGE_HEIGHT;
    }

    public boolean isLandscape(Mat source) {
        return source.cols() > source.rows();
    }

    public Mat normalizeFace(Mat faceRegion) {
        Mat gray = toGray(faceRegion);
        Mat resized = new Mat();
        Imgproc.resize(
                gray,
                resized,
                new Size(Constants.FACE_SAMPLE_SIZE, Constants.FACE_SAMPLE_SIZE)
        );
        Imgproc.equalizeHist(resized, resized);
        gray.release();
        return resized;
    }

    public boolean save(Mat image, String path) {
        File parent = new File(path).getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            return false;
        }
        return Imgcodecs.imwrite(path, image);
    }

    public Mat load(String path) {
        return Imgcodecs.imread(path);
    }
}