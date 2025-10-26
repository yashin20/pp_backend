package project.pp_backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * STOMP 연결 시, JWT 토큰을 검증하고, SecurityContext 에 인증 정보를 설정하는 인터셉터
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

    //STOMP CONNECT 프레임에서 JWT(AccessToken)을 전달받을 헤더 이름
    public static final String ACCESS_TOKEN_HEADER = "Authorization"; //AccessToken Key
    public static final String BEARER_ = "Bearer "; //AccessToken 선행 문자

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        log.info("🚨 [STOMP INTERCEPTOR] Message received. Command: {}", StompHeaderAccessor.wrap(message).getCommand());
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        log.info("[test] accessor.getCommand(): {}", accessor.getCommand());
        if (!StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.info("[test] message: {}", message);
            return message;
        }

        Optional<String> jwtTokenOptional = Optional.ofNullable(accessor.getFirstNativeHeader(ACCESS_TOKEN_HEADER));
        String jwtToken = jwtTokenOptional
                .filter(token -> token.startsWith(BEARER_))
                .map(token -> token.substring(BEARER_.length()))
                .filter(token -> jwtTokenProvider.validateToken(token)) //토큰의 유효성(만료) 검증(T/F)
                .orElseThrow(() -> new RuntimeException("Invalid token"));
        log.info("[test] jwtToken: {}", jwtToken);

        Authentication authentication = jwtTokenProvider.getAuthentication(jwtToken);
        accessor.setUser(authentication);
        log.info("[test] accessor.getUser(): {}", accessor.getUser());

        log.info("[test] message: {}", message);
        return message;
    }



//    @Override
//    public Message<?> preSend(Message<?> message, MessageChannel channel) {
//
//        log.info("🚨 [STOMP INTERCEPTOR] Message received. Command: {}", StompHeaderAccessor.wrap(message).getCommand());
//
//        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
//
//        log.info("[test] StompCommand.CONNECT.equals(accessor.getCommand()): {}", StompCommand.CONNECT.equals(accessor.getCommand()));
//
//
//
//        // STOMP CONNECT 명령 처리 (WebSocket 연결 시도)
//        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
//
//            // 1. 헤더에서 'Authorization' 토큰 추출
//            // 클라이언트는 CONNECT 프레임에 "Authorization: Bearer <token>" 형태로 토큰을 전달해야 함.
//            String authHeader = accessor.getFirstNativeHeader(ACCESS_TOKEN_HEADER);
//
//            if (authHeader != null && authHeader.startsWith(BEARER_)) {
//                String token = authHeader.substring(7);
//                log.info("[test] token: {}", token);
//
//                // 2. 토큰 유효성 검증
//                log.info("[test] jwtTokenProvider.validateToken(token): {}", jwtTokenProvider.validateToken(token));
//                if (jwtTokenProvider.validateToken(token)) {
//                    // 3. 토큰에서 인증 정보(Authentication) 객체 생성
//                    Authentication authentication = jwtTokenProvider.getAuthentication(token);
//                    log.info("[test] authentication: {}", authentication.getName());
//
//                    // 4. WebSocket 세션에 인증 정보 저장
//                    accessor.setUser(authentication);
//                    log.info("[test] accessor: {}", accessor.getUser());
//
//                    //SecurityContextHolder 에서도 인증 정보 설정
//                    SecurityContextHolder.getContext().setAuthentication(authentication);
//
//                    log.info("STOMP 세션 인증 성공: {}", authentication.getName());
//                } else {
//                    log.warn("STOMP 연결 중 유효하지 않은 JWT 토큰 수신.");
//                }
//            } else {
//                log.warn("Authorization 헤더 누락 또는 'Bearer '로 시작하지 않음.");
//            }
//        }
//
//
//        return message;
//    }

}
