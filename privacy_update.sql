-- ===============================
-- GİZLİLİK ODAKLI PROFILE SİSTEMİ
-- ===============================

-- 1. User Stats tablosunu güncelle - Güvenlik yerine Aktivite Skoru
ALTER TABLE user_stats 
CHANGE security_score activity_score DECIMAL(5,2) DEFAULT 0.0;

-- 2. Aktiviteleri daha genel hale getir - Detay bilgi yok
UPDATE user_activities 
SET activity_description = CASE 
    WHEN activity_type = 'room_created' THEN 'Created a secure room'
    WHEN activity_type = 'room_joined' THEN 'Joined a room'
    WHEN activity_type = 'file_shared' THEN 'Shared a file'
    WHEN activity_type = 'message_sent' THEN 'Sent a message'
    WHEN activity_type = 'login' THEN 'Logged in'
    WHEN activity_type = 'logout' THEN 'Logged out'
    WHEN activity_type = 'friend_request_sent' THEN 'Sent a friend request'
    WHEN activity_type = 'friend_request_received' THEN 'Received a friend request'
    ELSE activity_description
END;

-- 3. Activity data'yı temizle - Gizli bilgileri kaldır
UPDATE user_activities SET activity_data = NULL;

-- 4. Örnek gizlilik-dostu veri ekle
INSERT INTO user_stats (username, rooms_created, rooms_joined, files_shared, messages_sent, activity_score) VALUES 
('ryuzaki', 5, 12, 8, 156, 8.5),
('James', 3, 7, 4, 89, 7.2),
('ryuzaki1', 2, 5, 3, 45, 6.8),
('shadowwolf2', 1, 8, 6, 78, 7.5),
('nightfox3', 4, 6, 2, 34, 6.2)
ON DUPLICATE KEY UPDATE 
rooms_created = VALUES(rooms_created),
rooms_joined = VALUES(rooms_joined),
files_shared = VALUES(files_shared),
messages_sent = VALUES(messages_sent),
activity_score = VALUES(activity_score);

-- 5. Gizlilik-dostu aktiviteler ekle
INSERT INTO user_activities (username, activity_type, activity_description) VALUES 
('James', 'login', 'Logged in'),
('James', 'room_created', 'Created a secure room'),
('James', 'room_joined', 'Joined a room'),
('James', 'message_sent', 'Sent a message'),
('ryuzaki', 'login', 'Logged in'),
('ryuzaki', 'room_joined', 'Joined a room'),
('ryuzaki', 'message_sent', 'Sent a message'),
('ryuzaki', 'file_shared', 'Shared a file');
