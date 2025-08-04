package chat.model;

import java.io.Serializable;

/**
 * 好友请求/通知的数据模型。
 * 实现了Serializable接口，这是一个良好的编程习惯。
 */
public class FriendRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int otherUserId; // 代表对方用户的ID（可能是发送者或接收者）
    private String otherUserNickname;
    private String type;     // 'received' (我收到的) 或 'sent' (我发出的)
    private String status;   // 'pending', 'accepted', 'rejected', 'rejected_seen'

    public FriendRequest(int id, int otherUserId, String otherUserNickname, String type, String status) {
        this.id = id;
        this.otherUserId = otherUserId;
        this.otherUserNickname = otherUserNickname;
        this.type = type;
        this.status = status;
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getOtherUserId() {
        return otherUserId;
    }

    public String getOtherUserNickname() {
        return otherUserNickname;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setOtherUserId(int otherUserId) {
        this.otherUserId = otherUserId;
    }

    public void setOtherUserNickname(String otherUserNickname) {
        this.otherUserNickname = otherUserNickname;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}