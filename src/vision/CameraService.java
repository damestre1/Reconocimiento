package vision;

import org.opencv.core.*;
import org.opencv.videoio.VideoCapture;
import org.opencv.highgui.HighGui;
import util.ImageUtil;

public class CameraService {

    private VideoCapture camera;
    private Mat frame;
    private FaceDetector detector;

    public CameraService() {
        camera = new VideoCapture(0);
        frame = new Mat();
        detector = new FaceDetector();
    }

    public String captureFace(String name) {

        System.out.println("Coloca tu rostro frente a la cámara....");

        while (true) {

            camera.read(frame);

            if (frame.empty()) continue;

            Core.flip(frame, frame, 1);

            Rect face = detector.detect(frame);

            HighGui.imshow("Camera", frame);

            if (face != null) {
                String path = ImageUtil.saveFace(frame, face, name);

                System.out.println("Cara capturada con éxito");

                HighGui.destroyAllWindows();
                return path;
            }

            if (HighGui.waitKey(30) == 27) break;
        }

        return null;
    }

    public void close() {
        camera.release();
        HighGui.destroyAllWindows();
    }
}