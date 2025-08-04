package chat.model;

import java.io.Serializable;

/**
 * 聊天室的数据模型。
 * 用于封装从数据库查询出的聊天室列表信息。
 */
public class ChatRoom implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private boolean isPrivate;
    private String partnerNickname; // 对于私聊，这是对方的昵称
    private int partnerId;          // 对于私聊，这是对方的用户ID

    public ChatRoom(int id, boolean isPrivate, String partnerNickname, int partnerId) {
        this.id = id;
        this.isPrivate = isPrivate;
        this.partnerNickname = partnerNickname;
        this.partnerId = partnerId;
    }

    // Getters
    public int getId() {
        return id;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public String getPartnerNickname() {
        return partnerNickname;
    }

    public int getPartnerId() {
        return partnerId;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public void setPartnerNickname(String partnerNickname) {
        this.partnerNickname = partnerNickname;
    }

    public void setPartnerId(int partnerId) {
        this.partnerId = partnerId;
    }
}