package project.pp_backend.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import project.pp_backend.dto.TokenDto;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {
    private static final String AUTHORITIES_KEY = "auth";
    private static final String BEARER_TYPE = "Bearer";
    private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 30; // 30분
    private final Key key;

    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 1. Authentication 객체를 받아서 토큰(Access/Refresh) 생성
     */
    public TokenDto.Response generateToken(Authentication authentication) {
        //1. 권한 정보 가져오기 (ROLE_USER, ROLE_ADMIN)
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();

        //2. Access Token 생성
        String accessToken = Jwts.builder()
                .setSubject(authentication.getName()) // Principal(주체) 이름 (예: username 또는 ID)
                .claim(AUTHORITIES_KEY, authorities)  // Payload에 권한 정보 저장
                .setExpiration(new Date(now + ACCESS_TOKEN_EXPIRE_TIME)) // 만료 시간 설정
                .signWith(key, SignatureAlgorithm.HS512) // 시그니처 생성
                .compact();

        // 3. Refresh Token 생성 (여기서는 간단히 Access Token만 반환)
        // 실제로는 더 긴 만료 시간으로 Refresh Token을 별도로 생성해야 합니다.

        return TokenDto.Response.builder()
                .grantType(BEARER_TYPE)
                .accessToken(accessToken)
                .build();
    }

    /**
     * 2. JWT 토큰을 복호화하여 인증 객체(Authentication)를 생성합니다.
     */
    public Authentication getAuthentication(String accessToken) {

        // 1. 토큰 복호화
        Claims claims = parseClaims(accessToken);

        if (claims.get(AUTHORITIES_KEY) == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        // 2. 클레임에서 권한 정보 추출
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        // 3. UserDetails 객체를 만들어서 Authentication 리턴 (SecurityContext 사용)
        UserDetails principal = new User(claims.getSubject(), "", authorities);

        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    /**
     * 3. 토큰의 유효성을 검증합니다.
     */
    public boolean validateToken(String token) {
        try {
            // [수정 핵심]: Jwts.parser().setSigningKey(key) 뒤에 .build()를 추가해야 합니다.
            Jwts.parser() // 0.12.5 버전에서는 Jwts.parserBuilder() 사용 가능 (또는 Jwts.parser())
                    .setSigningKey(key) // Secret Key 설정
                    .build()            // <--- 이 메서드를 반드시 호출해야 합니다!
                    .parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다.", e);
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.", e);
        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰입니다.", e);
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 잘못되었습니다.", e);
        }
        return false;
    }

    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(accessToken)
                    .getBody();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰이어도 클레임은 추출 가능 (재발급 등에 사용)
            return e.getClaims();
        }
    }
}
