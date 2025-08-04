package chat;

import chat.model.ChatRoom;
import chat.model.Contact;
import chat.model.FriendRequest;
import chat.model.User;
import chat.model.UserDetail;
import chat.util.PasswordUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import chat.model.ChatMessage; // 确保导入了 ChatMessage 模型
import java.time.LocalDateTime; // 确保导入

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class DatabaseUtil {

    private static HikariDataSource dataSource;

    // 静态初始化数据库连接池
    static {
        try {
            HikariConfig config = new HikariConfig();
            // 数据库连接配置 (已更新为 testv2 数据库)
            config.setJdbcUrl("jdbc:mysql://192.168.192.249:3306/testv2?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true");            //
            config.setUsername("LinkWheat"); //
            config.setPassword("123456"); //
            config.setDriverClassName("com.mysql.cj.jdbc.Driver"); //

            // 保留您的连接池优化配置
            config.setMaximumPoolSize(20); //
            config.setMinimumIdle(2); //
            config.setConnectionTimeout(30000); //
            config.setIdleTimeout(600000); //
            config.setMaxLifetime(1800000); //
            config.addDataSourceProperty("cachePrepStmts", "true"); //
            config.addDataSourceProperty("prepStmtCacheSize", "250"); //
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048"); //
            config.addDataSourceProperty("useServerPrepStmts", "true"); //

            dataSource = new HikariDataSource(config);

            // 初始化V1.0所需的数据库表
            initializeDatabase();

        } catch (Exception e) {
            System.err.println("数据库连接池初始化失败！请检查JDBC URL、用户名/密码以及网络连接。"); //
            e.printStackTrace();
            throw new ExceptionInInitializerError(e); //
        }
    }

    /**
     * 初始化V1.0的数据库表结构 (users表)
     */
    private static void initializeDatabase() {
        // V1.0 users 表的创建语句
        String createTableSQL = "CREATE TABLE IF NOT EXISTS users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "account VARCHAR(20) NOT NULL UNIQUE, " +
                "password_hash VARCHAR(255) NOT NULL, " +
                "nickname VARCHAR(50) NOT NULL, " +
                "avatar BLOB, " +
                "security_question VARCHAR(255) NOT NULL, " +
                "security_answer_hash VARCHAR(255) NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "INDEX idx_account (account)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("V1.0 'users' 数据库表初始化完成！");
        } catch (SQLException e) {
            System.err.println("数据库表初始化失败: " + e.getMessage());
        }
    }

    /**
     * 检查账号是否存在
     * @param account 要检查的账号
     * @return 如果存在返回true, 否则返回false
     */
    public static boolean accountExists(String account) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE account = ? LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, account);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * 注册新用户
     * @return 如果注册成功返回true
     */
    public static boolean registerUser(String account, String password, String nickname, byte[] avatar, String question, String answer) throws SQLException {
        String sql = "INSERT INTO users (account, password_hash, nickname, avatar, security_question, security_answer_hash) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, account);
            pstmt.setString(2, PasswordUtil.hash(password));
            pstmt.setString(3, nickname);
            pstmt.setBytes(4, avatar);
            pstmt.setString(5, question);
            pstmt.setString(6, PasswordUtil.hash(answer));
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 根据账号和密码验证用户登录
     * @return 成功则返回User对象，失败则返回null
     */
//    public static User login(String account, String password) throws SQLException {
//        String sql = "SELECT id, account, nickname, password_hash FROM users WHERE account = ?";
//        try (Connection conn = dataSource.getConnection();
//             PreparedStatement pstmt = conn.prepareStatement(sql)) {
//            pstmt.setString(1, account);
//            try (ResultSet rs = pstmt.executeQuery()) {
//                if (rs.next()) {
//                    if (PasswordUtil.check(password, rs.getString("password_hash"))) {
//                        return new User(rs.getInt("id"), rs.getString("account"), rs.getString("nickname"));
//                    }
//                }
//            }
//        }
//        return null;
//    }

    /**
     * 根据用户ID获取用户信息
     * @return 成功则返回User对象，失败则返回null
     */
    public static User getUserById(int userId) throws SQLException {
        String sql = "SELECT id, account, nickname FROM users WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getInt("id"), rs.getString("account"), rs.getString("nickname"));
                }
            }
        }
        return null;
    }

    /**
     * 根据账号获取密保问题
     * @return 找到则返回问题字符串，否则返回null
     */
    public static String getSecurityQuestion(String account) throws SQLException {
        String sql = "SELECT security_question FROM users WHERE account = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, account);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("security_question");
                }
            }
        }
        return null;
    }

    /**
     * 通过验证密保答案来重置密码
     * @return 成功返回true, 否则返回false
     */
    public static boolean resetPasswordByAnswer(String account, String answer, String newPassword) throws SQLException {
        String sql = "SELECT security_answer_hash FROM users WHERE account = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, account);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && PasswordUtil.check(answer, rs.getString("security_answer_hash"))) {
                    return updatePassword(conn, account, newPassword);
                }
            }
        }
        return false;
    }

    /**
     * 通过验证旧密码来修改密码
     * @return 成功返回true, 否则返回false
     */
    public static boolean changePasswordByOldPassword(int userId, String oldPassword, String newPassword) throws SQLException {
        String sql = "SELECT password_hash FROM users WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && PasswordUtil.check(oldPassword, rs.getString("password_hash"))) {
                    String accountSql = "SELECT account FROM users WHERE id = ?";
                    try(PreparedStatement accPstmt = conn.prepareStatement(accountSql)){
                        accPstmt.setInt(1, userId);
                        ResultSet accRs = accPstmt.executeQuery();
                        if(accRs.next()){
                            return updatePassword(conn, accRs.getString("account"), newPassword);
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * 更新用户的昵称和/或头像
     */
    public static boolean updateUserProfile(int userId, String newNickname, byte[] newAvatarBytes) throws SQLException {
        // 根据传入参数动态构建SQL语句
        StringBuilder sql = new StringBuilder("UPDATE users SET ");
        boolean needsComma = false;
        if(newNickname != null && !newNickname.isEmpty()){
            sql.append("nickname = ?");
            needsComma = true;
        }
        if(newAvatarBytes != null && newAvatarBytes.length > 0){
            if(needsComma) sql.append(", ");
            sql.append("avatar = ?");
        }
        sql.append(" WHERE id = ?");

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if(newNickname != null && !newNickname.isEmpty()){
                pstmt.setString(paramIndex++, newNickname);
            }
            if(newAvatarBytes != null && newAvatarBytes.length > 0){
                pstmt.setBytes(paramIndex++, newAvatarBytes);
            }
            pstmt.setInt(paramIndex, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 根据用户ID获取头像
     * @return 头像的byte数组，或null
     */
    public static byte[] getAvatar(int userId) throws SQLException {
        String sql = "SELECT avatar FROM users WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBytes("avatar");
                }
            }
        }
        return null;
    }

    /**
     * 内部辅助方法，用于更新密码
     */
    private static boolean updatePassword(Connection conn, String account, String newPassword) throws SQLException {
        String updateSql = "UPDATE users SET password_hash = ? WHERE account = ?";
        try (PreparedStatement updatePstmt = conn.prepareStatement(updateSql)) {
            updatePstmt.setString(1, PasswordUtil.hash(newPassword));
            updatePstmt.setString(2, account);
            return updatePstmt.executeUpdate() > 0;
        }
    }

    /**
     * 关闭数据库连接池
     */
    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("数据库连接池已关闭");
        }
    }



    // 在 DatabaseUtil.java 中

    public static User login(String account, String password) throws SQLException {
        String sql = "SELECT id, account, nickname, password_hash FROM users WHERE account = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, account);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String passwordHash = rs.getString("password_hash");

                    // =========== 关键的调试代码开始 ===========
                    System.out.println("\n--- 登录调试信息 ---");
                    System.out.println("请求登录的账号: [" + account + "]");
                    System.out.println("用户输入的密码 (trim后): [" + password + "]"); // 假设您已在Servlet中添加了trim()
                    System.out.println("数据库存储的哈希: [" + passwordHash + "]");
                    boolean passwordsMatch = PasswordUtil.check(password, passwordHash);
                    System.out.println("密码是否匹配 (check结果): " + passwordsMatch);
                    System.out.println("--- 调试信息结束 ---\n");
                    // =========== 关键的调试代码结束 ===========

                    if (passwordsMatch) { // 使用我们刚刚计算的布尔值
                        return new User(rs.getInt("id"), rs.getString("account"), rs.getString("nickname"));
                    }
                }
            }
        }
        return null; // 验证失败
    }

    // 添加到 DatabaseUtil.java

    /**
     * [新增] 通过验证密保答案来修改已登录用户的密码
     * @param userId 用户ID
     * @param answer 用户输入的答案
     * @param newPassword 新密码
     * @return 成功返回true, 否则返回false
     */
    public static boolean changePasswordByAnswer(int userId, String answer, String newPassword) throws SQLException {
        // 首先获取该用户的账号和答案哈希值
        String sql = "SELECT account, security_answer_hash FROM users WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && PasswordUtil.check(answer, rs.getString("security_answer_hash"))) {
                    // 答案正确，调用内部方法更新密码
                    String account = rs.getString("account");
                    return updatePassword(conn, account, newPassword);
                }
            }
        }
        return false;
    }

    /**
     * [新增] 根据用户ID获取用户的详细信息（包括安全问题）
     * @param userId 用户ID
     * @return 成功则返回UserDetail对象，失败则返回null
     */
    public static UserDetail getUserDetailsById(int userId) throws SQLException {
        String sql = "SELECT id, account, nickname, security_question FROM users WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new UserDetail(
                            rs.getInt("id"),
                            rs.getString("account"),
                            rs.getString("nickname"),
                            rs.getString("security_question")
                    );
                }
            }
        }
        return null;
    }

    // ==================== V2.0 新增方法 ====================

    /**
     * 根据账号或昵称模糊搜索用户
     * @param currentUserId 当前登录用户的ID，用于排除自己和已是好友的用户
     * @param query 搜索关键词
     * @return 用户列表
     */
    public static List<User> searchUsers(int currentUserId, String query) throws SQLException {
        List<User> users = new ArrayList<>();
        // SQL: 查找非自己、非好友的用户
        String sql = "SELECT id, account, nickname FROM users u " +
                "WHERE (u.account LIKE ? OR u.nickname LIKE ?) AND u.id != ? " +
                "AND u.id NOT IN (SELECT contact_user_id FROM contacts WHERE user_id = ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String searchQuery = "%" + query + "%";
            pstmt.setString(1, searchQuery);
            pstmt.setString(2, searchQuery);
            pstmt.setInt(3, currentUserId);
            pstmt.setInt(4, currentUserId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(new User(rs.getInt("id"), rs.getString("account"), rs.getString("nickname")));
                }
            }
        }
        return users;
    }

    /**
     * [V2.2 最终修复版] 发送好友请求。
     * 使用 INSERT ... ON DUPLICATE KEY UPDATE 来处理全新请求和重新请求的逻辑。
     * @return 总是返回true，因为这个操作要么插入要么更新，逻辑上总会成功。
     * @throws SQLException 如果发生其他数据库错误
     */
    public static boolean sendFriendRequest(int senderId, int receiverId) throws SQLException {
        // 基础验证：不能添加自己为好友
        if (senderId == receiverId) {
            System.err.println("错误：用户 " + senderId + " 尝试添加自己为好友。");
            return false;
        }

        // 关键SQL：如果 (sender_id, receiver_id) 这个唯一键已存在，则更新status；否则，插入新行。
        String sql = "INSERT INTO friend_requests (sender_id, receiver_id, status) VALUES (?, ?, 'pending') " +
                "ON DUPLICATE KEY UPDATE status = 'pending'";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, senderId);
            pstmt.setInt(2, receiverId);

            pstmt.executeUpdate();

            // 这个操作要么插入(返回1)，要么更新(返回2)，只要没抛出异常，就视为成功。
            return true;

        } catch (SQLException e) {
            System.err.println("[DB 严重错误] sendFriendRequest 发生SQL异常！");
            throw e; // 向上抛出，让Servlet处理500错误
        }
    }

