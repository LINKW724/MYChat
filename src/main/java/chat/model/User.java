package chat.model;

import java.io.Serializable;

// 实现 Serializable 接口，以便可以安全地存储在 HttpSession 中
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private String account;
    private String nickname;

    public User(int id, String account, String nickname) {
        this.id = id;
        this.account = account;
        this.nickname = nickname;
    }
    // Getters and Setters...
    public int getId() { return id; }
    public String getAccount() { return account; }
    public String getNickname() { return nickname; }
    public void setId(int id) { this.id = id; }
    public void setAccount(String account) { this.account = account; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}