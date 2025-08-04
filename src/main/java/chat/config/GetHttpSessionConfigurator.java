package chat.config;

import jakarta.servlet.http.HttpSession;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * 一个自定义的ServerEndpoint配置器。
 * 它的唯一作用是在WebSocket握手阶段，从HTTP请求中提取出HttpSession，
 * 并将其传递给WebSocket的Session，以便我们在@OnOpen等方法中可以访问。
 */
public class GetHttpSessionConfigurator extends ServerEndpointConfig.Configurator {

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        // 通过HandshakeRequest获取到原生的HttpSession
        HttpSession httpSession = (HttpSession) request.getHttpSession();

        if (httpSession != null) {
            // 将HttpSession对象放入ServerEndpointConfig的用户属性中
            // 键名使用HttpSession的类名，这是一种标准做法，可以避免命名冲突
            sec.getUserProperties().put(HttpSession.class.getName(), httpSession);
        }
    }
}