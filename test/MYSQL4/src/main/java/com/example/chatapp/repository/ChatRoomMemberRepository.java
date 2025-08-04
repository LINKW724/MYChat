package com.example.chatapp.repository;

import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.model.ChatRoomMember;
import com.example.chatapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    boolean existsByRoomAndUser(ChatRoom room, User user);
    List<ChatRoomMember> findByUser(User user);

    @Transactional
    void deleteAllByRoom(ChatRoom room);
}
