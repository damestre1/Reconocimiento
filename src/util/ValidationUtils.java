package util;

import exception.InvalidNationalIdException;
import exception.ValidationException;

import java.util.regex.Pattern;

public final class ValidationUtils {

    private static final Pattern ONLY_DIGITS =
            Pattern.compile("^\\d+$");

    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[A-Za-zÁÉÍÓÚáéíóúÑñÜü' -]+$");

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private ValidationUtils() {
    }

    public static String validateNationalId(String nationalId)
            throws InvalidNationalIdException {

        if (nationalId == null || nationalId.trim().isEmpty()) {
            throw new InvalidNationalIdException(
                    "La cédula es obligatoria.");
        }

        String value = nationalId.trim();

        if (!ONLY_DIGITS.matcher(value).matches()) {
            throw new InvalidNationalIdException(
                    "La cédula solo puede contener números.");
        }

        if (value.length() < Constants.NATIONAL_ID_MIN_LENGTH
                || value.length() > Constants.NATIONAL_ID_MAX_LENGTH) {
            throw new InvalidNationalIdException(
                    "La cédula debe tener entre "
                            + Constants.NATIONAL_ID_MIN_LENGTH + " y "
                            + Constants.NATIONAL_ID_MAX_LENGTH + " dígitos.");
        }

        return value;
    }

    public static String validateName(String name, String fieldLabel)
            throws ValidationException {

        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException(
                    "El campo '" + fieldLabel + "' es obligatorio.");
        }

        String value = name.trim();

        if (value.length() < Constants.NAME_MIN_LENGTH
                || value.length() > Constants.NAME_MAX_LENGTH) {
            throw new ValidationException(
                    "El campo '" + fieldLabel + "' debe tener entre "
                            + Constants.NAME_MIN_LENGTH + " y "
                            + Constants.NAME_MAX_LENGTH + " caracteres.");
        }

        if (!NAME_PATTERN.matcher(value).matches()) {
            throw new ValidationException(
                    "El campo '" + fieldLabel
                            + "' solo puede contener letras y espacios.");
        }

        return value;
    }

    public static String validateEmail(String email)
            throws ValidationException {

        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException(
                    "El correo electrónico es obligatorio.");
        }

        String value = email.trim().toLowerCase();

        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new ValidationException(
                    "El correo electrónico no tiene un formato válido.");
        }

        return value;
    }

    public static String validatePhone(String phone)
            throws ValidationException {

        if (phone == null || phone.trim().isEmpty()) {
            throw new ValidationException(
                    "El teléfono es obligatorio.");
        }

        String value = phone.trim();

        if (!ONLY_DIGITS.matcher(value).matches()) {
            throw new ValidationException(
                    "El teléfono solo puede contener números.");
        }

        if (value.length() < Constants.PHONE_MIN_LENGTH
                || value.length() > Constants.PHONE_MAX_LENGTH) {
            throw new ValidationException(
                    "El teléfono debe tener entre "
                            + Constants.PHONE_MIN_LENGTH + " y "
                            + Constants.PHONE_MAX_LENGTH + " dígitos.");
        }

        return value;
    }
}
