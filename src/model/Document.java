package model;

import java.time.LocalDate;

public class Document {

    private final DocumentType type;
    private final String number;
    private final LocalDate expeditionDate;
    private final LocalDate expirationDate; // null = no vence

    public Document(DocumentType type,
                    String number,
                    LocalDate expeditionDate,
                    LocalDate expirationDate) {
        this.type = type;
        this.number = number;
        this.expeditionDate = expeditionDate;
        this.expirationDate = expirationDate;
    }

    public boolean isExpired() {
        return expirationDate != null
                && expirationDate.isBefore(LocalDate.now());
    }

    public DocumentType getType() {
        return type;
    }

    public String getNumber() {
        return number;
    }

    public LocalDate getExpeditionDate() {
        return expeditionDate;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    @Override
    public String toString() {
        return type.getLabel() + " No. " + number;
    }
}
