package chat.servlets;

import chat.DatabaseUtil;
import chat.util.ImageUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

//@WebServlet("/api/register")
@MultipartConfig
public class RegisterServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");

        // 1. 获取所有表单数据
        String account = req.getParameter("account");
        String password = req.getParameter("password");
        String nickname = req.getParameter("nickname");
        String question = req.getParameter("security_question");
        String answer = req.getParameter("security_answer");
        Part filePart = req.getPart("avatar");

        // 2. 逐一进行后端数据校验，并返回清晰的纯文本错误信息
        if (sendPlainTextError(resp, account == null || !account.trim().matches("^[a-zA-Z0-9]{6,13}$"), 400, "账号格式不符合规范 (6-13位字母或数字)")) return;
        if (sendPlainTextError(resp, password == null || password.trim().isEmpty(), 400, "密码不能为空")) return;
        if (sendPlainTextError(resp, nickname == null || nickname.trim().isEmpty(), 400, "昵称不能为空")) return;
        if (sendPlainTextError(resp, filePart == null || filePart.getSize() == 0, 400, "必须上传头像")) return;

        // ======== 新增：强制用户选择一个安全问题 ========
        if (sendPlainTextError(resp, question == null || question.isEmpty(), 400, "必须选择一个安全问题")) return;
        // ===========================================

        if (sendPlainTextError(resp, answer == null || answer.trim().isEmpty(), 400, "安全问题答案不能为空")) return;


        // 3. 使用清理过的数据进行注册
        String trimmedAccount = account.trim();
        String trimmedPassword = password.trim();
        String trimmedNickname = nickname.trim();
        String trimmedAnswer = answer.trim();

        try {
            if (DatabaseUtil.accountExists(trimmedAccount)) {
                sendPlainTextError(resp, true, HttpServletResponse.SC_CONFLICT, "该账号已被注册");
                return;
            }

            byte[] avatarBytes;
            try (InputStream avatarInputStream = filePart.getInputStream()) {
                avatarBytes = ImageUtil.resizeAvatar(avatarInputStream);
            }

            boolean success = DatabaseUtil.registerUser(trimmedAccount, trimmedPassword, trimmedNickname, avatarBytes, question, trimmedAnswer);

            if (success) {
                resp.setStatus(HttpServletResponse.SC_CREATED);
            } else {
                sendPlainTextError(resp, true, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "注册失败，请稍后重试");
            }

        } catch (SQLException e) {
            throw new ServletException("注册时数据库操作失败", e);
        } catch (IOException e) {
            throw new ServletException("头像处理失败", e);
        }
    }

    /**
     * 辅助方法，用于发送纯文本的错误响应
     * @param resp HttpServletResponse对象
     * @param condition 如果为true，则发送错误
     * @param statusCode HTTP状态码 (e.g., 400, 409)
     * @param message 要发送的纯文本消息
     * @return 如果发送了错误则返回true，否则返回false
     * @throws IOException
     */
    private boolean sendPlainTextError(HttpServletResponse resp, boolean condition, int statusCode, String message) throws IOException {
        if (condition) {
            resp.setStatus(statusCode);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write(message);
            return true;
        }
        return false;
    }
}