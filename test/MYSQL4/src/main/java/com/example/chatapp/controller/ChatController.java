// File: src/main/java/com/example/chatapp/controller/ChatController.java
// ★★★ 修改说明 ★★★
// 我们修改了 sendMessage 方法。
// 对于文件消息，现在我们将文件的 `fileId` 保存到数据库的 `content` 字段中，
// 而不是之前的文件名。这是最关键的改动，确保了消息和文件的关联性。

package com.example.chatapp.controller;

import com.example.chatapp.model.ChatMessage;
import com.example.chatapp.repository.ChatMessageRepository;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.service.ActiveUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ChatController {

    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private ChatMessageRepository chatMessageRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ActiveUserService activeUserService;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        chatMessage.setTimestamp(LocalDateTime.now());

        userRepository.findByUsername(chatMessage.getSender()).ifPresent(user -> {
            chatMessage.setSenderNickname(user.getNickname());
        });

        // ★ [关键修改] 对于文件消息，将 fileId 存入 content 字段以供持久化
        if (chatMessage.getType() == ChatMessage.MessageType.FILE) {
            // 前端在成功上传文件后，会将 fileId, fileName, fileSize 都包含在消息体中。
            // 我们将 fileId 存入 content 字段，这样刷新后就能从数据库中找回它。
            chatMessage.setContent(chatMessage.getFileId());
        }

        // P2P信令消息不需要存储
        if (chatMessage.getType() != ChatMessage.MessageType.P2P_SIGNAL) {
            chatMessageRepository.save(chatMessage);
        }

        // 将消息广播到对应的频道
        // 注意：此时 chatMessage 对象在内存中是完整的（包含fileName, fileSize等），
        // 所以实时接收方看到的是正确的。此修改主要影响刷新后的历史记录。
        messagingTemplate.convertAndSend("/topic/chat/" + chatMessage.getReceiver(), chatMessage);
    }

    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        String username = chatMessage.getSender();
        String sessionId = headerAccessor.getSessionId();

        if (!activeUserService.addUser(username, sessionId)) {
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/errors",
                    Map.of("type", "DUPLICATE_LOGIN", "message", "账号已在别处登录，此连接将断开。"),
                    headerAccessor.getMessageHeaders()
            );
            System.out.println("Duplicate login attempt for user: " + username + ". New session " + sessionId + " rejected.");
            return;
        }

        headerAccessor.getSessionAttributes().put("username", username);

        Map<String, Object> statusUpdate = new HashMap<>();
        statusUpdate.put("username", username);
        statusUpdate.put("status", "ONLINE");
        messagingTemplate.convertAndSend("/topic/status", statusUpdate);

        messagingTemplate.convertAndSend("/topic/public", chatMessage);
    }

    @MessageMapping("/status.getOnlineUsers")
    public void getOnlineUsers(@Payload String fromUser) {
        Map<String, Object> onlineUsersPayload = new HashMap<>();
        onlineUsersPayload.put("type", "online-list");
        onlineUsersPayload.put("users", activeUserService.getActiveUsers());
        messagingTemplate.convertAndSendToUser(fromUser, "/queue/status", onlineUsersPayload);
    }
}
