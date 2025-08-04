// File: src/main/java/com/example/chatapp/controller/HistoryController.java
// ★★★ 修改说明 ★★★
// 这个文件的改动最大。
// 1. 我们注入了 `ChatFileRepository` 以便查询文件信息。
// 2. 我们创建了一个新的辅助方法 `populateSenderNicknamesAndFileData`。
// 3. 当从数据库获取历史记录时，这个新方法会检查消息是否为文件类型。
//    如果是，它会用 `content` 字段里的 `fileId` 去 `chat_files` 表里查出完整的文件信息（文件名、大小），
//    然后把这些信息填充回消息对象中，再发给前端。
// 这样，刷新页面后，前端就能收到包含完整文件信息的历史记录了。

package com.example.chatapp.controller;

import com.example.chatapp.model.ChatFile;
import com.example.chatapp.model.ChatMessage;
import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.model.User;
import com.example.chatapp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    @Autowired private ChatMessageRepository chatMessageRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ChatRoomMemberRepository memberRepository;
    @Autowired private ChatFileRepository chatFileRepository; // ★ [新增] 注入文件仓库

    /**
     * ★ [核心修改] 这是一个新的辅助方法，用于丰富从数据库查出的消息列表。
     * 它会一次性查询并填充所有发送者的昵称和所有文件消息的详细信息。
     * @param messages 从数据库获取的原始消息列表
     */
    private void populateSenderNicknamesAndFileData(List<ChatMessage> messages) {
        if (messages.isEmpty()) {
            return;
        }

        // 1. 收集所有发送者的用户名
        Set<String> usernames = messages.stream()
                .map(ChatMessage::getSender)
                .collect(Collectors.toSet());

        // 2. 一次性查询所有用户，并创建 username -> nickname 的映射
        Map<String, String> nicknameMap = userRepository.findAll().stream()
                .filter(user -> usernames.contains(user.getUsername()))
                .collect(Collectors.toMap(User::getUsername, User::getNickname));

        // 3. 从文件类型的消息中收集所有 fileId (现在存储在 content 字段)
        List<String> fileIds = messages.stream()
                .filter(msg -> msg.getType() == ChatMessage.MessageType.FILE && msg.getContent() != null && !msg.getContent().isEmpty())
                .map(ChatMessage::getContent)
                .collect(Collectors.toList());

        // 4. 一次性查询所有文件信息，并创建 fileId -> ChatFile 的映射
        Map<String, ChatFile> fileMap = new HashMap<>();
        if (!fileIds.isEmpty()) {
            fileMap = chatFileRepository.findAllById(fileIds).stream()
                    .collect(Collectors.toMap(ChatFile::getId, file -> file));
        }

        // 5. 遍历消息列表，填充昵称和文件信息
        for (ChatMessage msg : messages) {
            // 填充发送者昵称
            msg.setSenderNickname(nicknameMap.getOrDefault(msg.getSender(), msg.getSender()));

            // 如果是文件消息，则填充文件详情
            if (msg.getType() == ChatMessage.MessageType.FILE) {
                String fileId = msg.getContent(); // content 字段现在是 fileId
                ChatFile fileInfo = fileMap.get(fileId);
                if (fileInfo != null) {
                    // 找到了文件信息，填充到消息对象的@Transient字段中
                    msg.setFileId(fileInfo.getId());
                    msg.setFileName(fileInfo.getFileName());
                    msg.setFileSize(fileInfo.getFileSize());
                } else {
                    // 如果文件信息找不到（可能已过期被删除），提供一个友好的提示
                    msg.setFileId(fileId);
                    msg.setFileName("[文件已过期或不存在]");
                    msg.setFileSize(0L);
                }
            }
        }
    }

    @GetMapping("/{channelId}")
    public ResponseEntity<?> getChatHistory(@PathVariable String channelId, @RequestParam String username) {
        // 安全检查逻辑保持不变
        if (channelId.startsWith("private-")) {
            String[] users = channelId.substring(8).split("-");
            if (users.length != 2 || (!users[0].equals(username) && !users[1].equals(username))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权访问此聊天记录");
            }
        } else if (channelId.startsWith("room-")) {
            try {
                long roomId = Long.parseLong(channelId.substring(5));
                ChatRoom room = chatRoomRepository.findById(roomId).orElse(null);
                User user = userRepository.findByUsername(username).orElse(null);
                if (room == null || user == null || !memberRepository.existsByRoomAndUser(room, user)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("您不是该聊天室成员");
                }
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body("无效的群组ID格式");
            }
        } else {
            return ResponseEntity.badRequest().body("无效的频道ID");
        }

        List<ChatMessage> messages = chatMessageRepository.findByReceiverOrderByTimestampAsc(channelId);

        // ★ [关键修改] 调用新的方法来填充所有需要的信息
        populateSenderNicknamesAndFileData(messages);

        return ResponseEntity.ok(messages);
    }

    @DeleteMapping("/private/{channelId}")
    @Transactional
    public ResponseEntity<?> clearPrivateChatHistory(@PathVariable String channelId, @RequestParam String username) {
        if (!channelId.startsWith("private-")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("此接口仅用于清除私聊记录");
        }

        String[] users = channelId.substring(8).split("-");
        if (Arrays.stream(users).noneMatch(u -> u.equals(username))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权清除此聊天记录");
        }

        chatMessageRepository.deleteAllByReceiver(channelId);
        return ResponseEntity.ok("私聊记录已清空。");
    }
}
