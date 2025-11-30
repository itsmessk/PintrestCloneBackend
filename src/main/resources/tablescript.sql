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


-- ================================================================
-- MOCK DATA FOR DEMO
-- ================================================================

-- Insert demo users (password for all: Password123!)
INSERT INTO users (user_id, username, email, password_hash, full_name, bio, profile_picture_url, account_type, mobile_number) VALUES
('user-sundar', 'sundar', 'sundar@pinterest.com', '$2a$10$YourHashedPasswordHere', 'Sundar Pichai', 'Tech enthusiast and photography lover 📸', 'https://randomuser.me/api/portraits/men/1.jpg', 'personal', '9876543210'),
('user-kanishk', 'kanishk', 'kanishk@pinterest.com', '$2a$10$YourHashedPasswordHere', 'Kanishk Kumar', 'Travel blogger exploring the world 🌍', 'https://randomuser.me/api/portraits/men/2.jpg', 'personal', '9876543211'),
('user-adarsh', 'adarsh', 'adarsh@pinterest.com', '$2a$10$YourHashedPasswordHere', 'Adarsh Sharma', 'Food & recipe creator 🍕', 'https://randomuser.me/api/portraits/men/3.jpg', 'personal', '9876543212'),
('user-soumya', 'soumya', 'soumya@pinterest.com', '$2a$10$YourHashedPasswordHere', 'Soumya Patel', 'Interior designer & DIY expert 🏡', 'https://randomuser.me/api/portraits/women/1.jpg', 'business', '9876543213');

-- Insert business profile for Soumya
INSERT INTO business_profiles (business_id, user_id, business_name, description, website, category, logo_url, contact_email) VALUES
('business-soumya', 'user-soumya', 'Soumya Designs', 'Premium interior design and home decor solutions', 'https://soumyadesigns.com', 'Interior Design', 'https://randomuser.me/api/portraits/women/1.jpg', 'business@soumyadesigns.com');

-- Insert boards for Sundar (Technology & Photography)
INSERT INTO boards (board_id, user_id, name, description, category, visibility, is_collaborative, cover_image_url) VALUES
('board-sundar-1', 'user-sundar', 'Tech Gadgets', 'Latest technology and gadgets', 'Technology', 'public', FALSE, 'https://images.unsplash.com/photo-1498049794561-7780e7231661?w=400'),
('board-sundar-2', 'user-sundar', 'Photography Tips', 'Professional photography techniques', 'Photography', 'public', FALSE, 'https://images.unsplash.com/photo-1452587925148-ce544e77e70d?w=400'),
('board-sundar-3', 'user-sundar', 'My Workspace', 'Office setup inspiration', 'Workspace', 'private', FALSE, 'https://images.unsplash.com/photo-1484480974693-6ca0a78fb36b?w=400');

-- Insert boards for Kanishk (Travel)
INSERT INTO boards (board_id, user_id, name, description, category, visibility, is_collaborative, cover_image_url) VALUES
('board-kanishk-1', 'user-kanishk', 'Dream Destinations', 'Places I must visit', 'Travel', 'public', TRUE, 'https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=400'),
('board-kanishk-2', 'user-kanishk', 'Adventure Sports', 'Thrilling experiences around the world', 'Adventure', 'public', FALSE, 'https://images.unsplash.com/photo-1533130061792-64b345e4a833?w=400'),
('board-kanishk-3', 'user-kanishk', 'Travel Tips', 'Budget travel hacks', 'Travel', 'public', FALSE, 'https://images.unsplash.com/photo-1436491865332-7a61a109cc05?w=400');

