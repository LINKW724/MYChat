package com.example.chatapp.service;

import com.example.chatapp.model.ChatFile;
import com.example.chatapp.repository.ChatFileRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
@EnableScheduling // 启用定时任务
public class FileStorageService {

    private final Path fileStorageLocation;
    @Autowired
    private ChatFileRepository chatFileRepository;

    public FileStorageService() {
        // **修复:** 将存储位置更改为用户主文件夹中一个可靠的目录。
        // 这可以避免大多数写入项目目录时的权限问题。
        String userHome = System.getProperty("user.home");
        this.fileStorageLocation = Paths.get(userHome, "chat-app-uploads").toAbsolutePath().normalize();

        System.out.println("文件存储位置已设置为: " + this.fileStorageLocation.toString());

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            // 如果目录创建失败，提供更详细的错误信息。
            throw new RuntimeException("无法创建目录: " + this.fileStorageLocation.toString() + ". 请检查权限。", ex);
        }
    }

    public Path getFileStorageLocation() {
        return this.fileStorageLocation;
    }

    /**
     * 定时任务，每5分钟执行一次，清理过期文件。
     * cron表达式: "0 *5 * * * *" 表示每5分钟的第0秒执行
            */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void cleanupExpiredFiles() {
        System.out.println("Running expired files cleanup job at " + LocalDateTime.now());
        List<ChatFile> expiredFiles = chatFileRepository.findByExpiryTimestampBefore(LocalDateTime.now());

        if (expiredFiles.isEmpty()) {
            System.out.println("No expired files to clean up.");
            return;
        }

        for (ChatFile fileRecord : expiredFiles) {
            try {
                Path filePath = this.fileStorageLocation.resolve(fileRecord.getId()).normalize();
                Files.deleteIfExists(filePath);
                chatFileRepository.delete(fileRecord);
                System.out.println("Deleted expired file: " + fileRecord.getFileName() + " (ID: " + fileRecord.getId() + ")");
            } catch (IOException e) {
                // **修复:** 重新输入此行以删除任何可能导致编译错误的不可见字符。
                System.err.println("无法删除文件 " + fileRecord.getFileName() + ": " + e.getMessage());
            }
        }
    }
}
