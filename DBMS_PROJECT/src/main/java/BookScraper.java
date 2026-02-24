import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.sql.*;

public class BookScraper {

    public void fetchAndSaveBooks(String mood) {
        String searchUrl = "";

        // Map mood to URL
        switch (mood.toLowerCase()) {
            case "happy": searchUrl = "https://books.toscrape.com/catalogue/category/books/humor_30/index.html"; break;
            case "sad": searchUrl = "https://books.toscrape.com/catalogue/category/books/poetry_23/index.html"; break;
            case "adventurous": searchUrl = "https://books.toscrape.com/catalogue/category/books/travel_2/index.html"; break;
            case "mystery": searchUrl = "https://books.toscrape.com/catalogue/category/books/mystery_3/index.html"; break;
            default: System.out.println("Mood not supported."); return;
        }

        try {
            Document doc = Jsoup.connect(searchUrl).get();
            Elements books = doc.select(".product_pod");

            try (Connection conn = DBConnection.getConnection()) {
                if (conn == null) return;

                int moodId = getMoodId(conn, mood);

                // SQL: Note we now use 'author_id' instead of 'author'
                String insertBookSQL = "INSERT INTO Books (title, author_id, rating) VALUES (?, ?, ?)";
                String linkSQL = "INSERT INTO Book_Moods (book_id, mood_id) VALUES (?, ?)";

                PreparedStatement pstmtBook = conn.prepareStatement(insertBookSQL, Statement.RETURN_GENERATED_KEYS);
                PreparedStatement pstmtLink = conn.prepareStatement(linkSQL);

                for (Element book : books) {
                    String title = book.select("h3 a").attr("title");
                    // Fix for the Jsoup error you saw earlier
                    String rating = book.select("p.star-rating").first().className().replace("star-rating ", "");

                    // NEW: Handle the Author Relation
                    // Since this website doesn't list authors, we use a default.
                    // In a real project, you would scrape this variable.
                    int authorId = getOrCreateAuthorId(conn, "Unknown Author");

                    // Insert Book with the Author ID
                    pstmtBook.setString(1, title);
                    pstmtBook.setInt(2, authorId); // <--- THIS WAS THE FIX
                    pstmtBook.setString(3, rating);
                    pstmtBook.executeUpdate();

                    ResultSet rs = pstmtBook.getGeneratedKeys();
                    if (rs.next()) {
                        int bookId = rs.getInt(1);
                        pstmtLink.setInt(1, bookId);
                        pstmtLink.setInt(2, moodId);
                        pstmtLink.executeUpdate();
                        System.out.println("Saved: " + title);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- HELPER METHODS ---

    // NEW: Checks if author exists, inserts if not, and returns ID
    private int getOrCreateAuthorId(Connection conn, String authorName) throws SQLException {
        // 1. Check if author already exists
        String checkSQL = "SELECT author_id FROM Authors WHERE author_name = ?";
        PreparedStatement checkStmt = conn.prepareStatement(checkSQL);
        checkStmt.setString(1, authorName);
        ResultSet rs = checkStmt.executeQuery();

        if (rs.next()) {
            return rs.getInt("author_id");
        }

        // 2. If not, insert new author
        String insertSQL = "INSERT INTO Authors (author_name) VALUES (?)";
        PreparedStatement insertStmt = conn.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS);
        insertStmt.setString(1, authorName);
        insertStmt.executeUpdate();

        ResultSet keys = insertStmt.getGeneratedKeys();
        if (keys.next()) {
            return keys.getInt(1);
        }
        return 1; // Fallback
    }

    private int getMoodId(Connection conn, String moodName) throws SQLException {
        String query = "SELECT mood_id FROM Moods WHERE mood_name = ?";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setString(1, moodName);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt("mood_id");
        return 1;
    }
}