-- Insert boards for Adarsh (Food & Recipes)
INSERT INTO boards (board_id, user_id, name, description, category, visibility, is_collaborative, cover_image_url) VALUES
('board-adarsh-1', 'user-adarsh', 'Quick Recipes', '30-minute meals', 'Food', 'public', FALSE, 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=400'),
('board-adarsh-2', 'user-adarsh', 'Desserts Heaven', 'Sweet treats and desserts', 'Food', 'public', TRUE, 'https://images.unsplash.com/photo-1488477181946-6428a0291777?w=400'),
('board-adarsh-3', 'user-adarsh', 'Healthy Meals', 'Nutritious and delicious', 'Health', 'public', FALSE, 'https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=400');

-- Insert boards for Soumya (Interior Design)
INSERT INTO boards (board_id, user_id, name, description, category, visibility, is_collaborative, cover_image_url) VALUES
('board-soumya-1', 'user-soumya', 'Modern Living Rooms', 'Contemporary living room designs', 'Home Decor', 'public', FALSE, 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=400'),
('board-soumya-2', 'user-soumya', 'Kitchen Makeovers', 'Beautiful kitchen transformations', 'Home Decor', 'public', FALSE, 'https://images.unsplash.com/photo-1556911220-bff31c812dba?w=400'),
('board-soumya-3', 'user-soumya', 'Client Projects', 'Showcase of completed work', 'Portfolio', 'public', FALSE, 'https://images.unsplash.com/photo-1616486338812-3dadae4b4ace?w=400'),
('board-soumya-4', 'user-soumya', 'DIY Home Decor', 'Budget-friendly decor ideas', 'DIY', 'public', FALSE, 'https://images.unsplash.com/photo-1513694203232-719a280e022f?w=400');

-- Insert pins for Sundar (Tech & Photography)
INSERT INTO pins (pin_id, user_id, board_id, title, description, image_url, visibility, is_draft, source_url) VALUES
('pin-sundar-1', 'user-sundar', 'board-sundar-1', 'Latest MacBook Pro', 'M3 chip review and specs', 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=400&h=600', 'public', FALSE, 'https://apple.com'),
('pin-sundar-2', 'user-sundar', 'board-sundar-1', 'Smart Home Setup', 'Complete IoT home automation', 'https://images.unsplash.com/photo-1558002038-1055907df827?w=400&h=600', 'public', FALSE, NULL),
('pin-sundar-3', 'user-sundar', 'board-sundar-1', 'Gaming Setup 2024', 'Ultimate gaming rig', 'https://images.unsplash.com/photo-1593305841991-05c297ba4575?w=400&h=600', 'public', FALSE, NULL),
('pin-sundar-4', 'user-sundar', 'board-sundar-2', 'Golden Hour Photography', 'Tips for perfect sunset shots', 'https://images.unsplash.com/photo-1495344517868-8ebaf0a2044a?w=400&h=600', 'public', FALSE, NULL),
('pin-sundar-5', 'user-sundar', 'board-sundar-2', 'Portrait Lighting Guide', 'Professional portrait setup', 'https://images.unsplash.com/photo-1542038784456-1ea8e935640e?w=400&h=600', 'public', FALSE, NULL),
('pin-sundar-6', 'user-sundar', 'board-sundar-3', 'Minimalist Desk Setup', 'Clean and productive workspace', 'https://images.unsplash.com/photo-1484480974693-6ca0a78fb36b?w=400&h=600', 'private', FALSE, NULL);

-- Insert pins for Kanishk (Travel)
INSERT INTO pins (pin_id, user_id, board_id, title, description, image_url, visibility, is_draft, source_url) VALUES
('pin-kanishk-1', 'user-kanishk', 'board-kanishk-1', 'Santorini Sunset', 'Beautiful Greek island paradise', 'https://images.unsplash.com/photo-1613395877344-13d4a8e0d49e?w=400&h=600', 'public', FALSE, NULL),
('pin-kanishk-2', 'user-kanishk', 'board-kanishk-1', 'Maldives Beach Resort', 'Crystal clear waters and white sand', 'https://images.unsplash.com/photo-1514282401047-d79a71a590e8?w=400&h=600', 'public', FALSE, NULL),
('pin-kanishk-3', 'user-kanishk', 'board-kanishk-1', 'Swiss Alps Winter', 'Snow-covered mountain peaks', 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400&h=600', 'public', FALSE, NULL),
('pin-kanishk-4', 'user-kanishk', 'board-kanishk-2', 'Skydiving Adventure', 'Freefall experience', 'https://images.unsplash.com/photo-1512553353614-82a7370096dc?w=400&h=600', 'public', FALSE, NULL),
('pin-kanishk-5', 'user-kanishk', 'board-kanishk-2', 'Bungee Jumping', 'Extreme adrenaline rush', 'https://images.unsplash.com/photo-1533130061792-64b345e4a833?w=400&h=600', 'public', FALSE, NULL),
('pin-kanishk-6', 'user-kanishk', 'board-kanishk-3', 'Budget Travel Hacks', 'Save money while traveling', 'https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=400&h=600', 'public', FALSE, NULL),
('pin-kanishk-7', 'user-kanishk', 'board-kanishk-1', 'Tokyo Night Streets', 'Vibrant Japanese nightlife', 'https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=400&h=600', 'public', TRUE, NULL);

-- Insert pins for Adarsh (Food)
INSERT INTO pins (pin_id, user_id, board_id, title, description, image_url, visibility, is_draft, source_url) VALUES
('pin-adarsh-1', 'user-adarsh', 'board-adarsh-1', 'Pasta Carbonara', 'Classic Italian recipe in 20 minutes', 'https://images.unsplash.com/photo-1612874742237-6526221588e3?w=400&h=600', 'public', FALSE, NULL),
('pin-adarsh-2', 'user-adarsh', 'board-adarsh-1', 'Chicken Stir Fry', 'Quick Asian-style dinner', 'https://images.unsplash.com/photo-1603073124479-a7d56c7ba8d6?w=400&h=600', 'public', FALSE, NULL),
('pin-adarsh-3', 'user-adarsh', 'board-adarsh-1', 'Veggie Tacos', 'Mexican street food at home', 'https://images.unsplash.com/photo-1551504734-5ee1c4a1479b?w=400&h=600', 'public', FALSE, NULL),
('pin-adarsh-4', 'user-adarsh', 'board-adarsh-2', 'Chocolate Lava Cake', 'Molten chocolate heaven', 'https://images.unsplash.com/photo-1606313564200-e75d5e30476c?w=400&h=600', 'public', FALSE, NULL),
('pin-adarsh-5', 'user-adarsh', 'board-adarsh-2', 'Tiramisu Recipe', 'Italian coffee dessert', 'https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?w=400&h=600', 'public', FALSE, NULL),
('pin-adarsh-6', 'user-adarsh', 'board-adarsh-2', 'Fruit Tart', 'Fresh and colorful dessert', 'https://images.unsplash.com/photo-1464349095431-e9a21285b5f3?w=400&h=600', 'public', FALSE, NULL),
('pin-adarsh-7', 'user-adarsh', 'board-adarsh-3', 'Quinoa Buddha Bowl', 'Nutritious power bowl', 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400&h=600', 'public', FALSE, NULL),
('pin-adarsh-8', 'user-adarsh', 'board-adarsh-3', 'Green Smoothie', 'Healthy breakfast option', 'https://images.unsplash.com/photo-1610970881699-44a5587cabec?w=400&h=600', 'public', FALSE, NULL);

-- Insert pins for Soumya (Interior Design)
INSERT INTO pins (pin_id, user_id, board_id, title, description, image_url, visibility, is_draft, is_sponsored, source_url) VALUES
('pin-soumya-1', 'user-soumya', 'board-soumya-1', 'Scandinavian Living Room', 'Minimalist Nordic design', 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=400&h=600', 'public', FALSE, FALSE, 'https://soumyadesigns.com'),
('pin-soumya-2', 'user-soumya', 'board-soumya-1', 'Cozy Reading Nook', 'Perfect corner for book lovers', 'https://images.unsplash.com/photo-1519710164239-da123dc03ef4?w=400&h=600', 'public', FALSE, FALSE, NULL),
('pin-soumya-3', 'user-soumya', 'board-soumya-1', 'Modern Fireplace Design', 'Contemporary warmth', 'https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?w=400&h=600', 'public', FALSE, TRUE, 'https://soumyadesigns.com'),
('pin-soumya-4', 'user-soumya', 'board-soumya-2', 'Marble Kitchen Island', 'Luxury kitchen centerpiece', 'https://images.unsplash.com/photo-1556911220-bff31c812dba?w=400&h=600', 'public', FALSE, FALSE, NULL),
('pin-soumya-5', 'user-soumya', 'board-soumya-2', 'Open Shelf Storage', 'Modern kitchen organization', 'https://images.unsplash.com/photo-1556912173-46c336c7fd55?w=400&h=600', 'public', FALSE, FALSE, NULL),
('pin-soumya-6', 'user-soumya', 'board-soumya-3', 'Client: Luxury Apartment', 'High-end residential project', 'https://images.unsplash.com/photo-1616486338812-3dadae4b4ace?w=400&h=600', 'public', FALSE, FALSE, NULL),
('pin-soumya-7', 'user-soumya', 'board-soumya-3', 'Office Renovation', 'Corporate space transformation', 'https://images.unsplash.com/photo-1497366216548-37526070297c?w=400&h=600', 'public', FALSE, FALSE, NULL),
('pin-soumya-8', 'user-soumya', 'board-soumya-4', 'DIY Wall Art', 'Budget-friendly decor', 'https://images.unsplash.com/photo-1513694203232-719a280e022f?w=400&h=600', 'public', FALSE, FALSE, NULL),
('pin-soumya-9', 'user-soumya', 'board-soumya-4', 'Upcycled Furniture', 'Sustainable home ideas', 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=400&h=600', 'public', FALSE, FALSE, NULL);

-- Insert follows (everyone follows everyone)
INSERT INTO follows (follow_id, follower_id, following_id) VALUES
('follow-1', 'user-sundar', 'user-kanishk'),
('follow-2', 'user-sundar', 'user-adarsh'),
('follow-3', 'user-sundar', 'user-soumya'),
('follow-4', 'user-kanishk', 'user-sundar'),
('follow-5', 'user-kanishk', 'user-adarsh'),
('follow-6', 'user-kanishk', 'user-soumya'),
('follow-7', 'user-adarsh', 'user-sundar'),
('follow-8', 'user-adarsh', 'user-kanishk'),
('follow-9', 'user-adarsh', 'user-soumya'),
('follow-10', 'user-soumya', 'user-sundar'),
('follow-11', 'user-soumya', 'user-kanishk'),
('follow-12', 'user-soumya', 'user-adarsh');

-- Insert board collaborators (Sundar and Adarsh collaborate on Desserts board, Kanishk on Dream Destinations)
INSERT INTO board_collaborators (collaborator_id, board_id, user_id, permission) VALUES
('collab-1', 'board-adarsh-2', 'user-sundar', 'EDIT'),
('collab-2', 'board-kanishk-1', 'user-soumya', 'VIEW');

-- Insert invitations
INSERT INTO invitations (invitation_id, board_id, from_user_id, to_user_id, message, permission, status) VALUES
('invite-1', 'board-adarsh-2', 'user-adarsh', 'user-sundar', 'Hey Sundar! Want to collaborate on my desserts board?', 'EDIT', 'ACCEPTED'),
('invite-2', 'board-kanishk-1', 'user-kanishk', 'user-soumya', 'Check out my travel destinations!', 'VIEW', 'ACCEPTED'),
('invite-3', 'board-soumya-1', 'user-soumya', 'user-adarsh', 'Would love your input on interior designs!', 'EDIT', 'PENDING');

-- Insert pin likes (cross-engagement)
INSERT INTO pin_likes (like_id, pin_id, user_id) VALUES
('like-1', 'pin-sundar-1', 'user-kanishk'),
('like-2', 'pin-sundar-1', 'user-adarsh'),
('like-3', 'pin-kanishk-1', 'user-sundar'),
('like-4', 'pin-kanishk-1', 'user-soumya'),
('like-5', 'pin-adarsh-4', 'user-sundar'),
('like-6', 'pin-adarsh-4', 'user-kanishk'),
('like-7', 'pin-soumya-1', 'user-sundar'),
('like-8', 'pin-soumya-1', 'user-adarsh'),
('like-9', 'pin-soumya-3', 'user-kanishk');

-- Insert saved pins (users saving each other's content)
INSERT INTO saved_pins (save_id, pin_id, user_id, board_id) VALUES
('save-1', 'pin-kanishk-1', 'user-sundar', 'board-sundar-2'),
('save-2', 'pin-adarsh-4', 'user-kanishk', 'board-kanishk-3'),
('save-3', 'pin-soumya-1', 'user-adarsh', 'board-adarsh-3'),
('save-4', 'pin-sundar-1', 'user-soumya', 'board-soumya-4');

-- Insert sponsored pin for Soumya's business
INSERT INTO sponsored_pins (sponsored_id, pin_id, business_id, campaign_name, budget, spent, status, impressions, clicks, saves, start_date, end_date) VALUES
('sponsored-1', 'pin-soumya-3', 'business-soumya', 'Modern Fireplace Promotion', 5000.00, 1250.50, 'ACTIVE', 15000, 450, 89, '2025-11-01', '2025-12-31');

-- Insert business showcase for Soumya
INSERT INTO business_showcases (showcase_id, business_id, title, description, theme, is_active) VALUES
('showcase-1', 'business-soumya', 'Featured Projects 2025', 'Our best interior design work', 'modern', TRUE);

INSERT INTO showcase_pins (showcase_pin_id, showcase_id, pin_id, display_order) VALUES
('showpin-1', 'showcase-1', 'pin-soumya-1', 1),
('showpin-2', 'showcase-1', 'pin-soumya-6', 2),
('showpin-3', 'showcase-1', 'pin-soumya-7', 3);

-- Insert notifications
INSERT INTO notifications (notification_id, user_id, sender_id, type, message, entity_id, entity_type, is_read) VALUES
('notif-1', 'user-sundar', 'user-adarsh', 'INVITATION_RECEIVED', 'Adarsh invited you to collaborate on Desserts Heaven', 'invite-1', 'invitation', TRUE),
('notif-2', 'user-adarsh', 'user-sundar', 'INVITATION_ACCEPTED', 'Sundar accepted your collaboration invitation', 'invite-1', 'invitation', FALSE),
('notif-3', 'user-soumya', 'user-kanishk', 'NEW_FOLLOWER', 'Kanishk started following you', 'user-kanishk', 'user', FALSE),
('notif-4', 'user-sundar', 'user-kanishk', 'PIN_LIKED', 'Kanishk liked your pin', 'pin-sundar-1', 'pin', FALSE),
('notif-5', 'user-adarsh', 'user-soumya', 'INVITATION_RECEIVED', 'Soumya invited you to collaborate on Modern Living Rooms', 'invite-3', 'invitation', FALSE);


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
