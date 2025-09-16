-- Tüm tablolarda Abdurrahman'ı ara
USE saferoom;

SELECT 'users' as table_name, username FROM users WHERE username LIKE '%Abdurrahman%'
UNION ALL
SELECT 'user_sessions' as table_name, username FROM user_sessions WHERE username LIKE '%Abdurrahman%'  
UNION ALL
SELECT 'user_stats' as table_name, username FROM user_stats WHERE username LIKE '%Abdurrahman%'
UNION ALL
SELECT 'friend_requests' as table_name, sender FROM friend_requests WHERE sender LIKE '%Abdurrahman%' OR receiver LIKE '%Abdurrahman%'
UNION ALL
SELECT 'friendships' as table_name, user1 FROM friendships WHERE user1 LIKE '%Abdurrahman%' OR user2 LIKE '%Abdurrahman%';

-- Ayrıca tüm aktif session'ları göster
SELECT 'Active Sessions:' as info, username, session_id, last_heartbeat FROM user_sessions ORDER BY last_heartbeat DESC;
