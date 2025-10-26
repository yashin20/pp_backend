package project.pp_backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.messaging.access.intercept.AuthorizationChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String ENDPOINT = "/ws-stomp";
    private static final String SIMPLE_BROKER = "/sub";
    private static final String PUBLISH = "/pub";

    //STOMP 연결 시 JWT 인증을 위한 인터셉터 주입
    private final StompChannelInterceptor stompChannelInterceptor;


    /**
     * STOMP 메시지 브로커 설정
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        //(클라이언트 -> 서버) 클라이언트가 구독할때 사용하는 주소(Prefix)
        registry.enableSimpleBroker(SIMPLE_BROKER);
        //(클라이언트 -> 서버) 클라이언트가 서버로 메시지를 보낼때 사용하는 prefix
        registry.setApplicationDestinationPrefixes(PUBLISH);
    }

    /**
     * STOMP WebSocket 연결 엔드포인트 등록
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        //WebSocket 연결 엔드포인트: /ws-stomp
        registry.addEndpoint(ENDPOINT)
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * 클라이언트로 부터 들어오는 메시지를 채녈(Inbound Channel)에 인터셉터를 등록
     * 이를 통해 STOMP 연결 시 JWT 토큰을 검증할 수 있음.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // [1단계: 인증] JWT 검증 후 STOMP 세션에 Principal 저장
        registration.interceptors(stompChannelInterceptor);
    }
}