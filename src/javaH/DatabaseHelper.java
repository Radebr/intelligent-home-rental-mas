package javaH;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;

public class DatabaseHelper {

    private final HikariDataSource dataSource;

    public DatabaseHelper(String hostname, int port, String dbName, String username, String password) {
        // 1. Direct connection to ensure the database exists
        String serverUrl = "jdbc:mysql://" + hostname + ":" + port + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
        try (Connection conn = DriverManager.getConnection(serverUrl, username, password);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (SQLException e) {}

        // 2. Configure HikariCP Connection Pool
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8");
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(30000);
        
        this.dataSource = new HikariDataSource(config);

        // 3. Initialize Tables and Seed Data
        initializeUsersTable();
        initializeHomesTable();
        initializeOffersTable();

        if (dbName != null && dbName.toLowerCase().contains("owner")) {
            seedDummyDataIfEmpty();
        } else if (dbName != null && dbName.toLowerCase().contains("customer")) {
            seedDummyCustomersIfEmpty();
        }
    }

    private void initializeUsersTable() {
        String sql = "CREATE TABLE IF NOT EXISTS users ("
                   + "username VARCHAR(255) PRIMARY KEY NOT NULL,"
                   + "password TEXT NOT NULL"
                   + ") CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;";
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {}
    }

    private void initializeHomesTable() {
         String createSql = "CREATE TABLE IF NOT EXISTS homes ("
                    + "home_id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "owner_username VARCHAR(255) NOT NULL,"
                    + "address TEXT NOT NULL,"
                    + "description TEXT NULL,"
                    + "min_price INT NOT NULL,"
                    + "max_price INT NOT NULL,"
                    + "min_duration INT NOT NULL,"
                    + "max_duration INT NOT NULL,"
                    + "status VARCHAR(50) DEFAULT 'Available' NOT NULL,"
                    + "image_path TEXT NULL DEFAULT NULL,"
                    + "FOREIGN KEY (owner_username) REFERENCES users(username) ON DELETE CASCADE ON UPDATE CASCADE"
                    + ") CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;";
         try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
             stmt.execute(createSql);
         } catch (SQLException e) {}
     }


