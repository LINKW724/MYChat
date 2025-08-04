package com.example.chatapp.controller;

import com.example.chatapp.model.User;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.service.ActiveUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private UserRepository userRepository;
    @Autowired private ActiveUserService activeUserService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestParam("username") String username,
                                          @RequestParam("password") String password,
                                          @RequestParam("nickname") String nickname,
                                          @RequestParam("securityQuestion") String securityQuestion,
                                          @RequestParam("securityAnswer") String securityAnswer,
                                          @RequestParam(value = "avatar", required = false) MultipartFile avatarFile) {
        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("用户名已存在!");
        }
        User user = new User();
        user.setUsername(username);
        user.setNickname(nickname);
        user.setPassword(password); // 生产环境应加密
        user.setSecurityQuestion(securityQuestion);
        user.setSecurityAnswer(securityAnswer);

        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                user.setAvatar(avatarFile.getBytes());
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("头像上传失败: " + e.getMessage());
            }
        }
        userRepository.save(user);
        return ResponseEntity.ok("注册成功!");
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        // FEATURE: Single Session Login - HTTP Level Check
        if (activeUserService.isUserActive(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("账号已在别处登录！");
        }

        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getPassword().equals(password)) {
                // [修复] 删除了此处不正确的 activeUserService.addUser(user.getUsername()); 调用
                // 用户的在线状态应该在 WebSocket 连接成功后（在ChatController中）才被添加。

                Map<String, String> response = new HashMap<>();
                response.put("message", "登录成功");
                response.put("username", user.getUsername());
                response.put("nickname", user.getNickname());
                return ResponseEntity.ok(response);
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("账号或密码错误");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        if (username != null) {
            // 注意: 这里调用 removeUser, 而不是 removeUserBySessionId，因为我们没有sessionId
            activeUserService.removeUser(username);
        }
        return ResponseEntity.ok("退出成功");
    }

    @PostMapping("/check-username")
    public ResponseEntity<?> checkUsername(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        boolean exists = userRepository.findByUsername(username).isPresent();
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @GetMapping("/security-question")
    public ResponseEntity<?> getSecurityQuestion(@RequestParam String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            return ResponseEntity.ok(Map.of("question", userOptional.get().getSecurityQuestion()));
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String answer = payload.get("answer");
        String newPassword = payload.get("newPassword");

        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getSecurityAnswer().equalsIgnoreCase(answer)) {
                user.setPassword(newPassword); // 生产环境应加密
                userRepository.save(user);
                return ResponseEntity.ok("密码重置成功!");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("安全问题答案错误!");
            }
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("用户不存在!");
    }
}