//    /**
//     * 获取指定用户收到的待处理好友请求
//     */
//    public static List<FriendRequest> getPendingFriendRequests(int receiverId) throws SQLException {
//        List<FriendRequest> requests = new ArrayList<>();
//        String sql = "SELECT fr.id, fr.sender_id, u.nickname FROM friend_requests fr " +
//                "JOIN users u ON fr.sender_id = u.id " +
//                "WHERE fr.receiver_id = ? AND fr.status = 'pending'";
//        try (Connection conn = dataSource.getConnection();
//             PreparedStatement pstmt = conn.prepareStatement(sql)) {
//            pstmt.setInt(1, receiverId);
//            try (ResultSet rs = pstmt.executeQuery()) {
//                while (rs.next()) {
//                    requests.add(new FriendRequest(rs.getInt("id"), rs.getInt("sender_id"), rs.getString("nickname")));
//                }
//            }
//        }
//        return requests;
//    }

    /**
     * 响应好友请求 (接受或拒绝)
     * @return 是否成功
     */
    /**
     * [V2.0 修复版] 响应好友请求 (接受或拒绝)
     * @return 是否成功
     */
    public static boolean respondToFriendRequest(int requestId, int currentUserId, String status) throws SQLException {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false); // 开启事务

            // 1. 更新请求状态
            String updateSql = "UPDATE friend_requests SET status = ? WHERE id = ? AND receiver_id = ?";
            try (PreparedStatement updatePstmt = conn.prepareStatement(updateSql)) {
                updatePstmt.setString(1, status);
                updatePstmt.setInt(2, requestId);
                updatePstmt.setInt(3, currentUserId);
                if (updatePstmt.executeUpdate() == 0) {
                    throw new SQLException("请求不存在或无权操作");
                }
            }

            // 2. 如果是接受请求，则双向添加好友关系
            if ("accepted".equals(status)) {
                String selectSql = "SELECT sender_id FROM friend_requests WHERE id = ?";
                int senderId;
                try (PreparedStatement selectPstmt = conn.prepareStatement(selectSql)) {
                    selectPstmt.setInt(1, requestId);
                    ResultSet rs = selectPstmt.executeQuery();
                    if (rs.next()) {
                        senderId = rs.getInt("sender_id");
                    } else {
                        throw new SQLException("找不到请求发送者");
                    }
                }

                // 关键修复：使用 INSERT IGNORE 来防止因好友关系已存在而导致的崩溃
                String insertSql = "INSERT IGNORE INTO contacts (user_id, contact_user_id) VALUES (?, ?), (?, ?)";
                try (PreparedStatement insertPstmt = conn.prepareStatement(insertSql)) {
                    insertPstmt.setInt(1, currentUserId);
                    insertPstmt.setInt(2, senderId);
                    insertPstmt.setInt(3, senderId);
                    insertPstmt.setInt(4, currentUserId);
                    insertPstmt.executeUpdate();
                }
            }
            conn.commit(); // 提交事务
            return true;
        } catch (SQLException e) {
            if (conn != null) conn.rollback(); // 回滚事务
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    /**
     * 获取用户的所有联系人
     */
    public static List<Contact> getContacts(int userId) throws SQLException {
        List<Contact> contacts = new ArrayList<>();
        String sql = "SELECT u.id, u.account, u.nickname, c.remark_name FROM users u " +
                "JOIN contacts c ON u.id = c.contact_user_id WHERE c.user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while(rs.next()){
                    contacts.add(new Contact(rs.getInt("id"), rs.getString("account"), rs.getString("nickname"), rs.getString("remark_name")));
                }
            }
        }
        return contacts;
    }

    // 添加到 DatabaseUtil.java 文件中

    /**
     * [V2.2 最终修复版] 双向删除联系人关系，并同步删除对应的私聊房间。
     * 这是一个事务性操作。
     * @param userId 发起删除的用户ID
     * @param contactId 被删除的联系人ID
     * @return 如果操作成功返回true
     */
    public static boolean deleteContact(int userId, int contactId) throws SQLException {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false); // 开启事务

            // 步骤 1: 查找这两个用户唯一的私聊房间ID
            String findRoomSql = "SELECT room_id FROM chat_room_members " +
                    "WHERE user_id IN (?, ?) " +
                    "GROUP BY room_id " +
                    "HAVING COUNT(DISTINCT user_id) = 2 " +
                    "AND room_id IN (SELECT id FROM chat_rooms WHERE is_private = TRUE)";

            int roomIdToDelete = -1;
            try (PreparedStatement findPstmt = conn.prepareStatement(findRoomSql)) {
                findPstmt.setInt(1, userId);
                findPstmt.setInt(2, contactId);
                try (ResultSet rs = findPstmt.executeQuery()) {
                    if (rs.next()) {
                        roomIdToDelete = rs.getInt("room_id");
                    }
                }
            }

            // 步骤 2: 如果找到了私聊房间，则删除它
            // 由于数据库设置了外键级联删除(ON DELETE CASCADE)，
            // 删除chat_rooms中的记录会自动删除chat_room_members和chat_messages中所有相关的记录。
            if (roomIdToDelete != -1) {
                System.out.println("[DB] 正在为用户 " + userId + " 和 " + contactId + " 删除私聊房间: " + roomIdToDelete);
                String deleteRoomSql = "DELETE FROM chat_rooms WHERE id = ?";
                try (PreparedStatement deleteRoomPstmt = conn.prepareStatement(deleteRoomSql)) {
                    deleteRoomPstmt.setInt(1, roomIdToDelete);
                    deleteRoomPstmt.executeUpdate();
                }
            }

            // 步骤 3: 双向删除contacts表中的好友关系
            String deleteContactsSql = "DELETE FROM contacts WHERE (user_id = ? AND contact_user_id = ?) OR (user_id = ? AND contact_user_id = ?)";
            try (PreparedStatement deleteContactsPstmt = conn.prepareStatement(deleteContactsSql)) {
                deleteContactsPstmt.setInt(1, userId);
                deleteContactsPstmt.setInt(2, contactId);
                deleteContactsPstmt.setInt(3, contactId);
                deleteContactsPstmt.setInt(4, userId);
                deleteContactsPstmt.executeUpdate();
            }

            conn.commit(); // 提交事务
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                System.err.println("[DB Error] 删除联系人事务失败，正在回滚...");
                conn.rollback(); // 如果任何一步出错，回滚所有操作
            }
            throw e; // 向上抛出异常
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    /**
     * [V2.0] 更新联系人的备注名
     * @param userId 当前用户ID
     * @param contactId 联系人ID
     * @param remark 新的备注名 (如果为空字符串，则表示清除备注)
     * @return 如果操作成功返回true
     */
    public static boolean updateRemark(int userId, int contactId, String remark) throws SQLException {
        String sql = "UPDATE contacts SET remark_name = ? WHERE user_id = ? AND contact_user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, remark);
            pstmt.setInt(2, userId);
            pstmt.setInt(3, contactId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * [V2.2 修复版] 查找或创建两个用户之间的私聊房间。
     * 在操作前会先检查两人是否为好友关系。
     * @param userId1 用户1的ID
     * @param userId2 用户2的ID
     * @return 找到的或新创建的私聊房间的ID。如果非好友，返回-2；如果发生其他错误，返回-1。
     */
    public static int findOrCreatePrivateRoom(int userId1, int userId2) throws SQLException {
        // 步骤 1: 强制检查好友关系
        if (!areFriends(userId1, userId2)) {
            System.out.println("[DB Check] 用户 " + userId1 + " 和 " + userId2 + " 不是好友，拒绝创建私聊。");
            return -2; // 返回一个特殊代码-2，表示“非好友”
        }

        // 步骤 2: 查找是否已存在包含这两个用户的私聊房间 (逻辑不变)
        String findSql = "SELECT room_id FROM chat_room_members " +
                "WHERE user_id IN (?, ?) " +
                "GROUP BY room_id " +
                "HAVING COUNT(DISTINCT user_id) = 2 " +
                "AND room_id IN (SELECT id FROM chat_rooms WHERE is_private = TRUE)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement findPstmt = conn.prepareStatement(findSql)) {
            findPstmt.setInt(1, userId1);
            findPstmt.setInt(2, userId2);
            try (ResultSet rs = findPstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("room_id");
                }
            }
        }

        // 步骤 3: 如果没找到，则创建一个新的私聊房间 (逻辑不变)
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            String createRoomSql = "INSERT INTO chat_rooms (is_private) VALUES (TRUE)";
            int roomId;
            try (PreparedStatement createRoomPstmt = conn.prepareStatement(createRoomSql, Statement.RETURN_GENERATED_KEYS)) {
                createRoomPstmt.executeUpdate();
                try (ResultSet generatedKeys = createRoomPstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        roomId = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("创建房间失败，无法获取ID。");
                    }
                }
            }

            String addMembersSql = "INSERT INTO chat_room_members (room_id, user_id) VALUES (?, ?), (?, ?)";
            try (PreparedStatement addMembersPstmt = conn.prepareStatement(addMembersSql)) {
                addMembersPstmt.setInt(1, roomId);
                addMembersPstmt.setInt(2, userId1);
                addMembersPstmt.setInt(3, roomId);
                addMembersPstmt.setInt(4, userId2);
                addMembersPstmt.executeUpdate();
            }

            conn.commit();
            return roomId;

        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    /**
     * [V2.2 新增] 检查两个用户是否为好友
     * @return 如果是好友关系返回true, 否则返回false
     */
    private static boolean areFriends(int userId1, int userId2) throws SQLException {
        String sql = "SELECT COUNT(*) FROM contacts WHERE user_id = ? AND contact_user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId1);
            pstmt.setInt(2, userId2);
            try (ResultSet rs = pstmt.executeQuery()) {
                // 如果查询结果大于0，说明是好友
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }


    /**
     * [V2.0] 获取指定房间的最近聊天记录
     * @param roomId 房间ID
     * @param limit 获取的消息数量
     * @return 消息列表，按时间从旧到新排列，方便前端渲染
     */
    public static List<ChatMessage> getMessages(int roomId, int limit) throws SQLException {
        List<ChatMessage> chat_messages = new ArrayList<>();
        // 使用子查询和JOIN来获取消息内容和发送者昵称，并包含 is_read 状态
        String sql = "SELECT m.id, m.sender_id, u.nickname, m.message_content, m.created_at, m.is_read " +
                "FROM (SELECT * FROM chat_messages WHERE room_id = ? ORDER BY created_at DESC LIMIT ?) m " +
                "JOIN users u ON m.sender_id = u.id " +
                "ORDER BY m.created_at ASC"; // 最终结果按时间升序，方便前端直接渲染

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roomId);
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    chat_messages.add(new ChatMessage(
                            rs.getLong("id"),
                            rs.getInt("sender_id"),
                            rs.getString("nickname"),
                            rs.getString("message_content"),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getBoolean("is_read")
                    ));
                }
            }
        }
        return chat_messages;
    }

    /**
     * [V2.0] 保存聊天消息到数据库
     * @param roomId 房间ID
     * @param senderId 发送者ID
     * @param content 消息内容
     */
    public static void saveChatMessage(int roomId, int senderId, String content) throws SQLException {
        String sql = "INSERT INTO chat_messages (room_id, sender_id, message_content) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roomId);
            pstmt.setInt(2, senderId);
            pstmt.setString(3, content);
            pstmt.executeUpdate();
        }
    }

    /**
     * [V2.1 最终修复版] 获取与指定用户相关的所有通知
     * @param userId 当前用户ID
     * @return 通知列表
     */
    public static List<FriendRequest> getNotifications(int userId) throws SQLException {
        List<FriendRequest> notifications = new ArrayList<>();

        // 关键修复：使用别名(AS)确保UNION的两个部分的列名和类型完全一致
        String sql =
                // 1. 查询我收到的待处理请求
                "(SELECT fr.id, fr.sender_id AS other_user_id, u.nickname AS other_user_nickname, 'received' AS type, fr.status " +
                        "FROM friend_requests fr JOIN users u ON fr.sender_id = u.id " +
                        "WHERE fr.receiver_id = ? AND fr.status = 'pending') " +
                        "UNION ALL " +
                        // 2. 查询我发出的、已被对方处理的请求
                        "(SELECT fr.id, fr.receiver_id AS other_user_id, u.nickname AS other_user_nickname, 'sent' AS type, fr.status " +
                        "FROM friend_requests fr JOIN users u ON fr.receiver_id = u.id " +
                        "WHERE fr.sender_id = ? AND fr.status IN ('accepted', 'rejected')) " +
                        "ORDER BY id DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(new FriendRequest(
                            rs.getInt("id"),
                            rs.getInt("other_user_id"), // 关键修复：从正确的别名列获取ID
                            rs.getString("other_user_nickname"),
                            rs.getString("type"),
                            rs.getString("status")
                    ));
                }
            }
        }
        return notifications;
    }

    /**
     * [V2.1 最终修复版] 更新好友请求状态（例如，发送方点击“知道了”）
     * @param requestId 请求ID
     * @param currentUserId 当前用户ID（用于权限验证）
     * @param newStatus 新的状态
     * @return 是否成功
     */
    public static boolean updateRequestStatus(int requestId, int currentUserId, String newStatus) throws SQLException {
        String sql;
        boolean isDelete = false;

        // 根据 newStatus 选择正确的SQL语句
        if ("rejected_seen".equals(newStatus)) {
            // 对于被拒绝的通知，我们直接删除它，而不是更新状态
            sql = "DELETE FROM friend_requests WHERE id = ? AND sender_id = ? AND status = 'rejected'";
            isDelete = true;
        } else if ("accepted_seen".equals(newStatus)) {
            // 对于已接受的通知，我们也直接删除它
            sql = "DELETE FROM friend_requests WHERE id = ? AND sender_id = ? AND status = 'accepted'";
            isDelete = true;
        } else {
            // 如果有其他状态更新，可以加在这里。目前只处理删除。
            return false;
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 为SQL语句设置参数
            pstmt.setInt(1, requestId);
            pstmt.setInt(2, currentUserId);

            return pstmt.executeUpdate() > 0;
        }
    }

    // 添加到 DatabaseUtil.java

    /**
     * [V2.2 新增] 根据请求ID，获取该请求的发送者ID
     * @param requestId 好友请求的ID
     * @return 发送者的用户ID，如果找不到则返回 -1
     */
    public static int getRequestSenderId(int requestId) throws SQLException {
        String sql = "SELECT sender_id FROM friend_requests WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, requestId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("sender_id");
                }
            }
        }
        return -1; // 表示未找到
    }

    /**
     * [V2.2] Retrieves all chat rooms for a specific user.
     * For private chats, it also fetches the partner's name and ID.
     * @param userId The ID of the current user.
     * @return A list of ChatRoom objects.
     */
    public static List<ChatRoom> getChatRoomsForUser(int userId) throws SQLException {
        List<ChatRoom> rooms = new ArrayList<>();
        // This complex SQL query finds all rooms a user is a member of
        // and, for private rooms, identifies the other member (the "partner").
        String sql = "SELECT r.id, r.is_private, " +
                "(SELECT u.nickname FROM users u JOIN chat_room_members m2 ON u.id = m2.user_id WHERE m2.room_id = r.id AND m2.user_id != ?) AS partner_nickname, " +
                "(SELECT m2.user_id FROM chat_room_members m2 WHERE m2.room_id = r.id AND m2.user_id != ?) AS partner_id " +
                "FROM chat_rooms r JOIN chat_room_members m ON r.id = m.room_id WHERE m.user_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            pstmt.setInt(3, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    rooms.add(new ChatRoom(
                            rs.getInt("id"),
                            rs.getBoolean("is_private"),
                            rs.getString("partner_nickname"),
                            rs.getInt("partner_id")
                    ));
                }
            }
        }
        return rooms;
    }

    // 添加到 DatabaseUtil.java

    /**
     * [V2.3 新增] 获取一个用户的所有联系人的ID列表
     * @param userId 用户ID
     * @return 联系人ID的列表
     */
    public static List<Integer> getContactIds(int userId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT contact_user_id FROM contacts WHERE user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("contact_user_id"));
                }
            }
        }
        return ids;
    }

    /**
     * 保存一条新消息到数据库，并返回包含数据库生成ID的完整消息对象。
     * @param roomId 房间ID
     * @param senderId 发送者ID
     * @param content 消息内容
     * @return 保存后的完整 ChatMessage 对象
     * @throws SQLException
     */
    public static ChatMessage saveAndGetChatMessage(int roomId, int senderId, String content) throws SQLException {
        String sql = "INSERT INTO chat_messages (room_id, sender_id, message_content) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, roomId);
            stmt.setInt(2, senderId);
            stmt.setString(3, content);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("创建消息失败，没有行被影响。");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long newId = generatedKeys.getLong(1);
                    return getMessageById(newId);
                } else {
                    throw new SQLException("创建消息失败，没有获取到ID。");
                }
            }
        }
    }

    /**
     * 根据消息ID获取完整的 ChatMessage 对象
     * (这是 saveAndGetChatMessage 方法中用到的一个辅助方法)
     * @param messageId 消息ID
     * @return 完整的 ChatMessage 对象，如果未找到则返回 null
     * @throws SQLException
     */
    private static ChatMessage getMessageById(long messageId) throws SQLException {
        String sql = "SELECT m.id, m.sender_id, u.nickname, m.message_content, m.created_at, m.is_read FROM chat_messages m " +
                "JOIN users u ON m.sender_id = u.id WHERE m.id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, messageId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new ChatMessage(
                            rs.getLong("id"),
                            rs.getInt("sender_id"),
                            rs.getString("nickname"),
                            rs.getString("message_content"),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getBoolean("is_read")
                    );
                }
            }
        }
        return null;
    }


    /**
     * 标记一个房间中所有指定接收者的消息为已读。
     * @param roomId 房间ID
     * @param readerId 读取者ID (即消息的接收者)
     * @throws SQLException
     */
    public static void markMessagesAsRead(int roomId, int readerId) throws SQLException {
        // 首先获取该房间中发送消息的对方用户ID
        int partnerId = getPartnerId(roomId, readerId);

        String sql = "UPDATE chat_messages SET is_read = TRUE WHERE room_id = ? AND sender_id = ? AND is_read = FALSE";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roomId);
            stmt.setInt(2, partnerId); // 更新对方发给我的消息

            stmt.executeUpdate();
        }
    }

    /**
     * 获取一个房间中所有未读消息的ID列表。
     * @param roomId 房间ID
     * @param readerId 读取者ID (即消息的接收者)
     * @return 未读消息ID的列表
     * @throws SQLException
     */
    public static List<Integer> getUnreadMessageIds(int roomId, int readerId) throws SQLException {
        List<Integer> unreadIds = new ArrayList<>();
        // 首先获取该房间中发送消息的对方用户ID
        int partnerId = getPartnerId(roomId, readerId);

        String sql = "SELECT id FROM chat_messages WHERE room_id = ? AND sender_id = ? AND is_read = FALSE";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roomId);
            stmt.setInt(2, partnerId); // 获取对方发给我的未读消息

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    unreadIds.add(rs.getInt("id"));
                }
            }
        }
        return unreadIds;
    }

    /**
     * 获取指定房间中另一位用户的ID。
     * (这是一个简化版的辅助方法，你需要根据你的业务逻辑来实现)
     * @param roomId 房间ID
     * @param currentUserId 当前用户ID
     * @return 另一位用户的ID
     * @throws SQLException
     */
    private static int getPartnerId(int roomId, int currentUserId) throws SQLException {
        String sql = "SELECT user_id FROM chat_room_members WHERE room_id = ? AND user_id != ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, roomId);
            pstmt.setInt(2, currentUserId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("user_id");
                }
            }
        }
        throw new SQLException("无法找到房间中的另一位用户。");
    }

    /**
     * 获取数据库连接
     * @return Connection
     * @throws SQLException
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // 在 DatabaseUtil.java 中添加或修改此方法
    public static List<Integer> getReadMessageIdsByAuthor(int roomId, int readerId, int authorId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id FROM chat_messages WHERE room_id = ? AND sender_id = ? AND is_read = TRUE";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomId);
            stmt.setInt(2, authorId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("id"));
                }
            }
        }
        return ids;
    }
}