package chat.servlets;

import chat.DatabaseUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;

//@WebServlet("/api/avatar")
public class AvatarServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userIdStr = req.getParameter("userId");
        if (userIdStr == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少userId参数");
            return;
        }

        try {
            int userId = Integer.parseInt(userIdStr);
            byte[] avatarBytes = DatabaseUtil.getAvatar(userId);

            if (avatarBytes != null && avatarBytes.length > 0) {
                resp.setContentType("image/png"); // 之前已统一处理为PNG
                resp.setContentLength(avatarBytes.length);
                try (OutputStream os = resp.getOutputStream()) {
                    os.write(avatarBytes);
                }
            } else {
                // 如果用户没有头像，可以重定向到一个默认头像
                // resp.sendRedirect("assets/icons/default-avatar.png");
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "userId格式错误");
        } catch (SQLException e) {
            throw new ServletException("获取头像失败", e);
        }
    }
}