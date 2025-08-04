package chat.servlets;

import chat.DatabaseUtil;
import chat.listeners.ActiveUserListener;
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
public class OnlineContactsServlet extends HttpServlet {
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
            // 步骤 1: 调用DatabaseUtil获取所有好友ID
            List<Integer> allContactIds = DatabaseUtil.getContactIds(currentUser.getId());

            // 步骤 2: 调用ActiveUserListener筛选出在线的好友ID
            List<Integer> onlineContactIds = ActiveUserListener.getOnlineContactIds(allContactIds);

            // 步骤 3: 将在线好友ID列表作为JSON返回给前端
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(gson.toJson(onlineContactIds));

        } catch (SQLException e) {
            throw new ServletException("获取在线联系人列表时数据库操作失败", e);
        }
    }
}