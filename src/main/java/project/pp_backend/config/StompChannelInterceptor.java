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
import org.springframework.security.core.context.SecurityContextHolder;
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

        //1. Header에서 토큰 추출
        String authorizationHeader = accessor.getFirstNativeHeader(ACCESS_TOKEN_HEADER);

        //2. 토큰이 없거나 형식이 'Bearer'가 아니면 예외를 발생시키지 않고 연결을 거부
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_)) {
            log.error("[STOMP AUTH] 인증 헤더가 누락되었거나 형식이 잘못되었습니다.");
            return null;
        }

        //3. JWT TOKEN 추출
        String jwtToken = authorizationHeader.substring(BEARER_.length());

        try {
            // 3. 토큰의 유효성 검증 (만료, 변조 등)
            if (!jwtTokenProvider.validateToken(jwtToken)) {
                log.error("[STOMP AUTH] 토큰이 유효하지 않거나 만료되었습니다.");
                return null; // 유효성 검증 실패 시 연결 거부
            }

            // 4. 인증 정보 설정
            Authentication authentication = jwtTokenProvider.getAuthentication(jwtToken);
            accessor.setUser(authentication);

            // SecurityContext에도 설정 (필요에 따라)
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("[STOMP AUTH] 인증 성공. User: {}", authentication.getName());
            return message;

        } catch (Exception e) {
            log.error("[STOMP AUTH] 토큰 처리 중 예외 발생: {}", e.getMessage());
            // 예외 발생 시 연결 거부
            return null;
        }
    }
}
