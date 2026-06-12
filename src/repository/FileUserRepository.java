package repository;

import model.Document;
import model.DocumentType;
import model.FaceRecord;
import model.User;
import util.Constants;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repositorio que guarda los usuarios en data/users.csv para que
 * sobrevivan al cierre del programa. Será reemplazado por el DAO
 * de SQLite (solo hay que cambiar la implementación en Main).
 */
public class FileUserRepository implements UserRepository {

    private static final String SEPARATOR = ";";

    private final Map<String, User> users = new ConcurrentHashMap<>();

    public FileUserRepository() {
        loadFromDisk();
    }

    @Override
    public synchronized void save(User user) {
        users.put(user.getNationalId(), user);
        persist();
    }

    @Override
    public Optional<User> findByNationalId(String nationalId) {
        return Optional.ofNullable(users.get(nationalId));
    }

    @Override
    public boolean existsByNationalId(String nationalId) {
        return users.containsKey(nationalId);
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public synchronized void deleteByNationalId(String nationalId) {
        users.remove(nationalId);
        persist();
    }

    private void loadFromDisk() {
        File file = new File(Constants.USERS_FILE);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader =
                     new BufferedReader(new FileReader(file))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] parts = line.split(SEPARATOR, -1);
                if (parts.length < 5) {
                    continue;
                }

                String nationalId = parts[0];
                Document document = new Document(
                        DocumentType.CEDULA_CIUDADANIA,
                        nationalId,
                        LocalDate.now(),
                        null);

                User user = new User(
                        nationalId,
                        parts[1],
                        parts[2],
                        parts[3],
                        parts[4],
                        document);

                String facePath = Constants.PHOTOS_DIR
                        + File.separator
                        + nationalId
                        + Constants.FACE_SUFFIX;

                if (new File(facePath).exists()) {
                    user.setFaceRecord(
                            new FaceRecord(nationalId, facePath));
                }

                users.put(nationalId, user);
            }

        } catch (IOException e) {
            System.out.println(
                    "Advertencia: no se pudo leer " + Constants.USERS_FILE
                            + " (" + e.getMessage() + ")");
        }
    }

    private void persist() {
        File file = new File(Constants.USERS_FILE);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            System.out.println(
                    "Advertencia: no se pudo crear la carpeta "
                            + parent.getPath());
            return;
        }

        try (BufferedWriter writer =
                     new BufferedWriter(new FileWriter(file, false))) {

            for (User user : users.values()) {
                writer.write(String.join(SEPARATOR,
                        user.getNationalId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.getPhone()));
                writer.newLine();
            }

        } catch (IOException e) {
            System.out.println(
                    "Advertencia: no se pudo guardar " + Constants.USERS_FILE
                            + " (" + e.getMessage() + ")");
        }
    }
}
