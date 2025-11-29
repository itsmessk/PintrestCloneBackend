DROP DATABASE IF EXISTS pintrest_db;
CREATE DATABASE pintrest_db;
USE pintrest_db;

CREATE TABLE users (
    user_id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    bio TEXT,
    profile_picture_url VARCHAR(500),
    mobile_number VARCHAR(20),
    account_type ENUM('personal', 'business') DEFAULT 'personal',
    is_active BOOLEAN DEFAULT TRUE,
    failed_login_attempts INT DEFAULT 0,
    last_failed_login_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE password_reset_tokens (
    token_id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id VARCHAR(36) NOT NULL,
    email VARCHAR(100) NOT NULL,
    otp VARCHAR(10) NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_email (email),
    INDEX idx_otp (otp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE boards (
    board_id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    cover_image_url VARCHAR(500),
    visibility ENUM('public', 'private') DEFAULT 'public',
    is_collaborative BOOLEAN DEFAULT FALSE,
    pin_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_visibility (visibility)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pins (
    pin_id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id VARCHAR(36) NOT NULL,
    board_id VARCHAR(36) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    image_url VARCHAR(500) NOT NULL,
    source_url VARCHAR(500),
    visibility ENUM('public', 'private') DEFAULT 'public',
    is_draft BOOLEAN DEFAULT FALSE,
    is_sponsored BOOLEAN DEFAULT FALSE,
    save_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (board_id) REFERENCES boards(board_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_board_id (board_id),
    INDEX idx_is_draft (is_draft),
    INDEX idx_is_sponsored (is_sponsored),
    FULLTEXT idx_title_description (title, description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pin_saves (
    save_id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    pin_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    board_id VARCHAR(36) NOT NULL,
    saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (pin_id) REFERENCES pins(pin_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (board_id) REFERENCES boards(board_id) ON DELETE CASCADE,
    UNIQUE KEY unique_pin_user_board (pin_id, user_id, board_id),
    INDEX idx_pin_id (pin_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE follows (
    follow_id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    follower_id VARCHAR(36) NOT NULL,
    following_id VARCHAR(36) NOT NULL,
    followed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (follower_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (following_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE KEY unique_follower_following (follower_id, following_id),
    INDEX idx_follower_id (follower_id),
    INDEX idx_following_id (following_id),
    CHECK (follower_id != following_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE invitations (
    invitation_id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    board_id VARCHAR(36) NOT NULL,
    from_user_id VARCHAR(36) NOT NULL,
    to_user_id VARCHAR(36) NOT NULL,
    message TEXT,
    permission ENUM('VIEW', 'EDIT') DEFAULT 'EDIT',
    status ENUM('PENDING', 'ACCEPTED', 'DECLINED', 'IGNORED') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP NULL,

    FOREIGN KEY (board_id) REFERENCES boards(board_id) ON DELETE CASCADE,
    FOREIGN KEY (from_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (to_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_to_user_id (to_user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE board_collaborators (
    collaborator_id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    board_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    permission ENUM('VIEW', 'EDIT') DEFAULT 'EDIT',
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (board_id) REFERENCES boards(board_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE KEY unique_board_user (board_id, user_id),
    INDEX idx_board_id (board_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notifications (
    notification_id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id VARCHAR(36) NOT NULL,
    sender_id VARCHAR(36),
    type ENUM('INVITATION_RECEIVED', 'INVITATION_ACCEPTED', 'INVITATION_DECLINED', 'NEW_FOLLOWER', 'PIN_SAVED', 'PIN_LIKED', 'BOARD_SHARED', 'COLLABORATION_ADDED') NOT NULL,
    message VARCHAR(500) NOT NULL,
    entity_id VARCHAR(36),
    entity_type VARCHAR(50),
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,

    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_id_created (user_id, created_at DESC),
    INDEX idx_user_id_read (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE business_profiles (
    business_id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id VARCHAR(36) NOT NULL UNIQUE,
    business_name VARCHAR(100) NOT NULL,
    description TEXT,
    website VARCHAR(255),
    category VARCHAR(50),
    logo_url VARCHAR(500),
    contact_email VARCHAR(100),
    follower_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_business_name (business_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sponsored_pins (
    sponsored_id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    pin_id VARCHAR(36) NOT NULL,
    business_id VARCHAR(36) NOT NULL,
    campaign_name VARCHAR(100) NOT NULL,
    budget DECIMAL(10, 2) DEFAULT 0.00,
    spent DECIMAL(10, 2) DEFAULT 0.00,
    status ENUM('PENDING', 'ACTIVE', 'PAUSED', 'COMPLETED') DEFAULT 'PENDING',
    impressions INT DEFAULT 0,
    clicks INT DEFAULT 0,
    saves INT DEFAULT 0,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (pin_id) REFERENCES pins(pin_id) ON DELETE CASCADE,
    FOREIGN KEY (business_id) REFERENCES business_profiles(business_id) ON DELETE CASCADE,
    INDEX idx_pin_id (pin_id),
    INDEX idx_business_id (business_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pin_likes (
    like_id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    pin_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    liked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (pin_id) REFERENCES pins(pin_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE KEY unique_pin_user_like (pin_id, user_id),
    INDEX idx_pin_id (pin_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE saved_pins (
    save_id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    pin_id VARCHAR(36) NOT NULL,
    copied_pin_id VARCHAR(36),
    user_id VARCHAR(36) NOT NULL,
    board_id VARCHAR(36),
    saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (pin_id) REFERENCES pins(pin_id) ON DELETE CASCADE,
    FOREIGN KEY (copied_pin_id) REFERENCES pins(pin_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (board_id) REFERENCES boards(board_id) ON DELETE SET NULL,
    UNIQUE KEY unique_pin_user_board_save (pin_id, user_id, board_id),
    INDEX idx_pin_id (pin_id),
    INDEX idx_user_id (user_id),
    INDEX idx_board_id (board_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE blocked_users (
    block_id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    blocker_id VARCHAR(36) NOT NULL,
    blocked_id VARCHAR(36) NOT NULL,
    blocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (blocker_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (blocked_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE KEY unique_blocker_blocked (blocker_id, blocked_id),
    INDEX idx_blocker_id (blocker_id),
    INDEX idx_blocked_id (blocked_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE business_showcases (
    showcase_id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    business_id VARCHAR(36) NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    theme VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    display_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (business_id) REFERENCES business_profiles(business_id) ON DELETE CASCADE,
    INDEX idx_business_id (business_id),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE showcase_pins (
    showcase_pin_id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    showcase_id VARCHAR(36) NOT NULL,
    pin_id VARCHAR(36) NOT NULL,
    display_order INT DEFAULT 0,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (showcase_id) REFERENCES business_showcases(showcase_id) ON DELETE CASCADE,
    FOREIGN KEY (pin_id) REFERENCES pins(pin_id) ON DELETE CASCADE,
    UNIQUE KEY unique_showcase_pin (showcase_id, pin_id),
    INDEX idx_showcase_id (showcase_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_reports (
    report_id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    reporter_id VARCHAR(36) NOT NULL,
    reported_user_id VARCHAR(36) NOT NULL,
    reason ENUM('spam', 'harassment', 'inappropriate', 'other') NOT NULL,
    description TEXT,
    status ENUM('pending', 'under_review', 'resolved', 'dismissed') DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (reporter_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (reported_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_reported_user_id (reported_user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- Trigger to update pin_count when a pin is added to a board
DELIMITER //
CREATE TRIGGER after_pin_insert
AFTER INSERT ON pins
FOR EACH ROW
BEGIN
    UPDATE boards
    SET pin_count = pin_count + 1
    WHERE board_id = NEW.board_id;
END//
DELIMITER ;

-- Trigger to update pin_count when a pin is deleted
DELIMITER //
CREATE TRIGGER after_pin_delete
AFTER DELETE ON pins
FOR EACH ROW
BEGIN
    UPDATE boards
    SET pin_count = pin_count - 1
    WHERE board_id = OLD.board_id;
END//
DELIMITER ;

-- Trigger to update save_count when a pin is saved
DELIMITER //
CREATE TRIGGER after_pin_save_insert
AFTER INSERT ON pin_saves
FOR EACH ROW
BEGIN
    UPDATE pins
    SET save_count = save_count + 1
    WHERE pin_id = NEW.pin_id;
END//
DELIMITER ;

-- Trigger to update like_count when a like is added
DELIMITER //
CREATE TRIGGER after_pin_like_insert
AFTER INSERT ON pin_likes
FOR EACH ROW
BEGIN
    UPDATE pins
    SET like_count = like_count + 1
    WHERE pin_id = NEW.pin_id;
END//
DELIMITER ;

-- Trigger to update like_count when a like is removed
DELIMITER //
CREATE TRIGGER after_pin_like_delete
AFTER DELETE ON pin_likes
FOR EACH ROW
BEGIN
    UPDATE pins
    SET like_count = like_count - 1
    WHERE pin_id = OLD.pin_id;
END//
DELIMITER ;


-- View: User Statistics
CREATE VIEW user_stats AS
SELECT
    u.user_id,
    u.username,
    u.email,
    u.full_name,
    COUNT(DISTINCT b.board_id) as board_count,
    COUNT(DISTINCT p.pin_id) as pin_count,
    (SELECT COUNT(*) FROM follows WHERE following_id = u.user_id) as follower_count,
    (SELECT COUNT(*) FROM follows WHERE follower_id = u.user_id) as following_count
FROM users u
LEFT JOIN boards b ON u.user_id = b.user_id
LEFT JOIN pins p ON u.user_id = p.user_id
GROUP BY u.user_id;

-- View: Board Statistics
CREATE VIEW board_stats AS
SELECT
    b.board_id,
    b.name,
    b.user_id,
    b.visibility,
    b.pin_count,
    u.username as owner_username,
    COUNT(DISTINCT bc.user_id) as collaborator_count
FROM boards b
JOIN users u ON b.user_id = u.user_id
LEFT JOIN board_collaborators bc ON b.board_id = bc.board_id
GROUP BY b.board_id;

-- View: Pin Statistics
CREATE VIEW pin_stats AS
SELECT
    p.pin_id,
    p.title,
    p.user_id,
    p.board_id,
    p.visibility,
    p.is_sponsored,
    p.save_count,
    p.created_at,
    u.username as creator_username,
    b.name as board_name
FROM pins p
JOIN users u ON p.user_id = u.user_id
JOIN boards b ON p.board_id = b.board_id;


-- Insert sample users
INSERT INTO users (user_id, username, email, password_hash, full_name, bio, account_type) VALUES
('user-1', 'johndoe', 'john@example.com', '$2a$10$dummyhash1', 'John Doe', 'Travel enthusiast', 'personal'),
('user-2', 'janedoe', 'jane@example.com', '$2a$10$dummyhash2', 'Jane Doe', 'Food blogger', 'personal'),
('user-3', 'acmecorp', 'contact@acme.com', '$2a$10$dummyhash3', 'Acme Corporation', 'Leading tech company', 'business');

-- Insert sample boards
INSERT INTO boards (board_id, user_id, name, description, category, visibility) VALUES
('board-1', 'user-1', 'Travel Inspiration', 'Places I want to visit', 'travel', 'public'),
('board-2', 'user-1', 'Recipes', 'Delicious food ideas', 'food', 'public'),
('board-3', 'user-2', 'Home Decor', 'Interior design ideas', 'home', 'public');

-- Insert sample pins
INSERT INTO pins (pin_id, user_id, board_id, title, description, image_url, visibility) VALUES
('pin-1', 'user-1', 'board-1', 'Beautiful Sunset', 'Amazing sunset at the beach', 'https://picsum.photos/400/600', 'public'),
('pin-2', 'user-1', 'board-2', 'Chocolate Cake', 'Delicious chocolate cake recipe', 'https://picsum.photos/400/600', 'public'),
('pin-3', 'user-2', 'board-3', 'Modern Living Room', 'Minimalist design inspiration', 'https://picsum.photos/400/600', 'public');

-- Insert sample follows
INSERT INTO follows (follow_id, follower_id, following_id) VALUES
('follow-1', 'user-2', 'user-1'),
('follow-2', 'user-1', 'user-2');


-- ================================================================
-- USEFUL QUERIES FOR TESTING
-- ================================================================

-- Get user with statistics
-- SELECT * FROM user_stats WHERE user_id = 'user-1';

-- Get all public pins with creator info
-- SELECT p.*, u.username, u.profile_picture_url
-- FROM pins p
-- JOIN users u ON p.user_id = u.user_id
-- WHERE p.visibility = 'public' AND p.is_draft = FALSE;

-- Get user's followers
-- SELECT u.user_id, u.username, u.profile_picture_url
-- FROM follows f
-- JOIN users u ON f.follower_id = u.user_id
-- WHERE f.following_id = 'user-1';

-- Get user's following
-- SELECT u.user_id, u.username, u.profile_picture_url
-- FROM follows f
-- JOIN users u ON f.following_id = u.user_id
-- WHERE f.follower_id = 'user-1';

-- Search pins by keyword
-- SELECT * FROM pins
-- WHERE MATCH(title, description) AGAINST('sunset' IN NATURAL LANGUAGE MODE);
