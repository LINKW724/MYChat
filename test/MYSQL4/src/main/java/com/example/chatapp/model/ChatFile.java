package com.example.chatapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_files")
@Data
public class ChatFile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id; // 使用UUID作为ID，更安全

    private String fileName;
    private String fileType;
    private long fileSize;

    @ManyToOne
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    private LocalDateTime uploadTimestamp;
    private LocalDateTime expiryTimestamp; // 文件过期时间
}
