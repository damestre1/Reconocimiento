package model;

import java.time.LocalDate;
import java.time.LocalTime;

public class AccessRecord {

    private final String id;
    private final String nationalId;
    private final LocalDate date;
    private final LocalTime time;
    private final AccessMethod method;
    private final AccessResult result;
    private final String detail;

    public AccessRecord(String id,
                        String nationalId,
                        AccessMethod method,
                        AccessResult result,
                        String detail) {
        this(id, nationalId, LocalDate.now(),
                LocalTime.now().withNano(0), method, result, detail);
    }

    /** Constructor completo: usado al cargar registros desde la BD. */
    public AccessRecord(String id,
                        String nationalId,
                        LocalDate date,
                        LocalTime time,
                        AccessMethod method,
                        AccessResult result,
                        String detail) {
        this.id = id;
        this.nationalId = nationalId;
        this.date = date;
        this.time = time;
        this.method = method;
        this.result = result;
        this.detail = detail;
    }

    public String getId() {
        return id;
    }

    public String getNationalId() {
        return nationalId;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getTime() {
        return time;
    }

    public AccessMethod getMethod() {
        return method;
    }

    public AccessResult getResult() {
        return result;
    }

    public String getDetail() {
        return detail;
    }

    @Override
    public String toString() {
        return date + " " + time
                + " | " + method.getLabel()
                + " | " + result
                + " | usuario=" + (nationalId == null ? "-" : nationalId)
                + (detail == null ? "" : " | " + detail);
    }
}
