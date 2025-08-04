package chat.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id; // 更改为Long类型，以匹配数据库的bigint
    private int senderId;
    private String senderNickname;
    private String content;
    private LocalDateTime timestamp;
    private boolean isRead; // 新增 isRead 字段

    // 无参构造函数 (用于JSON反序列化)
    public ChatMessage() {}

    // 完整的构造函数，包含所有字段
    public ChatMessage(Long id, int senderId, String senderNickname, String content, LocalDateTime timestamp, boolean isRead) {
        this.id = id;
        this.senderId = senderId;
        this.senderNickname = senderNickname;
        this.content = content;
        this.timestamp = timestamp;
        this.isRead = isRead;
    }

    // 一个简化的构造函数，用于创建新消息时
    public ChatMessage(int senderId, String senderNickname, String content, LocalDateTime timestamp) {
        this(null, senderId, senderNickname, content, timestamp, false); // 新消息默认未读
    }

    // Getters
    public Long getId() {
        return id;
    }

    public int getSenderId() {
        return senderId;
    }

    public String getSenderNickname() {
        return senderNickname;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public boolean getIsRead() { // getter方法名应为getIsRead
        return isRead;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public void setSenderNickname(String senderNickname) {
        this.senderNickname = senderNickname;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setIsRead(boolean read) { // setter方法名应为setIsRead
        isRead = read;
    }
}