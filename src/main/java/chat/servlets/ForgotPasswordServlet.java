package chat.servlets;

import chat.DatabaseUtil;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

//@WebServlet("/api/forgot-password")
public class ForgotPasswordServlet extends HttpServlet {
    private final Gson gson = new Gson();

    /**
     * 处理获取安全问题的GET请求
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String account = req.getParameter("account");
        if (account == null || account.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "账号不能为空");
            return;
        }

        try {
            String question = DatabaseUtil.getSecurityQuestion(account);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");

            Map<String, String> responseMap = new HashMap<>();
            if (question != null) {
                responseMap.put("question", question);
                resp.getWriter().write(gson.toJson(responseMap));
            } else {
                // 为安全起见，即使账号不存在也返回空问题，不直接暴露“用户不存在”
                responseMap.put("question", "");
                resp.getWriter().write(gson.toJson(responseMap));
            }
        } catch (SQLException e) {
            throw new ServletException("数据库查询安全问题失败", e);
        }
    }

    /**
     * 处理提交答案和新密码以重置密码的POST请求
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String account = req.getParameter("account");
        String answer = req.getParameter("answer");
        String newPassword = req.getParameter("newPassword");

        if (account == null || answer == null || newPassword == null ||
                account.trim().isEmpty() || answer.trim().isEmpty() || newPassword.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "所有字段均为必填项");
            return;
        }

        try {
            boolean success = DatabaseUtil.resetPasswordByAnswer(account, answer, newPassword);
            if (success) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("{\"message\": \"密码重置成功！\"}");
            } else {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "安全问题答案错误");
            }
        } catch (SQLException e) {
            throw new ServletException("重置密码时数据库操作失败", e);
        }
    }
}