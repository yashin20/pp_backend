package project.pp_backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 이 필터는 Spring Security 설정(SecurityConfig)에서 생성자를 통해
     * JwtTokenProvider를 주입받아 사용됩니다.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 1. HTTP 헤더에서 토큰 추출
        String token = resolveToken(request);

        // 2. 토큰 유효성 검사 및 인증 처리
        if (token != null && jwtTokenProvider.validateToken(token)) {
            // 3. 토큰에서 인증 객체를 얻어 SecurityContext에 저장
            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
    /**
     * HTTP 요청 헤더에서 Bearer 토큰을 추출합니다.
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        //헤더 값이 있고, "Bearer "로 시작하는지 확인
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            //"Bearer " (7글자) 이후의 실제 토큰 값만 반환
            return bearerToken.substring(7);
        }
        return null;
    }
}
