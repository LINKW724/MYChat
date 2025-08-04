package com.example.chatapp.controller;

import com.example.chatapp.model.Friendship;
import com.example.chatapp.model.User;
import com.example.chatapp.repository.FriendshipRepository;
import com.example.chatapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/friends")
public class FriendshipController {

    @Autowired private FriendshipRepository friendshipRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    @PostMapping("/request")
    public ResponseEntity<?> sendFriendRequest(@RequestBody Map<String, String> payload) {
        String requesterUsername = payload.get("requester");
        String addresseeUsername = payload.get("addressee");

        User requester = userRepository.findByUsername(requesterUsername).orElse(null);
        User addressee = userRepository.findByUsername(addresseeUsername).orElse(null);

        if (requester == null || addressee == null) {
            return ResponseEntity.badRequest().body("用户不存在");
        }
        if (requester.equals(addressee)) {
            return ResponseEntity.badRequest().body("不能添加自己为好友");
        }

        if (friendshipRepository.findFriendshipBetween(requester, addressee).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("好友请求已发送或已经是好友关系");
        }

        Friendship friendship = new Friendship();
        friendship.setRequester(requester);
        friendship.setAddressee(addressee);
        friendship.setStatus(Friendship.FriendshipStatus.PENDING);
        friendship.setCreatedAt(LocalDateTime.now());
        friendshipRepository.save(friendship);

        Map<String, String> notification = new HashMap<>();
        notification.put("type", "FRIEND_REQUEST");
        notification.put("from", requester.getNickname());
        notification.put("fromUsername", requester.getUsername());
        messagingTemplate.convertAndSendToUser(addresseeUsername, "/queue/notifications", notification);

        return ResponseEntity.ok("好友请求已发送");
    }

    @GetMapping("/requests")
    public ResponseEntity<?> getPendingRequests(@RequestParam String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("用户不存在");
        }

        List<Map<String, Object>> requests = friendshipRepository.findByAddresseeAndStatus(user, Friendship.FriendshipStatus.PENDING)
                .stream()
                .map(req -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("requestId", req.getId());
                    map.put("requesterUsername", req.getRequester().getUsername());
                    map.put("requesterNickname", req.getRequester().getNickname());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(requests);
    }

    @PostMapping("/requests/{requestId}")
    public ResponseEntity<?> handleFriendRequest(@PathVariable Long requestId, @RequestBody Map<String, String> payload) {
        String action = payload.get("action");
        Friendship request = friendshipRepository.findById(requestId).orElse(null);

        if (request == null || request.getStatus() != Friendship.FriendshipStatus.PENDING) {
            return ResponseEntity.badRequest().body("请求不存在或已处理");
        }

        if ("accept".equals(action)) {
            request.setStatus(Friendship.FriendshipStatus.ACCEPTED);
            friendshipRepository.save(request);

            Map<String, String> notification = new HashMap<>();
            notification.put("type", "FRIEND_ACCEPT");
            notification.put("from", request.getAddressee().getNickname());
            notification.put("fromUsername", request.getAddressee().getUsername());
            messagingTemplate.convertAndSendToUser(request.getRequester().getUsername(), "/queue/notifications", notification);

            // 通知双方更新好友列表
            broadcastFriendListUpdate(request.getRequester().getUsername());
            broadcastFriendListUpdate(request.getAddressee().getUsername());

        } else {
            friendshipRepository.delete(request);
        }

        return ResponseEntity.ok("处理成功");
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<?> deleteFriend(@RequestParam String username, @RequestParam String friendUsername) {
        User user = userRepository.findByUsername(username).orElse(null);
        User friend = userRepository.findByUsername(friendUsername).orElse(null);

        if (user == null || friend == null) {
            return ResponseEntity.badRequest().body("用户不存在");
        }

        friendshipRepository.findFriendshipBetween(user, friend).ifPresent(friendship -> {
            friendshipRepository.deleteById(friendship.getId());

            // 通知双方更新好友列表
            broadcastFriendListUpdate(username);
            broadcastFriendListUpdate(friendUsername);
        });

        return ResponseEntity.ok("好友已删除");
    }

    private void broadcastFriendListUpdate(String username) {
        Map<String, Object> updateMessage = new HashMap<>();
        updateMessage.put("type", "FRIEND_LIST_UPDATE");
        messagingTemplate.convertAndSendToUser(username, "/queue/notifications", updateMessage);
    }
}
