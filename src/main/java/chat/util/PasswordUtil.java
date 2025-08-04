package chat.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * 密码处理工具类
 * 使用 BCrypt 算法对密码进行哈希处理和验证。
 * 这是一个行业标准，可以有效防止彩虹表攻击。
 */
public final class PasswordUtil {

    /**
     * 私有构造函数，防止该工具类被实例化。
     */
    private PasswordUtil() {}

    /**
     * 将明文字符串哈希化。
     * BCrypt.gensalt() 会生成一个随机的“盐”，并将其包含在最终的哈希字符串中。
     *
     * @param plainText 需要哈希的明文
     * @return 哈希后的字符串
     */
    public static String hash(String plainText) {
        return BCrypt.hashpw(plainText, BCrypt.gensalt());
    }

    /**
     * 验证明文字符串与一个已知的哈希值是否匹配。
     *
     * @param plainText 用户输入的明文
     * @param hashed    从数据库中取出的哈希字符串
     * @return 如果匹配返回true，否则返回false
     */
    public static boolean check(String plainText, String hashed) {
        if (plainText == null || hashed == null || hashed.isEmpty()) {
            return false;
        }
        try {
            // BCrypt.checkpw 会从'hashed'字符串中自动提取出盐值进行比较
            return BCrypt.checkpw(plainText, hashed);
        } catch (IllegalArgumentException e) {
            // 如果 'hashed' 字符串格式不正确，BCrypt会抛出异常
            System.err.println("哈希值格式不正确: " + e.getMessage());
            return false;
        }
    }
}