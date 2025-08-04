package chat.servlets;

import chat.DatabaseUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * 专门用于异步检查账号是否已存在的Servlet
 */
//@WebServlet("/api/check-account")
public class AccountCheckServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String account = req.getParameter("account");

        if (account == null || account.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            if (DatabaseUtil.accountExists(account.trim())) {
                // 如果账号已存在，返回 409 Conflict 状态码
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                resp.setContentType("text/plain; charset=UTF-8");
                resp.getWriter().write("该账号已被注册");
            } else {
                // 如果账号可用，返回 200 OK 状态码
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.setContentType("text/plain; charset=UTF-8");
                resp.getWriter().write("该账号可用");
            }
        } catch (SQLException e) {
            throw new ServletException("检查账号时数据库操作失败", e);
        }
    }
}