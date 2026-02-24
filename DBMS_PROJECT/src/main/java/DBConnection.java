import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/BookMoodDB";
    private static final String USER = "root";
    private static final String PASSWORD = "Nish@2525";

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.out.println("Connection Failed! Check console output.");
            e.printStackTrace();
            return null;
        }
    }
}