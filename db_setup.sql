CREATE DATABASE IF NOT EXISTS email_db;
USE email_db;

-- 1. Table users
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) DEFAULT 'active'
);

-- 2. Table emails
CREATE TABLE IF NOT EXISTS emails (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sender VARCHAR(255) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255),
    content TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Stored Procedures (Version compatible)

DELIMITER //

-- authenticate_user
DROP PROCEDURE IF EXISTS authenticate_user //
CREATE PROCEDURE authenticate_user(IN p_username VARCHAR(255), IN p_password VARCHAR(255))
BEGIN
    SELECT * FROM users WHERE username = p_username AND password = p_password;
END //

-- store_email
DROP PROCEDURE IF EXISTS store_email //
CREATE PROCEDURE store_email(IN p_sender VARCHAR(255), IN p_recipient VARCHAR(255), IN p_subject VARCHAR(255), IN p_content TEXT)
BEGIN
    INSERT INTO emails (sender, recipient, subject, content) VALUES (p_sender, p_recipient, p_subject, p_content);
END //

-- fetch_emails
DROP PROCEDURE IF EXISTS fetch_emails //
CREATE PROCEDURE fetch_emails(IN p_username VARCHAR(255))
BEGIN
    -- Match by exact username OR by the username@any-domain format stored by SMTP
    SELECT * FROM emails
    WHERE recipient = p_username
       OR recipient LIKE CONCAT(p_username, '@%')
    ORDER BY created_at DESC;
END //

-- delete_email
DROP PROCEDURE IF EXISTS delete_email //
CREATE PROCEDURE delete_email(IN p_email_id INT)
BEGIN
    DELETE FROM emails WHERE id = p_email_id;
END //

-- update_password
DROP PROCEDURE IF EXISTS update_password //
CREATE PROCEDURE update_password(IN p_username VARCHAR(255), IN p_new_password VARCHAR(255))
BEGIN
    UPDATE users SET password = p_new_password WHERE username = p_username;
END //

DELIMITER ;

-- Insert initial admin user
INSERT IGNORE INTO users (username, password) VALUES ('admin', 'admin123');
INSERT IGNORE INTO users (username, password) VALUES ('salah', 'salah123');
