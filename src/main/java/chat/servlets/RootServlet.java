package chat.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 这个Servlet专门处理对Web应用根目录的访问。
 * 它会立即将用户重定向到登录页面。
 */
//@WebServlet("") // <-- 空字符串的URL模式代表应用的根路径
public class RootServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 执行服务器端重定向到 login.html
        resp.sendRedirect("login.html");
    }
}