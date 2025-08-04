package com.example.chatapp.repository;

import com.example.chatapp.model.Friendship;
import com.example.chatapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    // 查找两个用户之间的任何关系，无论方向如何
    @Query("SELECT f FROM Friendship f WHERE (f.requester = :user1 AND f.addressee = :user2) OR (f.requester = :user2 AND f.addressee = :user1)")
    Optional<Friendship> findFriendshipBetween(@Param("user1") User user1, @Param("user2") User user2);

    // 查找发送给某个用户且状态为PENDING的请求
    List<Friendship> findByAddresseeAndStatus(User addressee, Friendship.FriendshipStatus status);

    // 查找一个用户的所有已接受的好友关系
    @Query("SELECT f FROM Friendship f WHERE (f.requester = :user OR f.addressee = :user) AND f.status = 'ACCEPTED'")
    List<Friendship> findFriends(@Param("user") User user);
}
