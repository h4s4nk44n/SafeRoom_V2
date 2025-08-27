package com.saferoom.db;

import java.io.FileInputStream;
import java.io.IOException;

import java.util.Properties;

import com.saferoom.p2p.*;
import com.saferoom.crypto.CryptoUtils;
import com.saferoom.email.*;
import com.saferoom.log.Logger;

import java.sql.*;



public class DBManager {
	
	private static final String CONFIG_FILE = "src/main/resources/dbconfig.properties";
	private static final String ICON_PATH = "src/main/resources/Verificate.png";
	private static String DB_URL;
	private static String DB_USER;
	private static String DB_PASSWORD;
	
	
	
    public static Logger LOGGER = Logger.getLogger(DBManager.class);

	static { try {
		loadDataBaseConfig();
	} catch (Exception e) {
		// TODO Auto-generated catch block
		try {
			LOGGER.error("DataBase Config loading failed."+ e.getMessage());
			throw new RuntimeException("Database Config File Unreadable", e);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		e.printStackTrace();
	}}
	


	private static void loadDataBaseConfig() throws Exception {
		try(FileInputStream fis = new FileInputStream(CONFIG_FILE)){
			
			Properties props = new Properties();
			props.load(fis);
			
			DB_URL = props.getProperty("db.url");
			DB_USER = props.getProperty("db.user");
			DB_PASSWORD = props.getProperty("db.password");
			
			LOGGER.info("Database config loaded successfully.");
		}
		catch(IOException e) {
			LOGGER.error("Database Config loaded successfully.");
			throw new RuntimeException("Database Config File Unreadable");
			
		}
		
	}
	
	public static Connection getConnection()throws SQLException{
		
		return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
	}
	
	
	public static boolean setSTUN_INFO(String myUsername) throws SQLException{
		String query = "INSERT INTO STUN_INFO(Public_IP, Public_Port) VALUES(?, ?) WHERE username = (?);";
		String[] returns = MultiStunClient.StunClient();
		
		if(returns[0].equals("true")) {
		System.out.println("Public IP:   " + returns[1]);
		System.out.println("Public Port:   " + returns[2]);
		System.out.println("OpenAccess? : "  + returns[3]);
		
			try(Connection contact = getConnection();
					PreparedStatement ptpt = contact.prepareStatement(query)){
				ptpt.setString(1,  returns[1]);
				ptpt.setInt(2, Integer.parseInt(returns[2]));
				ptpt.setString(3, myUsername);
				
				return ptpt.executeUpdate() > 0;
			}
		
		}
	
		return false;
	}
	
	public static String[] getSTUN_INFO(String username) throws SQLException{
		String query = "SELECT (Public_IP, Public_Port) FROM STUN_INFO WHERE username = (?);";
		String[] infos = new String[2];
		int port_num = 0;
		try(Connection con = getConnection();
				PreparedStatement ptpt = con.prepareStatement(query)){
			ptpt.setString(1, username);
			ResultSet rs = ptpt.executeQuery();
			if(rs.next()) {
			    port_num = rs.getInt("Public_Port");
				infos[0] = rs.getString("Public_IP");
				infos[1] = Integer.toString(port_num);	
			}
			return infos;
		}
		
	}
	
	
	public static String getEmailByUsername(String username) throws SQLException {
	    String query = "SELECT email FROM users WHERE username = ?";
	    try (Connection conn = getConnection();
	         PreparedStatement stmt = conn.prepareStatement(query)) {
	        stmt.setString(1, username);
	        ResultSet rs = stmt.executeQuery();
	        if (rs.next()) {
	            return rs.getString("email");
	        } else {
	            return null;
	        }
	    }
	}

	public static boolean check_email(String mail) throws SQLException{
		String query = "SELECT COUNT(*) username FROM users WHERE email = (?)";
		try(Connection conn = getConnection();
				PreparedStatement stmt = conn.prepareStatement(query)){
			stmt.setString(1, mail);
			ResultSet rs = stmt.executeQuery();
			if(rs.next()) {
				return rs.getInt(1) > 0;
			}
			return false;
		}
	}
	public static boolean userExists(String username) throws SQLException {
		String query = "SELECT COUNT(*) FROM users WHERE username = (?)";
		
		try(Connection conn = getConnection();
				PreparedStatement stmt = conn.prepareStatement(query)){
			
			stmt.setString(1, username);
			ResultSet rs =stmt.executeQuery();
			
			if(rs.next()) {
				return rs.getInt(1) > 0;
			}
			return false;
		}
		
	}
	
	public static boolean createUser(String username, String rawPassword, String email) throws Exception{
		String salt = CryptoUtils.generateSalt();
		String hashedPassword = CryptoUtils.hashPasswordWithSalt(rawPassword, salt);
		
		String query = "INSERT INTO users(username, password_hash, salt, email) VALUES (?, ?, ?, ?);";
		
		try(Connection conn = getConnection();
			PreparedStatement stmt = conn.prepareStatement(query)){
			
			stmt.setString(1, username);
			stmt.setString(2, hashedPassword);
			stmt.setString(3, salt);
			stmt.setString(4, email);
			
			if(stmt.executeUpdate() > 0) {
				LOGGER.info("Yeni kullanıcı oluşturuldu: " + username);
				return true;
			} else {
				LOGGER.warn("Kullanıcı oluşturulamadı: " + username);
				return false;
			}			
			
		}
	}
	public static boolean isUserBlocked(String username) throws Exception{
		String query = "SELECT username FROM blocked_users WHERE username =(?)";
		try(Connection con = getConnection();
			PreparedStatement st = con.prepareStatement(query)){
			st.setString(1, username);
			
			ResultSet rs = st.executeQuery();
			
			if(rs.next() && username.equals(rs.getString("username"))) {
				return true;
			}
			return false;
			
		}
	}
	
	public static boolean blockUser(String username, String ipAddress) throws SQLException {
	    String query = "INSERT INTO blocked_users (username, blocked_at, reason, ip_address) " +
	                   "VALUES (?, CURRENT_TIMESTAMP, 'Too many failed attempts', ?) " +
	                   "ON DUPLICATE KEY UPDATE blocked_at = CURRENT_TIMESTAMP, reason = 'Too many failed attempts', ip_address = ?";

	    try (Connection conn = getConnection();
	         PreparedStatement stmt = conn.prepareStatement(query)) {
	        stmt.setString(1, username);
	        stmt.setString(2, ipAddress);
	        stmt.setString(3, ipAddress);
	        return stmt.executeUpdate() > 0;
	    }
	}
	
	public static boolean unblockUser(String username) throws Exception {
		String query = "DELETE FROM blocked_users WHERE username = (?);";
		
		try(Connection con = getConnection();
			PreparedStatement st = con.prepareStatement(query)){
			
		 st.setString(1, username);
		
		 	return st.executeUpdate() > 0;
		 
		}
	}
	
	public static void setVerificationCode(String username, String code) throws SQLException {
	    String query = "UPDATE users SET verification_code = ? WHERE username = ?";
	    try (Connection contact = getConnection();
	         PreparedStatement ptpt = contact.prepareStatement(query)) {
	        ptpt.setString(1, code);
	        ptpt.setString(2, username);
	        ptpt.executeUpdate();
	    }
	}
	
	public static String getVerificationCode(String username) throws SQLException{
		String query = "SELECT verification_code FROM users WHERE username = (?);";
		String code = null;
		
		try(Connection contact = getConnection();
				PreparedStatement ptpt = contact.prepareStatement(query)){
			ptpt.setString(1, username);
			
			try(ResultSet rs = ptpt.executeQuery()){
				
				if(rs.next()) {
					code = rs.getString("verification_code");
					return code;

				}
				return "Method failed";
			
		}
		}
	}
	
	public static boolean Verify(String username) throws SQLException {
		
		String query = "UPDATE users SET is_verified = TRUE WHERE username = (?);";
		
		try(Connection contact = getConnection();
				PreparedStatement ptpt = contact.prepareStatement(query)){
			
			ptpt.setString(1, username);
			
			return ptpt.executeUpdate() > 0;
		}
		
		
		}
	
	
	public static boolean updateLastLogin(String username) throws SQLException {
	    String query = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE username = ?";

	    try (Connection conn = getConnection();
	         PreparedStatement stmt = conn.prepareStatement(query)) {
	        
	        stmt.setString(1, username);
	        
	        	return stmt.executeUpdate() > 0;
	    }
	}

	
	
	public static boolean updateLoginAttempts(String username) throws SQLException {
	    String query = "INSERT INTO login_attempts (username, attempt_count, last_attempt) " +
	                   "VALUES (?, 1, CURRENT_TIMESTAMP) " +
	                   "ON DUPLICATE KEY UPDATE attempt_count = attempt_count + 1, last_attempt = CURRENT_TIMESTAMP";

	    try (Connection conn = getConnection();
	         PreparedStatement stmt = conn.prepareStatement(query)) {
	        stmt.setString(1, username);

	        boolean updated = stmt.executeUpdate() > 0;
	        
	        String checkQuery = "SELECT attempt_count FROM login_attempts WHERE username = ?";

	        try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
	            checkStmt.setString(1, username);
	            ResultSet rs = checkStmt.executeQuery();
	            if (rs.next() && rs.getInt("attempt_count") >= 5) {
	                blockUser(username, "0.0.0.0");
	            }
	        }
	        return updated;
	    }
	}
	public static boolean updateVerificationAttempts(String username) throws Exception {
	    String selectQuery = "SELECT attempts, last_attempt FROM verification_attempts WHERE username = ?;";
	    String insertQuery = "INSERT INTO verification_attempts (username, attempts, last_attempt) VALUES (?, 1, CURRENT_TIMESTAMP) " +
	                         "ON DUPLICATE KEY UPDATE attempts = attempts + 1, last_attempt = CURRENT_TIMESTAMP;";
	    String blockQuery = "UPDATE users SET is_blocked = TRUE WHERE username = ?;";

	    try (Connection conn = getConnection()) {

	    	try (PreparedStatement selectStmt = conn.prepareStatement(selectQuery)) {
	            selectStmt.setString(1, username);
	            ResultSet rs = selectStmt.executeQuery();

	            int attempts = 0;
	            long lastAttemptMillis = 0;

	            if (rs.next()) {
	                attempts = rs.getInt("attempts");
	                lastAttemptMillis = rs.getTimestamp("last_attempt").getTime();
	            }
	            long now = System.currentTimeMillis();
	            if ((now - lastAttemptMillis) >= 5 * 60 * 1000) {
	                attempts = 0; 
	            }

	            if (attempts + 1 >= 3) {
	                try (PreparedStatement blockStmt = conn.prepareStatement(blockQuery)) {
	                    blockStmt.setString(1, username);
	                    blockStmt.executeUpdate();
	                }

	                EmailSender.notifyAccountLock(username);  
	                return false;
	            }
	            

	            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
	                insertStmt.setString(1, username);
	                insertStmt.executeUpdate();
	                
	                if (checkGlobalAnomaly("LOGIN")) {
	                    LOGGER.warn("Global Brute Force Attempt Detected! Temporarily blocking all verification.");
	                    Thread.sleep(10000);  
	                }

	            }

	            return true;
	        }
	    }
	}
	

	
	public static void notifyAccountLock(String username) throws Exception {
	    String email = DBManager.getEmailByUsername(username);
	    String subject = "Urgent: Your SafeRoom Account has been Locked";
	    String message = "Dear " + username + ",\n\n"
	                   + "Due to multiple incorrect verification attempts, your account has been temporarily locked.\n"
	                   + "If this was not you, please contact SafeRoom Security Team immediately.\n\n"
	                   + "Regards,\nSafeRoom Security Team";
	    
	    EmailSender.sendEmail(email, subject, message, ICON_PATH); 
	}

	public static boolean checkGlobalAnomaly(String actionType) throws Exception {
	    String table = "";
	    int threshold = 500;

	    switch (actionType) {
	        case "LOGIN":
	            table = "login_attempts";
	            threshold = 500;
	            break;
	        case "REGISTER":
	            table = "registration_attempts";
	            threshold = 300;
	            break;
	        case "VERIFY":
	            table = "verification_attempts";
	            threshold = 200;
	            break;
	        default:
	            throw new IllegalArgumentException("Unknown actionType: " + actionType);
	    }

	    String query = "SELECT COUNT(*) FROM " + table + " WHERE TIMESTAMPDIFF(SECOND, last_attempt, CURRENT_TIMESTAMP) = 0;";

	    try (Connection conn = getConnection();
	         PreparedStatement stmt = conn.prepareStatement(query)) {

	        ResultSet rs = stmt.executeQuery();
	        if (rs.next()) {
	            int count = rs.getInt(1);
	            if (count >= threshold) {
	                LOGGER.warn("Global Anomaly Detected in " + actionType + " - Count: " + count);
	                return true;
	            }
	        }
	    }
	    return false;
	}


	
	public static boolean checkVerificationCode(String username, String code) throws Exception
	{
		
		String query = "SELECT verification_code FROM users WHERE username = (?);";
		String update = "UPDATE users SET is_verified = TRUE WHERE username = (?);";
		
		try(Connection con = getConnection();
			PreparedStatement prpr = con.prepareStatement(query)){
			
			prpr.setString(1, username);
			ResultSet rs = prpr.executeQuery();
			if(rs.next()) {
				String verificationCode = rs.getString("verification_code");				
				if(code.equals(verificationCode)) {
						try(PreparedStatement updateStmt = con.prepareStatement(update)){
							updateStmt.setString(1, username);
							return updateStmt.executeUpdate() > 0;
						}
			}else {
					return false;
			}
		}else {
			return false;
		}
	}
}
	
	

	public static boolean verifyPassword(String username, String plainPassword) throws Exception {

		String query = "SELECT username, salt, password_hash FROM users WHERE username = (?);";
		
		try(Connection conn = getConnection();
				PreparedStatement prpstmt = conn.prepareStatement(query)){
			
			prpstmt.setString(1, username);
			
			ResultSet rsm = prpstmt.executeQuery();
			if(rsm.next()) {
				
				String stored_password = rsm.getString("password_hash");
				String salt = rsm.getString("salt");
				
				String users_hashed_password = CryptoUtils.hashPasswordWithSalt(plainPassword, salt); 
				
				return CryptoUtils.constantTimeEquals(stored_password, users_hashed_password);
				
			}else {
	            CryptoUtils.hashPasswordWithSalt(plainPassword, CryptoUtils.generateSalt());//Fake Hash Creator.(Constant Time Verification )
	            return false;
			}
			
		}
	
	}
	
}