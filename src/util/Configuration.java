package util;

public final class Configuration {

    private static String openCvLibraryPath =
            "C:\\opencv\\build\\java\\x64\\opencv_java4110.dll";

    private static boolean loaded = false;

    private Configuration() {
    }

    public static void setOpenCvLibraryPath(String path) {
        openCvLibraryPath = path;
    }

    public static synchronized void loadOpenCv() {
        if (loaded) {
            return;
        }
        System.load(openCvLibraryPath);
        loaded = true;
    }

    public static boolean isOpenCvLoaded() {
        return loaded;
    }
}
