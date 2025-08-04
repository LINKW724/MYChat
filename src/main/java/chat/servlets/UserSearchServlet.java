package chat.servlets;

import chat.DatabaseUtil;
import chat.model.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;


public class UserSearchServlet extends HttpServlet {
    private final Gson gson = new GsonBuilder().create();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not logged in");
            return;
        }

        User currentUser = (User) session.getAttribute("user");
        String query = req.getParameter("query");

        if (query == null || query.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Search query cannot be empty");
            return;
        }

        try {
            List<User> users = DatabaseUtil.searchUsers(currentUser.getId(), query.trim());

            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(gson.toJson(users));

        } catch (SQLException e) {
            throw new ServletException("Database error during user search", e);
        }
    }
}