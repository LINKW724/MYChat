package com.example.chatapp.repository;

import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.model.JoinRequest;
import com.example.chatapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface JoinRequestRepository extends JpaRepository<JoinRequest, Long> {
    List<JoinRequest> findByRoomOwnerAndStatus(User owner, JoinRequest.RequestStatus status);
    Optional<JoinRequest> findByRoomAndRequester(ChatRoom room, User requester);

    @Transactional
    void deleteAllByRoom(ChatRoom room);
}