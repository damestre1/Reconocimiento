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

    /**
     * Proporción de píxeles que son borde (0.0 - 1.0). Una cédula
     * tiene mucho texto y gráficos, por lo que su densidad de bordes
     * es alta; un fondo vacío (pared, mesa) da valores muy bajos.
     */
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
     * Proporción de píxeles rojos en la región (0.0 - 1.0).
     * Se usa para detectar el encabezado rojo "REPÚBLICA DE COLOMBIA"
     * de la cédula digital colombiana.
     */
    public double redRatio(Mat source) {
        Mat hsv = new Mat();
        Imgproc.cvtColor(source, hsv, Imgproc.COLOR_BGR2HSV);

        // El rojo en HSV ocupa dos rangos de matiz (0-10 y 170-180)
        Mat mask1 = new Mat();
        Mat mask2 = new Mat();
        Core.inRange(hsv,
                new Scalar(0, 80, 70),
                new Scalar(10, 255, 255), mask1);
        Core.inRange(hsv,
                new Scalar(170, 80, 70),
                new Scalar(180, 255, 255), mask2);

        Mat mask = new Mat();
        Core.bitwise_or(mask1, mask2, mask);

        double ratio = (double) Core.countNonZero(mask)
                / (mask.cols() * mask.rows());

        hsv.release();
        mask1.release();
        mask2.release();
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