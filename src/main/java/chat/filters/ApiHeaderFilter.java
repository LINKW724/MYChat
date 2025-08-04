package chat.filters;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 为所有 /api/* 路径下的请求统一添加安全和缓存控制的HTTP响应头。
 */
@WebFilter("/api/*") // <-- 这个注解让过滤器应用于所有以 /api/ 开头的请求
public class ApiHeaderFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 1. 添加 X-Content-Type-Options 安全头
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");

        // 2. 为API响应添加禁止缓存的头
        httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        httpResponse.setHeader("Pragma", "no-cache"); // 兼容旧版 HTTP/1.0
        httpResponse.setDateHeader("Expires", 0);

        // 将请求传递给过滤器链中的下一个元素（即您真正的Servlet）
        chain.doFilter(request, response);
    }

    // 其他生命周期方法
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void destroy() {}
}