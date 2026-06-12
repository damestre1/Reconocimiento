package service;

import exception.InvalidDocumentException;
import exception.InvalidNationalIdException;
import exception.ValidationException;
import model.Document;
import model.DocumentType;
import model.User;
import repository.UserRepository;
import util.ValidationUtils;

import java.time.LocalDate;

public class RegistrationService {

    private final UserRepository userRepository;

    public RegistrationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User registerUser(String nationalId,
                             String firstName,
                             String lastName,
                             String email,
                             String phone,
                             DocumentType documentType,
                             LocalDate expeditionDate,
                             LocalDate expirationDate)
            throws ValidationException,
            InvalidNationalIdException,
            InvalidDocumentException {

        // Solo cédula de ciudadanía
        if (documentType == null) {
            throw new InvalidDocumentException(
                    "El tipo de documento es obligatorio.");
        }

        if (documentType == DocumentType.TARJETA_IDENTIDAD) {
            throw new InvalidDocumentException(
                    "No se permiten tarjetas de identidad.");
        }

        if (documentType == DocumentType.PASAPORTE) {
            throw new InvalidDocumentException(
                    "No se permiten pasaportes.");
        }

        if (documentType != DocumentType.CEDULA_CIUDADANIA) {
            throw new InvalidDocumentException(
                    "Solo se permite el registro con cédula de ciudadanía.");
        }

        // Validaciones de datos
        String id = ValidationUtils.validateNationalId(nationalId);
        String name = ValidationUtils.validateName(firstName, "nombres");
        String surname = ValidationUtils.validateName(lastName, "apellidos");
        String mail = ValidationUtils.validateEmail(email);
        String phoneNumber = ValidationUtils.validatePhone(phone);

        // Documento vencido
        Document document = new Document(
                documentType, id, expeditionDate, expirationDate);

        if (document.isExpired()) {
            throw new InvalidDocumentException(
                    "El documento está vencido. No se permite el registro.");
        }

        // Cédula duplicada
        if (userRepository.existsByNationalId(id)) {
            throw new InvalidNationalIdException(
                    "Ya existe un usuario registrado con la cédula " + id + ".");
        }

        User user = new User(
                id, name, surname, mail, phoneNumber, document);

        userRepository.save(user);
        return user;
    }
}
