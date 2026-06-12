package vision;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

public class CameraCapture {

    private final VideoCapture camera;

    public CameraCapture(int cameraIndex) {
        this.camera = new VideoCapture(cameraIndex);
    }

    public boolean isOpened() {
        return camera.isOpened();
    }

    public boolean readFrame(Mat destination) {
        return camera.read(destination) && !destination.empty();
    }

    public void stabilize(int frames, int delayMillis) {
        Mat temp = new Mat();
        for (int i = 0; i < frames; i++) {
            camera.read(temp);
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        temp.release();
    }

    public void release() {
        if (camera.isOpened()) {
            camera.release();
        }
    }
}
