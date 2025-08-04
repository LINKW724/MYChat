package com.example.chatapp.controller;

import com.example.chatapp.model.*;
import com.example.chatapp.repository.*;
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
@RequestMapping("/api/rooms")
public class ChatRoomController {

    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JoinRequestRepository joinRequestRepository;
    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private ChatRoomMemberRepository memberRepository;
    @Autowired private ChatMessageRepository chatMessageRepository;

    @PostMapping
    @Transactional
    public ResponseEntity<?> createRoom(@RequestBody Map<String, String> payload) {
        String roomName = payload.get("name");
        String ownerUsername = payload.get("owner");

        if (chatRoomRepository.findByName(roomName).isPresent()) {
            return ResponseEntity.badRequest().body("聊天室名称已存在");
        }
        User owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new RuntimeException("创建者用户不存在"));

        ChatRoom room = new ChatRoom();
        room.setName(roomName);
        room.setOwner(owner);
        ChatRoom savedRoom = chatRoomRepository.save(room);

        ChatRoomMember ownerMembership = new ChatRoomMember(savedRoom, owner);
        memberRepository.save(ownerMembership);

        // 广播房间更新消息
        messagingTemplate.convertAndSend("/topic/rooms", "update");

        return ResponseEntity.ok(savedRoom);
    }

    @GetMapping
    public List<Map<String, Object>> getAllRooms() {
        return chatRoomRepository.findAll().stream()
                .map(room -> {
                    Map<String, Object> roomMap = new HashMap<>();
                    roomMap.put("id", room.getId());
                    roomMap.put("name", room.getName());
                    roomMap.put("ownerNickname", room.getOwner().getNickname());
                    roomMap.put("ownerUsername", room.getOwner().getUsername());
                    return roomMap;
                }).collect(Collectors.toList());
    }

    @GetMapping("/{roomId}/is-member")
    public ResponseEntity<Map<String, Boolean>> isMember(@PathVariable Long roomId, @RequestParam String username) {
        ChatRoom room = chatRoomRepository.findById(roomId).orElse(null);
        User user = userRepository.findByUsername(username).orElse(null);
        if (room == null || user == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("isMember", memberRepository.existsByRoomAndUser(room, user)));
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<?> requestToJoin(@PathVariable Long roomId, @RequestBody Map<String, String> payload) {
        String requesterUsername = payload.get("username");
        ChatRoom room = chatRoomRepository.findById(roomId).orElseThrow(() -> new RuntimeException("聊天室不存在"));
        User requester = userRepository.findByUsername(requesterUsername).orElseThrow(() -> new RuntimeException("用户不存在"));

        if (memberRepository.existsByRoomAndUser(room, requester)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("您已经是该聊天室成员");
        }
        if (joinRequestRepository.findByRoomAndRequester(room, requester).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("您已发送过加入请求");
        }

        JoinRequest request = new JoinRequest();
        request.setRoom(room);
        request.setRequester(requester);
        request.setStatus(JoinRequest.RequestStatus.PENDING);
        request.setRequestTime(LocalDateTime.now());
        joinRequestRepository.save(request);

        String ownerUsername = room.getOwner().getUsername();
        ChatMessage notification = new ChatMessage();
        notification.setType(ChatMessage.MessageType.NOTIFICATION);
        notification.setSender("system");
        notification.setContent(requester.getNickname() + " 申请加入您的聊天室: " + room.getName());
        messagingTemplate.convertAndSendToUser(ownerUsername, "/queue/notifications", notification);

        return ResponseEntity.ok("请求已发送");
    }

    @GetMapping("/requests")
    public List<Map<String, Object>> getPendingRequests(@RequestParam String ownerUsername) {
        User owner = userRepository.findByUsername(ownerUsername).orElse(null);
        if (owner == null) return List.of();
        return joinRequestRepository.findByRoomOwnerAndStatus(owner, JoinRequest.RequestStatus.PENDING)
                .stream().map(req -> {
                    Map<String, Object> requestMap = new HashMap<>();
                    requestMap.put("requestId", req.getId());
                    requestMap.put("roomName", req.getRoom().getName());
                    requestMap.put("requesterNickname", req.getRequester().getNickname());
                    return requestMap;
                }).collect(Collectors.toList());
    }

    @PostMapping("/requests/{requestId}")
    @Transactional
    public ResponseEntity<?> handleJoinRequest(@PathVariable Long requestId, @RequestBody Map<String, String> payload) {
        String action = payload.get("action");
        JoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("请求不存在"));

        if ("approve".equals(action)) {
            request.setStatus(JoinRequest.RequestStatus.APPROVED);
            if (!memberRepository.existsByRoomAndUser(request.getRoom(), request.getRequester())) {
                ChatRoomMember newMember = new ChatRoomMember(request.getRoom(), request.getRequester());
                memberRepository.save(newMember);
            }
        } else {
            request.setStatus(JoinRequest.RequestStatus.REJECTED);
        }

        ChatMessage notification = new ChatMessage();
        notification.setType(ChatMessage.MessageType.NOTIFICATION);
        notification.setSender("system");
        String content = String.format("您加入聊天室 '%s' 的请求已被%s",
                request.getRoom().getName(), "approve".equals(action) ? "批准" : "拒绝");
        notification.setContent(content);
        messagingTemplate.convertAndSendToUser(request.getRequester().getUsername(), "/queue/notifications", notification);

        return ResponseEntity.ok("处理完成");
    }

    @DeleteMapping("/{roomId}")
    @Transactional
    public ResponseEntity<?> deleteRoom(@PathVariable Long roomId, @RequestParam String username) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("聊天室不存在"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!room.getOwner().equals(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("只有房主才能删除聊天室");
        }

        chatMessageRepository.deleteAllByReceiver("room-" + roomId);
        joinRequestRepository.deleteAllByRoom(room);
        memberRepository.deleteAllByRoom(room);
        chatRoomRepository.delete(room);

        // 广播房间更新消息
        messagingTemplate.convertAndSend("/topic/rooms", "update");

        return ResponseEntity.ok("聊天室已成功删除。");
    }

    @DeleteMapping("/{roomId}/history")
    @Transactional
    public ResponseEntity<?> clearRoomHistory(@PathVariable Long roomId, @RequestParam String username) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("聊天室不存在"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!room.getOwner().equals(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("只有房主才能清空聊天记录");
        }

        chatMessageRepository.deleteAllByReceiver("room-" + roomId);
        return ResponseEntity.ok("聊天室记录已清空。");
    }
}
