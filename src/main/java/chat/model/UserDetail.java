package chat.model;

// 这个类继承自User，并额外增加了securityQuestion字段
public class UserDetail extends User {
    private String securityQuestion;

    public UserDetail(int id, String account, String nickname, String securityQuestion) {
        super(id, account, nickname);
        this.securityQuestion = securityQuestion;
    }

    public String getSecurityQuestion() {
        return securityQuestion;
    }

    public void setSecurityQuestion(String securityQuestion) {
        this.securityQuestion = securityQuestion;
    }
}