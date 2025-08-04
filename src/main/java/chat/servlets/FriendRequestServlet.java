package chat.servlets;

import chat.DatabaseUtil;
import chat.endpoints.NotificationServerEndpoint; // 新增：导入通知服务类
import chat.model.FriendRequest;
import chat.model.User;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap; // 新增：导入HashMap
import java.util.List;
import java.util.Map;    // 新增：导入Map

public class FriendRequestServlet extends HttpServlet {
    private final Gson gson = new Gson();

    /**
     * doGet方法用于获取当前用户的所有通知（收到的待处理请求、已发送的被处理请求）。
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not logged in");
            return;
        }

        User currentUser = (User) session.getAttribute("user");
        try {
            List<FriendRequest> notifications = DatabaseUtil.getNotifications(currentUser.getId());

            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(gson.toJson(notifications));

        } catch (SQLException e) {
            throw new ServletException("获取通知列表时数据库操作失败", e);
        }
    }

    /**
     * doPost方法根据'action'参数处理三种不同操作：
     * 1. send: 发送好友请求, 并通过WebSocket通知接收方
     * 2. respond: 响应好友请求, 并通过WebSocket通知发送方
     * 3. update_status: 更新通知状态（如"知道了"）
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not logged in");
            return;
        }

        User currentUser = (User) session.getAttribute("user");
        String action = req.getParameter("action");

        try {
            if ("send".equals(action)) {
                // --- 处理发送请求 ---
                int receiverId = Integer.parseInt(req.getParameter("receiverId"));
                boolean success = DatabaseUtil.sendFriendRequest(currentUser.getId(), receiverId);
                if (success) {
                    // [实时通知] 通知接收方有新的好友请求
                    Map<String, String> notification = new HashMap<>();
                    notification.put("type", "new_friend_request");
                    NotificationServerEndpoint.sendNotification(receiverId, notification);

                    resp.setStatus(HttpServletResponse.SC_OK);
                } else {
                    resp.sendError(HttpServletResponse.SC_CONFLICT, "请求已发送或无法添加该用户");
                }

            } else if ("respond".equals(action)) {
                // --- 处理响应请求 ---
                int requestId = Integer.parseInt(req.getParameter("requestId"));
                String status = req.getParameter("status"); // "accepted" or "rejected"

                if (!"accepted".equals(status) && !"rejected".equals(status)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "无效的响应状态");
                    return;
                }

                // 在数据库操作前，先获取发送者的ID，以便后续通知
                int senderId = DatabaseUtil.getRequestSenderId(requestId);
                if (senderId == -1) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "原始好友请求未找到");
                    return;
                }

                boolean success = DatabaseUtil.respondToFriendRequest(requestId, currentUser.getId(), status);

                if(success) {
                    // [实时通知] 通知请求发送方，他们的请求已被处理
                    Map<String, String> notification = new HashMap<>();
                    notification.put("type", "request_responded");
                    NotificationServerEndpoint.sendNotification(senderId, notification);

                    resp.setStatus(HttpServletResponse.SC_OK);
                } else {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "处理请求失败");
                }

            } else if ("update_status".equals(action)) {
                // --- 处理更新通知状态 ---
                int requestId = Integer.parseInt(req.getParameter("requestId"));
                String newStatus = req.getParameter("newStatus"); // "rejected_seen" or "accepted_seen"
                DatabaseUtil.updateRequestStatus(requestId, currentUser.getId(), newStatus);
                resp.setStatus(HttpServletResponse.SC_OK);

            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "无效的action参数");
            }
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "无效的ID参数格式");
        } catch (SQLException e) {
            e.printStackTrace(); // 在服务器日志中打印详细错误
            throw new ServletException("处理好友请求时数据库操作失败", e);
        }
    }
}