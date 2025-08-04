package chat.servlets;

import chat.DatabaseUtil;
import chat.model.User;
import chat.model.UserDetail;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

public class UserDetailsServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("\n[UserDetailsServlet 探针 1/4] doGet 方法被调用。");

        // 尝试获取会话，但不创建新的
        HttpSession session = req.getSession(false);

        // 检查会话是否存在
        if (session == null) {
            System.err.println("[UserDetailsServlet 探针 2/4] 严重错误: session 为 null！服务器没有找到任何会话。");
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "会话不存在");
            return;
        }

        System.out.println("[UserDetailsServlet 探针 2/4] Session 存在，ID为: " + session.getId());

        // 检查会话中是否有 "user" 属性
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            System.err.println("[UserDetailsServlet 探针 3/4] 严重错误: session中没有'user'属性！");
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "用户未在会话中认证");
            return;
        }

        System.out.println("[UserDetailsServlet 探针 3/4] Session中找到了用户: " + sessionUser.getNickname());

        try {
            UserDetail userDetails = DatabaseUtil.getUserDetailsById(sessionUser.getId());
            if (userDetails != null) {
                System.out.println("[UserDetailsServlet 探针 4/4] 成功从数据库获取用户详情，准备返回JSON。");
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().write(gson.toJson(userDetails));
            } else {
                session.invalidate();
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "无法在数据库中找到用户信息");
            }
        } catch (SQLException e) {
            throw new ServletException("获取用户详情时数据库操作失败", e);
        }
    }
}