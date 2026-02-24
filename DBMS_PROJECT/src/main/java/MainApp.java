import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Scanner;

public class MainApp {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        BookScraper scraper = new BookScraper();

        System.out.println("=== Mood-Based Book Recommender ===");
        System.out.println("Available Moods: Happy, Sad, Adventurous, Mystery");
        System.out.print("How are you feeling today? ");
        String mood = scanner.nextLine();

        // 1. Scrape data for this mood (Real-time fetching)
        scraper.fetchAndSaveBooks(mood);

        // 2. Fetch from Database to show the user
        System.out.println("\n=== Here are book recommendations for a " + mood + " mood ===");
        showBooks(mood);
    }

    public static void showBooks(String mood) {
        String sql = "SELECT b.title, b.rating FROM Books b " +
                "JOIN Book_Moods bm ON b.book_id = bm.book_id " +
                "JOIN Moods m ON bm.mood_id = m.mood_id " +
                "WHERE m.mood_name = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, mood);
            ResultSet rs = pstmt.executeQuery();

            System.out.printf("%-50s %-10s%n", "BOOK TITLE", "RATING");
            System.out.println("----------------------------------------------------------");

            while (rs.next()) {
                System.out.printf("%-50s %-10s%n",
                        rs.getString("title"),
                        rs.getString("rating"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void logUserHistory(int userId, String moodName, int bookId) {
        String sql = "INSERT INTO User_History (user_id, mood_id, book_recommended_id) " +
                "VALUES (?, (SELECT mood_id FROM Moods WHERE mood_name = ?), ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, moodName);
            pstmt.setInt(3, bookId);

            pstmt.executeUpdate();
            System.out.println("(Activity Logged: User " + userId + " felt " + moodName + ")");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}