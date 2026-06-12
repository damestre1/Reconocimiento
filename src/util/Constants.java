package util;

public final class Constants {

    private Constants() {
    }

    // Rutas
    public static final String PHOTOS_DIR = "photos";
    public static final String DATA_DIR = "data";
    public static final String USERS_FILE = "data/users.csv";
    public static final String FACE_SUFFIX = "_face.jpg";
    public static final String FRONT_SUFFIX = "_front.jpg";
    public static final String BACK_SUFFIX = "_back.jpg";
    public static final String FACE_CASCADE_PATH =
            "resources/haarcascade_frontalface_default.xml";
    public static final String EYES_CASCADE_PATH =
            "resources/haarcascade_eye.xml";

    // Cédula
    public static final int NATIONAL_ID_MIN_LENGTH = 6;
    public static final int NATIONAL_ID_MAX_LENGTH = 10;

    // Validación de datos personales
    public static final int NAME_MIN_LENGTH = 2;
    public static final int NAME_MAX_LENGTH = 40;
    public static final int PHONE_MIN_LENGTH = 7;
    public static final int PHONE_MAX_LENGTH = 10;

    // Cámara
    public static final int CAMERA_INDEX = 0;
    public static final int STABILIZATION_FRAMES = 30;
    public static final int FRAME_DELAY_MS = 33;

    // Captura automática
    public static final int COUNTDOWN_SECONDS = 3;
    public static final int COUNTDOWN_GRACE_FRAMES = 30;

    // Calidad de imagen
    public static final double MIN_BRIGHTNESS = 60.0;
    public static final double MIN_BLUR_VARIANCE = 80.0;
    public static final int MIN_IMAGE_WIDTH = 480;
    public static final int MIN_IMAGE_HEIGHT = 320;

    // Validación facial
    public static final double MIN_FACE_RATIO = 0.18;
    public static final double MAX_FACE_RATIO = 0.70;
    public static final double MAX_TILT_DEGREES = 25.0;
    public static final int EDGE_MARGIN_PX = 10;
    public static final int MIN_FACE_SIZE_PX = 80;
    public static final int GUIDE_TOLERANCE_PX = 50;

    // Reconocimiento facial (correlación de píxeles: -1 a 1)
    public static final int FACE_SAMPLE_SIZE = 200;
    public static final double RECOGNITION_THRESHOLD = 0.45;

    // Documento
    public static final double DOC_MIN_BRIGHTNESS = 45.0;
    public static final double DOC_MIN_BLUR = 20.0;
    public static final double DOC_MIN_EDGE_DENSITY = 0.035;
    public static final double DOC_MAX_CARD_FACE_RATIO = 0.50;
    // Foto del titular en la cédula: tamaño y posición esperados
    public static final double CARD_PHOTO_MIN_HEIGHT_RATIO = 0.25;
    public static final double CARD_PHOTO_MAX_HEIGHT_RATIO = 0.80;
    public static final double CARD_PHOTO_MAX_CENTER_X = 0.65;
    // Reconocimiento de documento (correlación de píxeles)
    public static final double DOC_RECOGNITION_THRESHOLD = 0.55;
    // Si la "posterior" se parece tanto a la frontal, es la misma cara
    public static final double DOC_SAME_SIDE_THRESHOLD = 0.70;

    // Marcas distintivas de la cédula digital colombiana
    // Frontal: fondo mayormente blanco
    public static final double CARD_WHITE_MIN_RATIO = 0.28;
    // Frontal: encabezado rojo "REPUBLICA DE COLOMBIA" (franja superior)
    public static final double CARD_RED_HEADER_MIN_RATIO = 0.008;
    // La frontal debe cumplir al menos N de las 3 marcas de color
    // (blanco, rojo, bandera). La foto del titular siempre es obligatoria.
    public static final int CARD_FRONT_MIN_MARKS = 2;
    // Posterior: zona MRZ con texto <<< (franja inferior)
    public static final double CARD_MRZ_MIN_DENSITY = 0.06;
}