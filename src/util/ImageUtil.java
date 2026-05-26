package util;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;

public class ImageUtil {

    public static String saveFace(Mat frame, Rect face, String name) {

        Mat faceCrop = new Mat(frame, face);

        String path = "photos/" + name + "_" + System.currentTimeMillis() + ".jpg";

        Imgcodecs.imwrite(path, faceCrop);

        return path;
    }
}