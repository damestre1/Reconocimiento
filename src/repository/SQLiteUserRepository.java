package repository;

import model.Document;
import model.DocumentType;
import model.FaceRecord;
import model.User;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SQLiteUserRepository implements UserRepository {

    @Override
    public void save(User user) {
        String sql = "INSERT OR REPLACE INTO users "
                + "(national_id, first_name, last_name, email, phone, "
                + "active, face_path) VALUES (?, ?, ?, ?, ?, ?, ?)";

        Connection connection = DatabaseManager.getConnection();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getNationalId());
            ps.setString(2, user.getFirstName());
            ps.setString(3, user.getLastName());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getPhone());
            ps.setInt(6, user.isActive() ? 1 : 0);
            ps.setString(7, user.hasFaceEnrolled()
                    ? user.getFaceRecord().getImagePath()
                    : null);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println(
                    "Error guardando usuario: " + e.getMessage());
        }
    }

    @Override
    public Optional<User> findByNationalId(String nationalId) {
        String sql = "SELECT * FROM users WHERE national_id = ?";

        Connection connection = DatabaseManager.getConnection();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nationalId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error consultando usuario: " + e.getMessage());
        }

        return Optional.empty();
    }

    @Override
    public boolean existsByNationalId(String nationalId) {
        return findByNationalId(nationalId).isPresent();
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";

        Connection connection = DatabaseManager.getConnection();

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {

            while (rs.next()) {
                users.add(mapUser(rs));
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error listando usuarios: " + e.getMessage());
        }

        return users;
    }

    @Override
    public void deleteByNationalId(String nationalId) {
        String sql = "DELETE FROM users WHERE national_id = ?";

        Connection connection = DatabaseManager.getConnection();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nationalId);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println(
                    "Error eliminando usuario: " + e.getMessage());
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        String nationalId = rs.getString("national_id");

        Document document = new Document(
                DocumentType.CEDULA_CIUDADANIA,
                nationalId,
                LocalDate.now(),
                null);

        User user = new User(
                nationalId,
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("email"),
                rs.getString("phone"),
                document);

        user.setActive(rs.getInt("active") == 1);

        String facePath = rs.getString("face_path");
        if (facePath != null && new File(facePath).exists()) {
            user.setFaceRecord(new FaceRecord(nationalId, facePath));
        }

        return user;
    }
}
