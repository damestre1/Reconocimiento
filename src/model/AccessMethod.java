package model;

public enum AccessMethod {
    FACIAL("Reconocimiento facial"),
    CEDULA("Cédula"),
    FACIAL_Y_CEDULA("Rostro + Cédula");

    private final String label;

    AccessMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
