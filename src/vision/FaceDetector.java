package vision;

import org.opencv.core.*;
import org.opencv.objdetect.CascadeClassifier;

public class FaceDetector {

    private CascadeClassifier detector;

    public FaceDetector() {

        String path = "resources/haarcascade_frontalface_default.xml";

        detector = new CascadeClassifier(path);

        // 🔥 Validación importante
        if (detector.empty()) {
            System.out.println("ERROR: Face detector XML not loaded");
        } else {
            System.out.println("Detector de rostros cargado correctamente");
        }
    }

    public Rect detect(Mat frame) {

        if (detector.empty()) {
            System.out.println("Detector no inicializado");
            return null;
        }

        MatOfRect faces = new MatOfRect();
        detector.detectMultiScale(frame, faces);

        if (faces.toArray().length > 0) {
            return faces.toArray()[0];
        }

        return null;
    }
}