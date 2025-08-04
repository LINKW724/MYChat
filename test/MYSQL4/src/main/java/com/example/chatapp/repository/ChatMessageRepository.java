package com.example.chatapp.repository;

import com.example.chatapp.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // This method now works for both private and group chats using the channelId
    List<ChatMessage> findByReceiverOrderByTimestampAsc(String receiver);

    @Transactional
    void deleteAllByReceiver(String receiver);

    // The old complex methods for private chat are no longer needed.
    // void deleteAllBySenderAndReceiverOrReceiverAndSender(...) is removed.
    // findBySenderAndReceiverOrReceiverAndSenderOrderByTimestampAsc(...) is removed.
}