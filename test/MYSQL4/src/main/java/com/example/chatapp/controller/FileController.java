package com.example.chatapp.controller;

import com.example.chatapp.model.ChatFile;
import com.example.chatapp.model.User;
import com.example.chatapp.repository.ChatFileRepository;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {

    // 添加一个日志记录器
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired private FileStorageService fileStorageService;
    @Autowired private ChatFileRepository chatFileRepository;
    @Autowired private UserRepository userRepository;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file, @RequestParam("sender") String senderUsername) {
        logger.info("Received file upload request from user: {}. File name: {}, Size: {} bytes",
                senderUsername, file.getOriginalFilename(), file.getSize());

        if (file.isEmpty()) {
            logger.warn("Upload request from {} received with an empty file.", senderUsername);
            return ResponseEntity.badRequest().body(Map.of("message", "上传的文件不能为空"));
        }

        if (file.getSize() > 100 * 1024 * 1024) { // 100MB
            logger.warn("User {} tried to upload a file larger than 100MB: {}", senderUsername, file.getSize());
            return ResponseEntity.badRequest().body(Map.of("message", "文件大小不能超过100MB"));
        }

        User uploader = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + senderUsername));

        ChatFile chatFile = new ChatFile();
        chatFile.setFileName(file.getOriginalFilename());
        chatFile.setFileType(file.getContentType());
        chatFile.setFileSize(file.getSize());
        chatFile.setUploader(uploader);
        chatFile.setUploadTimestamp(LocalDateTime.now());
        chatFile.setExpiryTimestamp(LocalDateTime.now().plusMinutes(20)); // 20分钟后过期

        ChatFile savedFile = chatFileRepository.save(chatFile);
        logger.info("Saved file metadata to DB with ID: {}", savedFile.getId());

        try {
            Path targetLocation = fileStorageService.getFileStorageLocation().resolve(savedFile.getId());
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Successfully stored file {} at {}", savedFile.getFileName(), targetLocation);

            return ResponseEntity.ok(Map.of(
                    "fileId", savedFile.getId(),
                    "fileName", savedFile.getFileName(),
                    "fileSize", savedFile.getFileSize()
            ));
        } catch (Exception ex) {
            logger.error("Could not store file for user {}. File ID: {}. Error: {}",
                    senderUsername, savedFile.getId(), ex.getMessage());
            chatFileRepository.delete(savedFile); // 如果存储失败，回滚数据库记录
            return ResponseEntity.internalServerError().body(Map.of("message", "文件存储失败，请联系管理员"));
        }
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) {
        logger.info("Received download request for file ID: {}", fileId);
        ChatFile chatFile = chatFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("文件不存在"));

        if (chatFile.getExpiryTimestamp().isBefore(LocalDateTime.now())) {
            logger.warn("Attempt to download expired file ID: {}", fileId);
            throw new RuntimeException("文件已过期");
        }

        try {
            Path filePath = fileStorageService.getFileStorageLocation().resolve(fileId).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                logger.info("Serving file for download: {}", chatFile.getFileName());
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(chatFile.getFileType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + chatFile.getFileName() + "\"")
                        .body(resource);
            } else {
                logger.error("File not found on disk for ID: {}", fileId);
                throw new RuntimeException("文件未找到");
            }
        } catch (MalformedURLException ex) {
            logger.error("Malformed URL for file ID: {}", fileId, ex);
            throw new RuntimeException("文件路径错误", ex);
        }
    }
}
