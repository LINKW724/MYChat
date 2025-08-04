package com.example.chatapp.repository;

import com.example.chatapp.model.ChatFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatFileRepository extends JpaRepository<ChatFile, String> {
    // 查找所有已过期的文件
    List<ChatFile> findByExpiryTimestampBefore(LocalDateTime now);
}
