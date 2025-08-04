package chat.servlets;

import chat.DatabaseUtil;
import chat.model.User;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class PrivateChatServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) { /* ... */ return; }
        User currentUser = (User) session.getAttribute("user");

        try {
            int contactId = Integer.parseInt(req.getParameter("contactId"));
            int roomId = DatabaseUtil.findOrCreatePrivateRoom(currentUser.getId(), contactId);

            if (roomId >= 0) { // 成功（ID大于等于0）
                Map<String, Integer> result = new HashMap<>();
                result.put("roomId", roomId);
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().write(gson.toJson(result));
            } else if (roomId == -2) {
                // 特殊情况：非好友
                // 返回 403 Forbidden 状态码，表示“禁止操作”
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "你们已不是好友关系");
            } else {
                // 其他错误
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "无法创建或查找私聊房间");
            }

        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "无效的联系人ID");
        } catch (SQLException e) {
            throw new ServletException("处理私聊请求时数据库操作失败", e);
        }
    }
}