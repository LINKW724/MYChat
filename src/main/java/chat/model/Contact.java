package chat.model;

public class Contact extends User {
    private String remarkName;
    public Contact(int id, String account, String nickname, String remarkName) {
        super(id, account, nickname);
        this.remarkName = remarkName;
    }
    public String getRemarkName() { return remarkName; }
    public String getDisplayName() { return (remarkName != null && !remarkName.isEmpty()) ? remarkName : getNickname(); }
}