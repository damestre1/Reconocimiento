package model;

public enum DocumentType {
    CEDULA_CIUDADANIA("Cédula de Ciudadanía"),
    TARJETA_IDENTIDAD("Tarjeta de Identidad"),
    PASAPORTE("Pasaporte"),
    OTRO("Otro");

    private final String label;

    DocumentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
