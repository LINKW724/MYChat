package chat.servlets;

import chat.listeners.ActiveUserListener;
import chat.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * 处理用户退出登录请求的Servlet。
 *
 * 如果你选择 web.xml 进行配置，请不要使用 @WebServlet 注解。
 * 如果你选择注解进行配置，请删除 web.xml 中的相应声明。
 */
// @WebServlet("/api/logout") // 如果使用web.xml配置，请确保此行被注释掉
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 获取当前的会话（如果存在）
        HttpSession session = req.getSession(false);

        if (session != null) {
            // 在使会话失效之前，从ActiveUserListener中移除用户
            User currentUser = (User) session.getAttribute("user");
            if (currentUser != null) {
                // 修复点：调用 userLoggedOut 时，同时传递用户ID和会话ID
                ActiveUserListener.userLoggedOut(currentUser.getId(), session.getId());
                System.out.println("[LogoutServlet] User " + currentUser.getNickname() + " logged out successfully.");
            }

            // 使会话失效。
            try {
                session.invalidate();
                System.out.println("[LogoutServlet] Session invalidated successfully.");
            } catch (IllegalStateException e) {
                // 如果会话已经因为超时等原因失效，则忽略此异常
                System.err.println("[LogoutServlet] 尝试销毁已失效的会话，安全忽略。");
            }
        } else {
            System.out.println("[LogoutServlet] No active session found to invalidate.");
        }

        // 无论会话是否存在，都返回一个成功的响应，通知前端跳转
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write("{\"status\": \"success\", \"message\": \"您已退出登录。\"}");
    }
}