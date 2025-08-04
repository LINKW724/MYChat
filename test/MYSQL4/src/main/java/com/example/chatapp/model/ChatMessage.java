package com.example.chatapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sender; // 发送者用户名
    private String receiver; // 接收者用户名或群聊ID

    @Column(columnDefinition = "TEXT")
    private String content; // 对于文件消息，这里可以存储文件名或文件ID

    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    private MessageType type;

    @Transient // 此字段不存入数据库
    private String senderNickname;

    // 新增文件相关字段，不存入数据库
    @Transient
    private String fileId;
    @Transient
    private String fileName;
    @Transient
    private Long fileSize;


    public enum MessageType {
        CHAT,
        JOIN,
        LEAVE,
        FILE, // 新增文件类型
        NOTIFICATION,
        P2P_REQUEST, // P2P文件传输请求
        P2P_ACCEPT,  // P2P接受
        P2P_REJECT,  // P2P拒绝
        P2P_SIGNAL   // P2P信令 (for WebRTC)
    }
}
