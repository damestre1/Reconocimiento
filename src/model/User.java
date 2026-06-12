package model;

import java.time.LocalDateTime;

public class User extends Person {

    private final String nationalId;
    private final Document document;
    private FaceRecord faceRecord;
    private boolean active;
    private final LocalDateTime registeredAt;

    public User(String nationalId,
                String firstName,
                String lastName,
                String email,
                String phone,
                Document document) {
        super(firstName, lastName, email, phone);
        this.nationalId = nationalId;
        this.document = document;
        this.active = true;
        this.registeredAt = LocalDateTime.now();
    }

    public boolean hasFaceEnrolled() {
        return faceRecord != null;
    }

    public String getNationalId() {
        return nationalId;
    }

    public Document getDocument() {
        return document;
    }

    public FaceRecord getFaceRecord() {
        return faceRecord;
    }

    public void setFaceRecord(FaceRecord faceRecord) {
        this.faceRecord = faceRecord;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    @Override
    public String toString() {
        return "[" + nationalId + "] " + getFullName();
    }
}
