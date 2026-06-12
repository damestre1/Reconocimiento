package service;

import exception.AccessDeniedException;
import model.Administrator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LoginService {

    private final Map<String, Administrator> administrators =
            new ConcurrentHashMap<>();

    public void registerAdministrator(Administrator administrator) {
        administrators.put(administrator.getUsername(), administrator);
    }

    public Administrator login(String username, String password)
            throws AccessDeniedException {

        if (username == null || username.trim().isEmpty()
                || password == null || password.isEmpty()) {
            throw new AccessDeniedException(
                    "Usuario y contraseña son obligatorios.");
        }

        Administrator admin = administrators.get(username.trim());

        if (admin == null || !admin.checkPassword(password)) {
            throw new AccessDeniedException(
                    "Credenciales incorrectas.");
        }

        return admin;
    }
}
