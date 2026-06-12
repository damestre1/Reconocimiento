package repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Administra la conexión única a la base de datos SQLite y crea
 * las tablas si no existen. Requiere el driver sqlite-jdbc en el
 * classpath del proyecto.
 */
public final class DatabaseManager {

    private static final String DB_URL =
            "jdbc:sqlite:data/reconocimiento.db";

    private static Connection connection;

    private DatabaseManager() {
    }

    public static synchronized Connection getConnection() {
        if (connection == null) {
            try {
                java.io.File dataDir = new java.io.File("data");
                if (!dataDir.exists() && !dataDir.mkdirs()) {
                    throw new IllegalStateException(
                            "No se pudo crear la carpeta data/");
                }

                connection = DriverManager.getConnection(DB_URL);
                createTables();

            } catch (SQLException e) {
                throw new IllegalStateException(
                        "No se pudo conectar a SQLite: " + e.getMessage(), e);
            }
        }
        return connection;
    }

    private static void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {

            statement.execute(
                    "CREATE TABLE IF NOT EXISTS users ("
                            + "national_id TEXT PRIMARY KEY,"
                            + "first_name  TEXT NOT NULL,"
                            + "last_name   TEXT NOT NULL,"
                            + "email       TEXT NOT NULL,"
                            + "phone       TEXT NOT NULL,"
                            + "active      INTEGER NOT NULL DEFAULT 1,"
                            + "face_path   TEXT"
                            + ")");

            statement.execute(
                    "CREATE TABLE IF NOT EXISTS access_records ("
                            + "id          TEXT PRIMARY KEY,"
                            + "national_id TEXT,"
                            + "date        TEXT NOT NULL,"
                            + "time        TEXT NOT NULL,"
                            + "method      TEXT NOT NULL,"
                            + "result      TEXT NOT NULL,"
                            + "detail      TEXT"
                            + ")");
        }
    }

    public static synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // Cierre del programa: no hay nada más que hacer.
            }
            connection = null;
        }
    }
}
