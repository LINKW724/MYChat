package com.example.chatapp.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ActiveUserService {
    // 将用户名映射到会话ID，以强制执行单会话
    private final Map<String, String> usernameToSessionId = new ConcurrentHashMap<>();
    // 将会话ID映射到用户名，以便在断开连接时快速查找
    private final Map<String, String> sessionIdToUsername = new ConcurrentHashMap<>();

    /**
     * 如果用户尚不存在，则以原子方式将其添加到活动列表中。
     * 这可以防止两个会话试图同时登录的竞态条件。
     * @param username 用户的用户名。
     * @param sessionId 用户的WebSocket会话ID。
     * @return 如果用户成功添加，则返回true；如果该用户的会话已存在，则返回false。
     */
    public boolean addUser(String username, String sessionId) {
        // putIfAbsent是一个原子操作。它只会在键（用户名）不存在的情况下添加映射。
        String existingSessionId = usernameToSessionId.putIfAbsent(username, sessionId);
        if (existingSessionId == null) {
            // 用户已成功添加，因此也添加反向映射。
            sessionIdToUsername.put(sessionId, username);
            return true;
        }
        // 该用户名已存在活动会话。
        return false;
    }

    /**
     * 通过会话ID删除用户。这是处理断开连接的首选方法。
     * 它确保只有正确的会话注销才会删除用户。
     * @param sessionId 要删除的用户的会话ID。
     * @return 已删除用户的用户名，如果未找到会话，则返回null。
     */
    public String removeUserBySessionId(String sessionId) {
        String username = sessionIdToUsername.remove(sessionId);
        if (username != null) {
            // 同时删除主映射。我们使用remove(key, value)来确保
            // 我们只在它对应于刚刚断开连接的会话时才删除条目。
            // 这可以防止在新会话刚登录时旧会话正在断开连接的竞态条件。
            usernameToSessionId.remove(username, sessionId);
        }
        return username;
    }

    /**
     * 按用户名删除用户。用于客户端的显式注销调用。
     * @param username 要删除的用户名。
     */
    public void removeUser(String username) {
        String sessionId = usernameToSessionId.remove(username);
        if (sessionId != null) {
            sessionIdToUsername.remove(sessionId);
        }
    }


    /**
     * 检查用户是否有活动会话。
     * @param username 要检查的用户名。
     * @return 如果用户处于活动状态，则返回true，否则返回false。
     */
    public boolean isUserActive(String username) {
        return usernameToSessionId.containsKey(username);
    }

    /**
     * 获取所有活动用户名的集合。
     * @return 一个不可修改的活动用户名的集合。
     */
    public Set<String> getActiveUsers() {
        return Collections.unmodifiableSet(usernameToSessionId.keySet());
    }
}
