package chat.servlets;

import chat.DatabaseUtil;
import chat.model.ChatRoom;
import chat.model.User;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

// 注意：这个Servlet依赖于web.xml进行注册
public class ChatRoomListServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        User currentUser = (User) session.getAttribute("user");
        try {
            List<ChatRoom> rooms = DatabaseUtil.getChatRoomsForUser(currentUser.getId());

            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(gson.toJson(rooms));
        } catch (SQLException e) {
            throw new ServletException("获取聊天室列表失败", e);
        }
    }
}