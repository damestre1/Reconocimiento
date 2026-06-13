package repository;

import model.AccessMethod;
import model.AccessRecord;
import model.AccessResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class SQLiteAccessRecordRepository
        implements AccessRecordRepository {

    @Override
    public void save(AccessRecord record) {
        String sql = "INSERT INTO access_records "
                + "(id, national_id, date, time, method, result, detail) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        Connection connection = DatabaseManager.getConnection();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, record.getId());
            ps.setString(2, record.getNationalId());
            ps.setString(3, record.getDate().toString());
            ps.setString(4, record.getTime().toString());
            ps.setString(5, record.getMethod().name());
            ps.setString(6, record.getResult().name());
            ps.setString(7, record.getDetail());
            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println(
                    "Error guardando acceso: " + e.getMessage());
        }
    }

    @Override
    public List<AccessRecord> findAll() {
        List<AccessRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM access_records ORDER BY date, time";

        Connection connection = DatabaseManager.getConnection();

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {

            while (rs.next()) {
                records.add(mapRecord(rs));
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error listando accesos: " + e.getMessage());
        }

        return records;
    }

    @Override
    public List<AccessRecord> findByNationalId(String nationalId) {
        List<AccessRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM access_records "
                + "WHERE national_id = ? ORDER BY date, time";

        Connection connection = DatabaseManager.getConnection();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nationalId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(mapRecord(rs));
                }
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error consultando accesos: " + e.getMessage());
        }

        return records;
    }

    private AccessRecord mapRecord(ResultSet rs) throws SQLException {
        return new AccessRecord(
                rs.getString("id"),
                rs.getString("national_id"),
                LocalDate.parse(rs.getString("date")),
                LocalTime.parse(rs.getString("time")),
                AccessMethod.valueOf(rs.getString("method")),
                AccessResult.valueOf(rs.getString("result")),
                rs.getString("detail"));
    }
}
