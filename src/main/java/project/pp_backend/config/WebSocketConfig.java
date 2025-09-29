package project.pp_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        //(클라이언트 -> 서버) 클라이언트가 구독할때 사용하는 주소(Prefix)
        registry.enableSimpleBroker("/sub");
        //(클라이언트 -> 서버) 클라이언트가 서버로 메시지를 보낼때 사용하는 prefix
        registry.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        //WebSocket 연결 엔드포인트
        registry.addEndpoint("/ws-stomp").withSockJS();
    }
}