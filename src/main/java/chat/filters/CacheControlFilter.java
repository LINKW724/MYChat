package chat.filters;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 一个用于禁用浏览器缓存的过滤器。
 * 这个过滤器会拦截所有对 .html 页面的请求，
 * 并添加响应头来告诉浏览器不要缓存这些页面。
 */
@WebFilter("*.html") // <-- 这个注解让过滤器应用于所有以 .html 结尾的请求
public class CacheControlFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // 将通用的ServletResponse转换为专门处理HTTP的HttpServletResponse
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 设置响应头来禁用缓存
        // HTTP 1.1
        httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        // HTTP 1.0 (兼容旧版)
        httpResponse.setHeader("Pragma", "no-cache");
        // Proxies
        httpResponse.setDateHeader("Expires", 0);

        // !! 关键步骤 !!
        // 将请求传递给过滤器链中的下一个元素（可能是另一个过滤器，或者最终的Servlet/资源）
        chain.doFilter(request, response);
    }

    // Filter生命周期中的其他方法，我们可以保留为空
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 过滤器初始化时调用
    }

    @Override
    public void destroy() {
        // 过滤器销毁时调用
    }
}