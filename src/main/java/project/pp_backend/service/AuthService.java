package project.pp_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.pp_backend.config.JwtTokenProvider;
import project.pp_backend.dto.MemberDto;
import project.pp_backend.dto.TokenDto;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.refresh-token-expiration-milliseconds}")
    private long refreshTokenExpiration;

    private static final String REFRESH_TOKEN_PREFIX = "RT:";

    /** Login 메서드
     * 사용자의 자격 증명 검증, 성공 시 JWT 토큰을 발행
     * @param request ID와 비밓번호를 포함하는 로그인 요청 DTO
     * @return 발급된 Access/Refresh 토큰 정보를 담은 TokenDto
     */
    @Transactional
    public TokenDto.Response login(MemberDto.LoginRequest request) {

        //1. Username/Password를 기반으로 Authentication 객체 생성
        // 이 시점에는 아직 인증되지 않은(unauthenticated) 객체
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                );

        try {
            //2. 실제 인증(Authentication) 진행
            // authenticate() 메서드 실행 시:
            // a. CustomUserDetailsService.loadUserByUsername() 호출
            // b. BCryptPasswordEncoder를 사용하여 비밀번호 일치 여부 검증
            Authentication authenticate = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

            //3. 인증이 성공적으로 완료되면 SecurityContext에 인증 정보 저장
            SecurityContextHolder.getContext().setAuthentication(authenticate);

            //4. JWT 토큰 생성 및 반환
            TokenDto.Response token = jwtTokenProvider.generateToken(authenticate);

            //4-1. Refresh Token을 Redis에 <Key: RT:<Username>, Value: Refresh Token> 형태로 저장
            String refreshTokenKey = REFRESH_TOKEN_PREFIX + authenticate.getName();

            redisTemplate.opsForValue().set(
                    refreshTokenKey,             // Key: RT:testuser01
                    token.getRefreshToken(),     // Value: Refresh Token 문자열
                    refreshTokenExpiration,      // 만료 시간 (TTL)
                    TimeUnit.MILLISECONDS        // 시간 단위: 밀리초
            );

            //4. 인증 정보를 기반으로 JWT 토큰 생성 및 반환
            return token;

        } catch (AuthenticationException e) {
            throw new RuntimeException("로그인 인증에 실패했습니다: " + e.getMessage());
        }
    }


    /** 토큰 재발급 (Reissue) 메서드
     * Refresh Token을 검증하고 새로운 Access Token과 Refresh Token 쌍을 발행
     * @param request Access Token과 Refresh Token을 포함하는 DTO
     * @return 새로 발급된 Access/Refresh 토큰 정보를 담은 TokenDto
     */
    @Transactional
    public TokenDto.Response reissue(TokenDto.Request request) {
        //1. Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            throw new RuntimeException("Refresh Token이 유효하지 않습니다.");
        }

        //2. Access Token에서 Authentication 객체 가져오기 (만료되어도 클레임은 가져올 수 있음)
        Authentication authentication = jwtTokenProvider.getAuthentication(request.getAccessToken());
        String username = authentication.getName();
        String refreshTokenKey = REFRESH_TOKEN_PREFIX + username;

        // 3. 저장소에서 사용자 ID를 기반으로 저장된 Refresh Token 값 가져오기
        String storedRefreshToken = redisTemplate.opsForValue().get(refreshTokenKey);

        //3-1. 저장된 Refresh Token이 없으면 (로그아웃되었거나 만료됨)
        if(storedRefreshToken == null) {
            throw new RuntimeException("로그아웃되었거나 만료된 사용자입니다. 다시 로그인 해주세요.");
        }

        // 4. 저장소의 Refresh Token과 요청된 Refresh Token 일치 여부 검증
        if (!storedRefreshToken.equals(request.getRefreshToken())) {
            // 보안상 중요한 조치: Refresh Token 탈취 시도(토큰 불일치)로 간주, 기존 토큰을 삭제하고 예외 발생
            redisTemplate.delete(refreshTokenKey);
            throw new RuntimeException("Refresh Token 정보가 일치하지 않습니다. 보안상 위험이 감지되었습니다.");
        }

        //5. 새로운 토큰 쌍 생성
        TokenDto.Response newToken = jwtTokenProvider.generateToken(authentication);

        // 6. 저장소 정보 업데이트 (새로운 Refresh Token으로 교체)
        // Redis에 새 Refresh Token을 기존의 TTL(만료 시간)을 유지하며 업데이트
        redisTemplate.opsForValue().set(
                refreshTokenKey,
                newToken.getRefreshToken(),
                refreshTokenExpiration,
                TimeUnit.MILLISECONDS
        );

        //7. 토큰 반환
        return newToken;
    }


    /** Logout 메서드
     * Refresh Token을 저장소에서 삭제하여 토큰 재사용을 방지합니다.
     * @param request Access Token을 포함하는 DTO
     */
    @Transactional
    public void logout(TokenDto.Request request) {
        //1. Access Token 에서 Authentication 객체 가져오기
        Authentication authentication = jwtTokenProvider.getAuthentication(request.getAccessToken());
        String username = authentication.getName();
        String refreshTokenKey = REFRESH_TOKEN_PREFIX + username;

        // 2. Refresh Token 저장소에서 해당 사용자 ID의 Refresh Token 삭제
        //Redis에 키가 존재하는지 확인 후 삭제
        if (redisTemplate.hasKey(refreshTokenKey)) {
            redisTemplate.delete(refreshTokenKey);
        }

        // SecurityContext 초기화
        SecurityContextHolder.clearContext();
    }



}
