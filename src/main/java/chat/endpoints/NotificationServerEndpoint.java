package chat.endpoints;

import chat.config.GetHttpSessionConfigurator;
import chat.model.User;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/notifications", configurator = GetHttpSessionConfigurator.class)
public class NotificationServerEndpoint {

    private static final Map<Integer, Session> userSessions = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        // 核心修改: 在连接建立时直接设置空闲超时时间
        session.setMaxIdleTimeout(600000); // 设置为10分钟 (600000毫秒)，确保大于前端心跳间隔

        HttpSession httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        if (httpSession == null || httpSession.getAttribute("user") == null) {
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "User not authenticated"));
            } catch (IOException e) { /* ignore */ }
            return;
        }

        User currentUser = (User) httpSession.getAttribute("user");
        int userId = currentUser.getId();

        userSessions.put(userId, session);
        System.out.println("[Notifications] 用户 " + userId + " 已连接通知服务。当前在线: " + userSessions.size());
    }

    @OnClose
    public void onClose(Session session) {
        Integer userId = findUserIdBySession(session);
        if (userId != null) {
            userSessions.remove(userId);
            System.out.println("[Notifications] 用户 " + userId + " 已断开通知服务。当前在线: " + userSessions.size());
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("[Notifications] WebSocket 发生错误，Session ID: " + session.getId());
        throwable.printStackTrace();
        Integer userId = findUserIdBySession(session);
        if (userId != null) {
            userSessions.remove(userId);
        }
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        try {
            JsonObject jsonObject = JsonParser.parseString(message).getAsJsonObject();
            String type = jsonObject.get("type").getAsString();
            if ("ping".equals(type)) {
                System.out.println("[Notifications] 收到用户 " + findUserIdBySession(session) + " 的心跳包。");
                return;
            }
        } catch (Exception e) {
            System.err.println("[Notifications] 处理消息失败: " + e.getMessage());
        }
    }

    private Integer findUserIdBySession(Session session) {
        return userSessions.entrySet().stream()
                .filter(entry -> entry.getValue().equals(session))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public static void sendNotification(int userId, Object notificationObject) {
        Session session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String jsonMessage = gson.toJson(notificationObject);
                session.getBasicRemote().sendText(jsonMessage);
                System.out.println("[Notifications] 成功向用户 " + userId + " 发送通知: " + jsonMessage);
            } catch (IOException e) {
                System.err.println("[Notifications] 向用户 " + userId + " 发送通知失败: " + e.getMessage());
            }
        } else {
            System.out.println("[Notifications] 尝试向用户 " + userId + " 发送通知，但用户不在线。");
        }
    }
}