    private void initializeOffersTable() {
        String sql = "CREATE TABLE IF NOT EXISTS offers ("
                   + "offer_id INT AUTO_INCREMENT PRIMARY KEY,"
                   + "home_id INT NOT NULL,"
                   + "customer_username VARCHAR(255) NOT NULL,"
                   + "owner_username VARCHAR(255) NOT NULL,"
                   + "price INT NOT NULL,"
                   + "duration INT NOT NULL,"
                   + "status VARCHAR(50) DEFAULT 'Pending' NOT NULL,"
                   + "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                   + ") CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;";
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {}
    }

     private void seedDummyDataIfEmpty() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement(); 
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM homes")) {
            
            if (rs.next() && rs.getInt(1) > 0) return;

            registerUser("owner1", "123");
            registerUser("owner2", "123");
            registerUser("owner3", "123");


            Object[][] dummyHomes = {
                {"owner1", "123 Maple St, White Modern", "Cozy family home with beautiful front porch and large garage.", 1500, 2000, 6, 12, "Available", "home_images/home1.png"},
                {"owner1", "456 Oak Ave, Classic Blue", "Beautiful suburban house with lovely landscaping and brick details.", 2000, 2500, 12, 24, "Rented", "home_images/home2.png"},
                {"owner2", "789 Pine Ln, Evening Villa", "Spacious brick house with spectacular evening lighting.", 1800, 2200, 6, 18, "Available", "home_images/home3.png"},
                {"owner2", "101 Modern Way, The Cube", "Luxury contemporary villa with wide glass windows.", 3500, 4500, 12, 36, "Available", "home_images/home4.png"},
                {"owner3", "202 Lake View, Water Front", "Stunning waterfront property with custom pool and luxury seating.", 5000, 6500, 1, 12, "Unavailable", "home_images/home5.png"},
                {"owner3", "303 Minimalist Blvd", "Chic modern design with smart home features and dark accents.", 2500, 2900, 6, 12, "Available", "home_images/home6.png"},
                {"owner1", "404 Classic Rd, Traditional", "Traditional two-story house with front porch and green yard.", 1700, 2100, 12, 24, "Rented", "home_images/home7.png"},
                {"owner2", "505 Countryside Dr", "Spacious white farmhouse style home with massive garage.", 2200, 2700, 6, 12, "Available", "home_images/home8.png"},
                {"owner3", "606 Stone Haven", "Elegant stone exterior with large front yard and classic design.", 2800, 3500, 12, 24, "Available", "home_images/home9.jpg"},
                {"owner1", "707 Forest Edge, Blue Cabin", "Cozy dark blue home near woods with beautiful stone pathways.", 1600, 1950, 3, 12, "Available", "home_images/home10.jpg"},
                {"owner2", "808 Cottage Row, Teal Charm", "Charming two-story cottage with a cozy front porch, teal shutters, and surrounded by beautiful greenery.", 1200, 1600, 6, 24, "Available", "home_images/home11.jpg"}
            };

            String sql = "INSERT INTO homes (owner_username, address, description, min_price, max_price, min_duration, max_duration, status, image_path) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Object[] home : dummyHomes) {
                    pstmt.setString(1, (String) home[0]); pstmt.setString(2, (String) home[1]);
                    pstmt.setString(3, (String) home[2]); pstmt.setInt(4, (Integer) home[3]);
                    pstmt.setInt(5, (Integer) home[4]);   pstmt.setInt(6, (Integer) home[5]);
                    pstmt.setInt(7, (Integer) home[6]);   pstmt.setString(8, (String) home[7]);
                    pstmt.setString(9, (String) home[8]); pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
        } catch (SQLException e) {}
    }

     private void seedDummyCustomersIfEmpty() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
            if (rs.next() && rs.getInt(1) > 0) return;
            
            registerUser("customer1", "123"); registerUser("customer2", "123"); registerUser("customer3", "123");
        } catch (SQLException e) {}
    }

     private HomeData mapRowToHomeData(ResultSet rs) throws SQLException {
         return new HomeData(
              rs.getInt("home_id"), 
              rs.getString("owner_username"),
              rs.getString("address"), 
              rs.getString("description"),
              rs.getInt("min_price"), 
              rs.getInt("max_price"), 
              rs.getInt("min_duration"),
              rs.getInt("max_duration"), 
              rs.getString("status"), 
              rs.getString("image_path")
          );
     }



    public boolean saveOffer(int homeId, String customer, String owner, int price, int duration) {
        String sql = "INSERT INTO offers (home_id, customer_username, owner_username, price, duration, status) VALUES (?, ?, ?, ?, ?, 'Pending')";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, homeId); pstmt.setString(2, customer); pstmt.setString(3, owner); pstmt.setInt(4, price); pstmt.setInt(5, duration);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean cancelOffer(int homeId, String customer) {
        String sql = "DELETE FROM offers WHERE home_id = ? AND customer_username = ? AND status = 'Pending'";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, homeId); pstmt.setString(2, customer);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean hasPendingOffer(int homeId, String customer) {
        String sql = "SELECT offer_id FROM offers WHERE home_id = ? AND customer_username = ? AND status = 'Pending'";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, homeId); pstmt.setString(2, customer);
            try(ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) { return false; }
    }

    public List<NegotiationRequest> getPendingOffersForOwner(String ownerUsername) {
        List<NegotiationRequest> offers = new ArrayList<>();
        String sql = "SELECT o.offer_id, o.home_id, o.customer_username, o.price, o.duration, h.address " +
                     "FROM offers o JOIN homes h ON o.home_id = h.home_id " +
                     "WHERE o.owner_username = ? AND o.status = 'Pending'";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ownerUsername);
            try(ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    offers.add(new NegotiationRequest(
                        rs.getString("customer_username"), 
                        rs.getInt("home_id"), 
                        rs.getString("address"), 
                        rs.getInt("price"), 
                        rs.getInt("duration"), 
                        "db-offer-" + rs.getInt("offer_id")
                    ));
                }
            }
        } catch (SQLException e) {}
        return offers;
    }

    public boolean updateOfferStatus(int homeId, String customer, String status) {
        String sql = "UPDATE offers SET status = ? WHERE home_id = ? AND customer_username = ? AND status = 'Pending'";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status); pstmt.setInt(2, homeId); pstmt.setString(3, customer);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }



    public boolean addHome(String ownerUsername, String address, String description, int minPrice, int maxPrice, int minDuration, int maxDuration, String imagePath) {
        if (ownerUsername == null || address == null) return false;
        String sql = "INSERT INTO homes (owner_username, address, description, min_price, max_price, min_duration, max_duration, status, image_path) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ownerUsername.trim()); pstmt.setString(2, address.trim());
            pstmt.setString(3, (description != null ? description.trim() : null));
            pstmt.setInt(4, minPrice); pstmt.setInt(5, maxPrice);
            pstmt.setInt(6, minDuration); pstmt.setInt(7, maxDuration);
            pstmt.setString(8, "Available"); 
            if (imagePath != null && !imagePath.trim().isEmpty()) pstmt.setString(9, imagePath.trim());
            else pstmt.setNull(9, Types.VARCHAR);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
     }

    public List<HomeData> getHomesForOwner(String ownerUsername) {
         List<HomeData> homes = new ArrayList<>();
         if (ownerUsername == null || ownerUsername.trim().isEmpty()) return homes;
         String sql = "SELECT * FROM homes WHERE owner_username = ? ORDER BY home_id";
         try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
             pstmt.setString(1, ownerUsername);
             try (ResultSet rs = pstmt.executeQuery()) {
                 while (rs.next()) homes.add(mapRowToHomeData(rs));
             }
         } catch(SQLException e) {}
         return homes;
     }

    public List<HomeData> getAllAvailableHomes() {
        List<HomeData> viewableHomes = new ArrayList<>();
        String sql = "SELECT * FROM homes WHERE status IN ('Available', 'Rented', 'Unavailable') ORDER BY FIELD(status, 'Available', 'Rented', 'Unavailable'), owner_username, home_id";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) viewableHomes.add(mapRowToHomeData(rs));
        } catch (SQLException e) {}
        return viewableHomes;
    }

    public HomeData getHomeById(int homeId) {
        if (homeId <= 0) return null;
        String sql = "SELECT * FROM homes WHERE home_id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, homeId);
            try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) return mapRowToHomeData(rs); }
        } catch (SQLException e) {}
        return null;
     }

    public boolean updateHomeStatus(int homeId, String newStatus) {
        if (homeId <= 0 || newStatus == null || newStatus.trim().isEmpty()) return false;
        String sql = "UPDATE homes SET status = ? WHERE home_id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus.trim()); pstmt.setInt(2, homeId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean updateHome(int homeId, String address, String description, int minPrice, int maxPrice, int minDuration, int maxDuration, String status, String imagePath) {
        if (homeId <= 0 || address == null) return false;
        String sql = "UPDATE homes SET address=?, description=?, min_price=?, max_price=?, min_duration=?, max_duration=?, status=?, image_path=? WHERE home_id=?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
             pstmt.setString(1, address.trim()); pstmt.setString(2, (description != null ? description.trim() : null));
             pstmt.setInt(3, minPrice); pstmt.setInt(4, maxPrice); pstmt.setInt(5, minDuration); pstmt.setInt(6, maxDuration);
             pstmt.setString(7, status.trim());
             if (imagePath != null && !imagePath.trim().isEmpty()) pstmt.setString(8, imagePath.trim()); else pstmt.setNull(8, Types.VARCHAR);
             pstmt.setInt(9, homeId);
             return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean deleteHome(int homeId, String imagePath) {
        if (homeId <= 0) return false;
        String sql = "DELETE FROM homes WHERE home_id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, homeId);
            if (pstmt.executeUpdate() > 0) {
                if (imagePath != null && !imagePath.trim().isEmpty()) {
                    try {
                        Path pathToDelete = Paths.get(imagePath.replace('/', File.separatorChar));
                        if (Files.exists(pathToDelete) && Files.isRegularFile(pathToDelete)) Files.delete(pathToDelete);
                    } catch (Exception ex) {}
                }
                return true;
            }
        } catch (SQLException e) {}
        return false;
    }

    public boolean registerUser(String user, String pass) { 
        if(user == null || pass == null) return false;
        String sql = "INSERT INTO users(username,password) VALUES(?,?)";
        try(Connection c = dataSource.getConnection(); PreparedStatement p = c.prepareStatement(sql)){
            p.setString(1, user.trim()); p.setString(2, pass);
            return p.executeUpdate() > 0;
        } catch(SQLException e){ return false; } 
    }

    public boolean checkLogin(String user, String pass){ 
        if(user == null || pass == null) return false;
        String sql = "SELECT password FROM users WHERE LOWER(username) = LOWER(?)";
        try(Connection c = dataSource.getConnection(); PreparedStatement p = c.prepareStatement(sql)){
            p.setString(1, user.trim());
            try(ResultSet rs = p.executeQuery()){
                if(rs.next()) return rs.getString("password").equals(pass);
            }
        } catch(SQLException e){ }
        return false;
    }
}