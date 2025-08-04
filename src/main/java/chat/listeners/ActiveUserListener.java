package chat.listeners;

import chat.DatabaseUtil;
import chat.endpoints.NotificationServerEndpoint;
import chat.model.User;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.Collections;

/**
 * [V2.3 最终版 - 多端登录修复] 用于追踪当前所有活跃用户会话的监听器。
 * 实现了IP感知的多端登录和实时在线状态广播的核心逻辑。
 */
@WebListener
public class ActiveUserListener implements HttpSessionListener {

    // 键: 用户ID, 值: 包含该用户所有活跃会话的列表
    private static final ConcurrentMap<Integer, CopyOnWriteArrayList<HttpSession>> activeUserSessions = new ConcurrentHashMap<>();

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        // 会话创建时不做任何操作
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        HttpSession destroyedSession = se.getSession();
        User user = (User) destroyedSession.getAttribute("user");

        if (user != null) {
            CopyOnWriteArrayList<HttpSession> sessions = activeUserSessions.get(user.getId());
            if (sessions != null) {
                sessions.removeIf(session -> session.getId().equals(destroyedSession.getId()));
                System.out.println("[ActiveUserListener] 用户 " + user.getNickname() + " (ID: " + user.getId() + ") 的会话已销毁。");

                // 如果该用户的所有会话都已销毁，则广播下线通知
                if (sessions.isEmpty()) {
                    System.out.println("[ActiveUserListener] 用户 " + user.getNickname() + " (ID: " + user.getId() + ") 已完全下线。");
                    activeUserSessions.remove(user.getId());
                    broadcastStatusChange(user.getId(), "offline");
                }
            }
        }
    }

    /**
     * 将新登录的用户添加到在线列表，并广播上线通知。
     * @param user 新登录的用户对象
     * @param newSession 新创建的会话
     */
    public static void userLoggedIn(User user, HttpSession newSession) {
        boolean wasOffline = !activeUserSessions.containsKey(user.getId()) || activeUserSessions.get(user.getId()).isEmpty();

        activeUserSessions.computeIfAbsent(user.getId(), k -> new CopyOnWriteArrayList<>()).add(newSession);

        System.out.println("[ActiveUserListener] 用户 " + user.getNickname() + " (ID: " + user.getId() + ") 已上线。当前在线会话数: " + activeUserSessions.get(user.getId()).size());

        if (wasOffline) {
            broadcastStatusChange(user.getId(), "online");
        }
    }

    /**
     * 将已退出登录的用户从在线列表中移除。
     * @param userId 已退出登录的用户ID
     * @param sessionId 要移除的会话ID
     */
    public static void userLoggedOut(Integer userId, String sessionId) {
        if (userId != null) {
            CopyOnWriteArrayList<HttpSession> sessions = activeUserSessions.get(userId);
            if (sessions != null) {
                sessions.removeIf(session -> session.getId().equals(sessionId));
                System.out.println("[ActiveUserListener] 用户ID " + userId + " 的会话 " + sessionId + " 已从列表中手动移除。");

                if (sessions.isEmpty()) {
                    activeUserSessions.remove(userId);
                    broadcastStatusChange(userId, "offline");
                }
            }
        }
    }

    /**
     * [核心] 强制下线一个已登录的用户。
     * @param userId 要强制下线的用户ID
     * @param sessionId 要强制下线的会话ID
     */
    public static void forceLogoutUser(Integer userId, String sessionId) {
        CopyOnWriteArrayList<HttpSession> sessions = activeUserSessions.get(userId);
        if (sessions != null) {
            sessions.stream()
                    .filter(s -> s.getId().equals(sessionId))
                    .findFirst()
                    .ifPresent(s -> {
                        System.out.println("[ActiveUserListener] 正在为用户ID " + userId + " 的会话 " + sessionId + " 执行强制下线...");
                        try {
                            s.invalidate();
                        } catch (IllegalStateException e) {
                            System.err.println("[ActiveUserListener] 尝试销毁一个已失效的会话，安全忽略。正在手动移除...");
                            userLoggedOut(userId, sessionId);
                        }
                    });
        }
    }

    /**
     * 检查用户是否已在线。
     * @return 如果用户有任何一个活跃会话，则返回 true
     */
    public static boolean isUserActive(Integer userId) {
        return userId != null && activeUserSessions.containsKey(userId) && !activeUserSessions.get(userId).isEmpty();
    }

    /**
     * [新增] 检查用户是否已在同一IP地址下登录。
     * @param userId 要检查的用户ID
     * @param ipAddress 登录的IP地址
     * @return 如果存在相同IP的活跃会话，则返回 true
     */
    public static boolean isUserActiveFromIp(Integer userId, String ipAddress) {
        if (userId == null || ipAddress == null) {
            return false;
        }
        CopyOnWriteArrayList<HttpSession> sessions = activeUserSessions.get(userId);
        if (sessions == null) {
            return false;
        }
        return sessions.stream().anyMatch(session -> ipAddress.equals(session.getAttribute("ipAddress")));
    }

    public static List<Integer> getOnlineContactIds(List<Integer> contactIds) {
        if (contactIds == null) {
            return Collections.emptyList();
        }
        return contactIds.stream()
                .filter(ActiveUserListener::isUserActive)
                .collect(Collectors.toList());
    }

    private static void broadcastStatusChange(int userId, String status) {
        try {
            List<Integer> friendIds = DatabaseUtil.getContactIds(userId);
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "status_change");
            notification.put("userId", userId);
            notification.put("status", status);
            for (Integer friendId : friendIds) {
                if (isUserActive(friendId)) {
                    NotificationServerEndpoint.sendNotification(friendId, notification);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ActiveUserListener] 广播用户 " + userId + " 状态变更时数据库出错。");
            e.printStackTrace();
        }
    }
}