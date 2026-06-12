package model;

import java.time.LocalDateTime;

public class FaceRecord {

    private final String nationalId;
    private final String imagePath;
    private final LocalDateTime capturedAt;

    public FaceRecord(String nationalId, String imagePath) {
        this.nationalId = nationalId;
        this.imagePath = imagePath;
        this.capturedAt = LocalDateTime.now();
    }

    public String getNationalId() {
        return nationalId;
    }

    public String getImagePath() {
        return imagePath;
    }

    public LocalDateTime getCapturedAt() {
        return capturedAt;
    }
}
