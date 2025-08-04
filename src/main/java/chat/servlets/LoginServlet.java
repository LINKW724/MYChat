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
import java.util.HashMap;
import java.util.Map;

public class LoginServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String account = req.getParameter("account");
        String password = req.getParameter("password");
        String ipAddress = req.getRemoteAddr(); // 获取客户端IP地址

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        if (account == null || password == null || account.trim().isEmpty() || password.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(gson.toJson(createErrorResponse("账号和密码均不能为空")));
            return;
        }

        try {
            User userToLogin = DatabaseUtil.login(account.trim(), password.trim());

            if (userToLogin != null) {
                // 关键修复点：检查该账号是否已在当前IP地址下登录
                if (ActiveUserListener.isUserActiveFromIp(userToLogin.getId(), ipAddress)) {
                    System.err.println("[LoginServlet] 登录被拒绝: 用户 " + userToLogin.getNickname() + " 已在同一IP下登录。IP: " + ipAddress);
                    resp.setStatus(HttpServletResponse.SC_CONFLICT);
                    resp.getWriter().write(gson.toJson(createErrorResponse("您已在当前设备登录，请勿重复登录")));
                    return;
                }

                // 获取或创建一个会话
                HttpSession newSession = req.getSession(true);
                newSession.setAttribute("user", userToLogin);
                newSession.setAttribute("ipAddress", ipAddress); // 将IP地址绑定到会话中
                newSession.setMaxInactiveInterval(30 * 60);

                ActiveUserListener.userLoggedIn(userToLogin, newSession);

                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(gson.toJson(createSuccessResponse("登录成功")));

            } else {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write(gson.toJson(createErrorResponse("账号或密码错误")));
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(gson.toJson(createErrorResponse("数据库登录验证失败")));
            throw new ServletException("数据库登录验证失败", e);
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
        return response;
    }

    private Map<String, String> createSuccessResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", message);
        return response;
    }
}