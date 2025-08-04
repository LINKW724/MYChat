package chat.endpoints;

import chat.DatabaseUtil;
import chat.config.GetHttpSessionConfigurator;
import chat.model.ChatMessage;
import chat.model.User;
import chat.util.GsonLocalDateTimeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint(value = "/chat/{roomId}", configurator = GetHttpSessionConfigurator.class)
public class ChatServerEndpoint {

    // 【修改】将Map的Key从String改为Integer，以匹配roomId的类型，更安全高效
    private static final Map<Integer, Set<Session>> rooms = new ConcurrentHashMap<>();
    private static final Set<Session> allSessions = new CopyOnWriteArraySet<>();
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new GsonLocalDateTimeAdapter())
            .create();

    @OnOpen
    public void onOpen(Session session, @PathParam("roomId") String roomIdStr, EndpointConfig config) {
        HttpSession httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        if (httpSession == null || httpSession.getAttribute("user") == null) {
            closeSession(session, "User not authenticated");
            return;
        }

        int roomId = Integer.parseInt(roomIdStr);
        User currentUser = (User) httpSession.getAttribute("user");
        session.getUserProperties().put("user", currentUser);
        session.getUserProperties().put("roomId", roomId); // 【修改】存储为Integer类型

        rooms.computeIfAbsent(roomId, k -> new CopyOnWriteArraySet<>()).add(session);
        allSessions.add(session);
        System.out.println("[WebSocket] 用户 " + currentUser.getNickname() + " (ID: " + currentUser.getId() + ") 的会话 " + session.getId() + " 加入房间 " + roomId);

        try {
            List<ChatMessage> history = DatabaseUtil.getMessages(roomId, 50);
            sendText(session, gson.toJson(Map.of("type", "history", "data", history)));

            User partner = getPartnerInRoom(session, roomId);
            if (partner != null) {
                DatabaseUtil.markMessagesAsRead(roomId, currentUser.getId());
                broadcastReadStatusUpdate(roomId, currentUser.getId(), partner.getId());
                // 通知对方我已上线
                sendMessageToUser(partner.getId(), gson.toJson(Map.of("type", "partner_status_change", "status", "online")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // 【修复】优先处理心跳，避免不必要的解析
        if ("{\"type\":\"ping\"}".equals(message)) {
            return;
        }

        User currentUser = (User) session.getUserProperties().get("user");
        Integer roomId = (Integer) session.getUserProperties().get("roomId");
        if (currentUser == null || roomId == null) {
            return;
        }

        // 尝试解析为JSON，以处理read_notification等控制消息
        try {
            JsonObject jsonMessage = gson.fromJson(message, JsonObject.class);
            if (jsonMessage.has("type") && "read_notification".equals(jsonMessage.get("type").getAsString())) {
                int recipientId = jsonMessage.get("recipientId").getAsInt();
                DatabaseUtil.markMessagesAsRead(roomId, currentUser.getId());
                broadcastReadStatusUpdate(roomId, currentUser.getId(), recipientId);
                return; // 处理完后直接返回
            }
        } catch (JsonSyntaxException e) {
            // 如果解析失败，说明是普通聊天文本消息，会继续向下执行
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // 处理普通聊天文本
        try {
            ChatMessage savedMessage = DatabaseUtil.saveAndGetChatMessage(roomId, currentUser.getId(), message);
            User partner = getPartnerInRoom(session, roomId);
            broadcastMessageToParticipants(currentUser, partner, savedMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session) {
        Integer roomId = (Integer) session.getUserProperties().get("roomId");
        User currentUser = (User) session.getUserProperties().get("user");

        allSessions.remove(session); // 总是从全局集合中移除
        if (currentUser == null || roomId == null) {
            System.out.println("[WebSocket] 一个未认证或未进入房间的会话关闭: " + session.getId());
            return;
        }

        Set<Session> roomSessions = rooms.get(roomId);
        if (roomSessions != null) {
            roomSessions.remove(session);
            System.out.println("[WebSocket] 用户 " + currentUser.getNickname() + " 的会话 " + session.getId() + " 离开房间 " + roomId);

            boolean isUserStillInRoom = roomSessions.stream()
                    .anyMatch(s -> {
                        User user = (User) s.getUserProperties().get("user");
                        return user != null && user.getId() == currentUser.getId();
                    });

            if (!isUserStillInRoom) {
                System.out.println("[WebSocket] 用户 " + currentUser.getNickname() + " 已完全离开房间 " + roomId);
                User partner = getPartnerInRoomAfterLeaving(roomId, currentUser.getId());
                if (partner != null) {
                    sendMessageToUser(partner.getId(), gson.toJson(Map.of("type", "partner_status_change", "status", "offline")));
                }
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        if (!(throwable instanceof IOException)) { // 忽略常见的IO异常，如连接重置
            System.err.println("WebSocket Error for session " + session.getId() + ": " + throwable.getMessage());
        }
        onClose(session);
    }

    // 【最终版广播逻辑】
    private void broadcastMessageToParticipants(User sender, User receiver, ChatMessage chatMessage) {
        String jsonMessage = gson.toJson(Map.of("type", "new_message", "data", chatMessage));
        int senderId = sender.getId();
        int receiverId = (receiver != null) ? receiver.getId() : -1;

        sendMessageToUser(senderId, jsonMessage);
        if (receiverId != -1) {
            sendMessageToUser(receiverId, jsonMessage);
        }
    }

    private void sendMessageToUser(int userId, String jsonMessage) {
        for (Session s : allSessions) {
            if (s.isOpen()) {
                User sessionUser = (User) s.getUserProperties().get("user");
                if (sessionUser != null && sessionUser.getId() == userId) {
                    sendText(s, jsonMessage);
                }
            }
        }
    }

    private User getPartnerInRoom(Session mySession, int roomId) {
        User currentUser = (User) mySession.getUserProperties().get("user");
        if (currentUser == null) return null;
        return rooms.getOrDefault(roomId, Set.of()).stream()
                .map(s -> (User) s.getUserProperties().get("user"))
                .filter(u -> u != null && u.getId() != currentUser.getId())
                .findFirst().orElse(null);
    }

    private User getPartnerInRoomAfterLeaving(int roomId, int leavingUserId) {
        return rooms.getOrDefault(roomId, Set.of()).stream()
                .map(s -> (User) s.getUserProperties().get("user"))
                .filter(u -> u != null && u.getId() != leavingUserId)
                .findFirst().orElse(null);
    }

    private void broadcastReadStatusUpdate(int roomId, int readerId, int authorId) throws SQLException {
        List<Integer> readMessageIds = DatabaseUtil.getReadMessageIdsByAuthor(roomId, readerId, authorId);
        if (readMessageIds.isEmpty()) return;
        String message = gson.toJson(Map.of("type", "read_status_update", "messageIds", readMessageIds));
        sendMessageToUser(authorId, message);
    }

    private void sendText(Session session, String text) {
        try {
            session.getBasicRemote().sendText(text);
        } catch (IOException e) {
            // 捕获并记录IO异常，但不再向上抛出，使调用代码更简洁
            System.err.println("Failed to send message to session " + session.getId() + ": " + e.getMessage());
        }
    }

    private void closeSession(Session session, String reason) {
        try {
            if (session.isOpen()) {
                session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, reason));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}