package org.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseManager {
    private static final String DB_HOST = System.getenv("DB_HOST") != null ? System.getenv("DB_HOST") : "localhost";
    private static final String URL = "jdbc:mysql://" + DB_HOST + ":3306/email_db";
    private static final String USER = "root"; // Update if necessary
    private static final String PASSWORD = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "root";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Authenticate User
    public static boolean authenticateUser(String username, String password) {
        String sql = "{CALL authenticate_user(?, ?)}";
        try (Connection conn = getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Store Email
    public static boolean storeEmail(String sender, String recipient, String subject, String content) {
        // Enforce quota
        if (isQuotaExceeded(recipient, content.length())) {
            System.err.println("QUOTA EXCEEDED for user: " + recipient);
            return false;
        }

        String sql = "{CALL store_email(?, ?, ?, ?)}";
        try (Connection conn = getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {
            stmt.setString(1, sender);
            stmt.setString(2, recipient);
            stmt.setString(3, subject);
            stmt.setString(4, content);
            stmt.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isQuotaExceeded(String username, int incomingSize) {
        String sql = "SELECT (SELECT quota_limit FROM users WHERE username = ?) as limit_val, " +
                     "       (SELECT IFNULL(SUM(LENGTH(content)), 0) FROM emails WHERE recipient = ?) as current_size";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long limit = rs.getLong("limit_val");
                    long current = rs.getLong("current_size");
                    return (current + incomingSize) > limit;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Fetch Emails
    public static List<Map<String, Object>> fetchEmails(String username) {
        List<Map<String, Object>> emails = new ArrayList<>();
        String sql = "{CALL fetch_emails(?)}";
        try (Connection conn = getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> email = new HashMap<>();
                    email.put("id", rs.getInt("id"));
                    email.put("sender", rs.getString("sender"));
                    email.put("recipient", rs.getString("recipient"));
                    email.put("subject", rs.getString("subject"));
                    email.put("content", rs.getString("content"));
                    email.put("is_read", rs.getBoolean("is_read"));
                    email.put("is_starred", rs.getBoolean("is_starred"));
                    email.put("category", rs.getString("category"));
                    email.put("created_at", rs.getTimestamp("created_at"));
                    emails.add(email);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return emails;
    }

    public static List<Map<String, Object>> fetchSentEmails(String username) {
        List<Map<String, Object>> emails = new ArrayList<>();
        String sql = "SELECT * FROM emails WHERE sender = ? OR sender LIKE CONCAT(?, '@%') ORDER BY created_at DESC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, username);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> email = new HashMap<>();
                    email.put("id", rs.getInt("id"));
                    email.put("sender", rs.getString("sender"));
                    email.put("recipient", rs.getString("recipient"));
                    email.put("subject", rs.getString("subject"));
                    email.put("content", rs.getString("content"));
                    email.put("is_read", rs.getBoolean("is_read"));
                    email.put("is_starred", rs.getBoolean("is_starred"));
                    email.put("category", rs.getString("category"));
                    email.put("created_at", rs.getTimestamp("created_at"));
                    emails.add(email);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return emails;
    }

    public static boolean toggleStar(int emailId) {
        String sql = "UPDATE emails SET is_starred = NOT is_starred WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, emailId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    // Delete Email
    public static boolean deleteEmail(int emailId) {
        String sql = "{CALL delete_email(?)}";
        try (Connection conn = getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {
            stmt.setInt(1, emailId);
            stmt.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Update Password
    public static boolean updatePassword(String username, String newPassword) {
        String sql = "{CALL update_password(?, ?)}";
        try (Connection conn = getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, newPassword);
            stmt.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Check if user exists
    public static boolean userExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Register User
    public static boolean registerUser(String username, String password) {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Get user statistics
    public static Map<String, Object> getUserStats(String username) {
        Map<String, Object> stats = new HashMap<>();
        String sql = "SELECT COUNT(*) as count, SUM(LENGTH(content)) as size FROM emails WHERE recipient = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    stats.put("totalEmails", rs.getInt("count"));
                    stats.put("totalSize", rs.getLong("size"));
                }
            }
            
            // Get Profile Data
            String sqlProfile = "SELECT quota_limit, display_name, profile_image FROM users WHERE username = ?";
            try (PreparedStatement stmtP = conn.prepareStatement(sqlProfile)) {
                stmtP.setString(1, username);
                try (ResultSet rsP = stmtP.executeQuery()) {
                    if (rsP.next()) {
                        stats.put("storageLimit", rsP.getLong("quota_limit"));
                        stats.put("displayName", rsP.getString("display_name"));
                        stats.put("profileImage", rsP.getString("profile_image"));
                    } else {
                        stats.put("storageLimit", 52428800L);
                    }
                }
            }

            // Unread count
            String sqlUnread = "SELECT COUNT(*) FROM emails WHERE recipient = ? AND is_read = 0";
            try (PreparedStatement stmt2 = conn.prepareStatement(sqlUnread)) {
                stmt2.setString(1, username);
                try (ResultSet rs2 = stmt2.executeQuery()) {
                    if (rs2.next()) {
                        stats.put("unreadEmails", rs2.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }

    public static void broadcastMessage(String subject, String content) {
        String getUsersSql = "SELECT username FROM users";
        String insertSql = "INSERT INTO emails (sender, recipient, subject, content) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement getStmt = conn.prepareStatement(getUsersSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            
            ResultSet rs = getStmt.executeQuery();
            while (rs.next()) {
                String recipient = rs.getString("username");
                insertStmt.setString(1, "SYSTEM <admin@emp.dz>");
                insertStmt.setString(2, recipient);
                insertStmt.setString(3, subject);
                insertStmt.setString(4, content);
                insertStmt.addBatch();
            }
            insertStmt.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static boolean updateUserQuota(String username, long newLimit) {
        String sql = "UPDATE users SET quota_limit = ? WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, newLimit);
            stmt.setString(2, username);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateUserProfile(String username, String displayName, String profileImage) {
        String sql = "UPDATE users SET display_name = ?, profile_image = ? WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, displayName);
            stmt.setString(2, profileImage);
            stmt.setString(3, username);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Delete User
    public static boolean deleteUser(String username) {
        String sql = "DELETE FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Admin : Get detailed stats for all users
    public static java.util.List<java.util.Map<String, Object>> getUsersDetailedStats() {
        java.util.List<java.util.Map<String, Object>> allStats = new java.util.ArrayList<>();
        String sql = "SELECT u.username, u.quota_limit, u.profile_image, COUNT(e.id) as mail_count, SUM(IFNULL(LENGTH(e.content), 0)) as storage_used " +
                     "FROM users u " +
                     "LEFT JOIN emails e ON u.username = e.recipient " +
                     "GROUP BY u.username, u.quota_limit, u.profile_image " +
                     "ORDER BY mail_count DESC";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                java.util.Map<String, Object> site = new java.util.HashMap<>();
                site.put("username", rs.getString("username"));
                site.put("quota_limit", rs.getLong("quota_limit"));
                site.put("profile_image", rs.getString("profile_image"));
                site.put("count", rs.getInt("mail_count"));
                site.put("size", rs.getLong("storage_used"));
                allStats.add(site);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return allStats;
    }

    // Get all usernames for contact list
    public static java.util.List<String> getAllUsernames() {
        java.util.List<String> users = new java.util.ArrayList<>();
        String sql = "SELECT username FROM users ORDER BY username";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             java.sql.ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                users.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }
}
