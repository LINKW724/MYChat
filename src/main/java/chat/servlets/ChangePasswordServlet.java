package chat.servlets;

import chat.DatabaseUtil;
import chat.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

//@WebServlet("/api/change-password")
public class ChangePasswordServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "用户未登录");
            return;
        }

        User currentUser = (User) session.getAttribute("user");
        String verificationType = req.getParameter("verificationType");
        String newPassword = req.getParameter("newPassword");

        if (newPassword == null || newPassword.trim().isEmpty() || verificationType == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "参数不完整");
            return;
        }

        boolean success = false;
        try {
            if ("password".equals(verificationType)) {
                String oldPassword = req.getParameter("oldPassword");
                if (oldPassword == null) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "旧密码不能为空");
                    return;
                }
                success = DatabaseUtil.changePasswordByOldPassword(currentUser.getId(), oldPassword.trim(), newPassword.trim());
                if (!success) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "旧密码不正确");
                    return;
                }
            } else if ("question".equals(verificationType)) {
                String answer = req.getParameter("answer");
                if (answer == null) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "答案不能为空");
                    return;
                }
                success = DatabaseUtil.changePasswordByAnswer(currentUser.getId(), answer.trim(), newPassword.trim());
                if (!success) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "安全问题答案不正确");
                    return;
                }
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "无效的验证类型");
                return;
            }

            if (success) {
                session.invalidate(); // 密码修改成功，强制用户重新登录
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("{\"message\": \"密码修改成功，请重新登录。\"}");
            }

        } catch (SQLException e) {
            throw new ServletException("修改密码时数据库操作失败", e);
        }
    }
}