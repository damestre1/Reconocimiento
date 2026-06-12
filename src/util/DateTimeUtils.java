package util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtils {

    public static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    public static final DateTimeFormatter DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateTimeUtils() {
    }

    public static String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DATE_FORMAT);
    }

    public static String formatTime(LocalTime time) {
        return time == null ? "" : time.format(TIME_FORMAT);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(DATETIME_FORMAT);
    }

    public static String nowAsString() {
        return LocalDateTime.now().format(DATETIME_FORMAT);
    }
}
