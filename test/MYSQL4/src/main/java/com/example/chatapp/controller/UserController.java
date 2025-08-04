package com.example.chatapp.controller;

import com.example.chatapp.model.User;
import com.example.chatapp.repository.ChatRoomMemberRepository;
import com.example.chatapp.repository.FriendshipRepository;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.service.ActiveUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired private UserRepository userRepository;
    @Autowired private ChatRoomMemberRepository memberRepository;
    @Autowired private FriendshipRepository friendshipRepository;
    @Autowired private ActiveUserService activeUserService;

    @GetMapping("/search")
    public List<Map<String, String>> searchUsers(@RequestParam String query, @RequestParam String currentUser) {
        return userRepository.findAll().stream()
                .filter(user -> !user.getUsername().equals(currentUser) &&
                        (user.getUsername().toLowerCase().contains(query.toLowerCase()) ||
                                user.getNickname().toLowerCase().contains(query.toLowerCase())))
                .map(user -> {
                    Map<String, String> userMap = new HashMap<>();
                    userMap.put("username", user.getUsername());
                    userMap.put("nickname", user.getNickname());
                    return userMap;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/{username}/friends")
    public ResponseEntity<?> getFriends(@PathVariable String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("用户不存在");
        }

        Set<String> onlineUsers = activeUserService.getActiveUsers();

        List<Map<String, Object>> friends = friendshipRepository.findFriends(user).stream()
                .map(friendship -> {
                    User friendUser = friendship.getRequester().equals(user) ? friendship.getAddressee() : friendship.getRequester();
                    Map<String, Object> friendMap = new HashMap<>();
                    friendMap.put("username", friendUser.getUsername());
                    friendMap.put("nickname", friendUser.getNickname());
                    friendMap.put("online", onlineUsers.contains(friendUser.getUsername()));
                    return friendMap;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(friends);
    }

    @GetMapping("/{username}/rooms")
    public List<Map<String, Object>> getMemberRooms(@PathVariable String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return List.of();
        }
        return memberRepository.findByUser(user).stream()
                .map(member -> {
                    Map<String, Object> roomMap = new HashMap<>();
                    roomMap.put("id", "room-" + member.getRoom().getId());
                    roomMap.put("name", member.getRoom().getName());
                    return roomMap;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/{username}/avatar")
    public ResponseEntity<byte[]> getAvatar(@PathVariable String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getAvatar() != null && user.getAvatar().length > 0) {
                return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(user.getAvatar());
            }
        }
        // 如果用户不存在，或用户头像为空，则返回 404 Not Found
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/update/avatar")
    public ResponseEntity<?> updateAvatar(@RequestParam("username") String username, @RequestParam("avatar") MultipartFile avatarFile) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        try {
            user.setAvatar(avatarFile.getBytes());
            userRepository.save(user);
            return ResponseEntity.ok("头像更新成功");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("头像更新失败");
        }
    }

    @PostMapping("/update/password")
    public ResponseEntity<?> updatePassword(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String oldPassword = payload.get("oldPassword");
        String newPassword = payload.get("newPassword");

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        if (!user.getPassword().equals(oldPassword)) {
            return ResponseEntity.badRequest().body("旧密码不正确");
        }

        user.setPassword(newPassword); // In production, hash the password
        userRepository.save(user);

        return ResponseEntity.ok("密码修改成功");
    }
}
