package chat.servlets;

import chat.DatabaseUtil;
import chat.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;


public class ContactActionServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        User currentUser = (User) session.getAttribute("user");
        String action = req.getParameter("action");

        try {
            int contactId = Integer.parseInt(req.getParameter("contactId"));

            if ("delete".equals(action)) {
                DatabaseUtil.deleteContact(currentUser.getId(), contactId);
                resp.setStatus(HttpServletResponse.SC_OK);

            } else if ("remark".equals(action)) {
                String remark = req.getParameter("remark");
                if (remark == null) {
                    remark = ""; // Allow clearing the remark
                }
                DatabaseUtil.updateRemark(currentUser.getId(), contactId, remark);
                resp.setStatus(HttpServletResponse.SC_OK);

            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid action");
            }
        } catch (SQLException | NumberFormatException e) {
            throw new ServletException("Database error processing contact action", e);
        }
    }
}