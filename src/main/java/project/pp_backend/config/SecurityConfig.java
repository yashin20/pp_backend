package project.pp_backend.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * RESTful API Backend 용
 */
@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    public static final String[] PUBLIC_URLS = {
            "/ws-stomp/**", //WebSocket 연결 엔드포인트
            "/api/auth/**", //인증 엔드포인트
            "/api/members/register",
            "/api/members/register",
            "/api/members/check-nickname",
            "/api/members/check-username",
            "/api/members/check-email",
            "/api/test/**",
            "/uploads/**" //파일 업로드 주소
    };

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtTokenProvider jwtTokenProvider;

    /*PasswordEncoder Bean 등록 - password 암호화 (방식 - BCryptPasswordEncoder)*/
    @Bean
    public static PasswordEncoder passwordEncoder() {return new BCryptPasswordEncoder();}

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

//    //이미지 경로는 시큐리티 필터를 타지 않음
//    @Bean
//    public WebSecurityCustomizer webSecurityCustomizer() {
//        return (web) -> web.ignoring()
//                .requestMatchers("/uploads/**");
//    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                //1. HTTP Basic 인증, Form Login 비활성화 (API 전용)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)

                //2. RESTful API -> Token 사용 (Session X)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                //3. CSRF 보호 비활성화 (Stateless API 에서 사용 X)
                .csrf(AbstractHttpConfigurer::disable)

                //4. 요청별 접근 권한 설정
                .authorizeHttpRequests((authorizeRequests) ->
                        authorizeRequests
                                .requestMatchers(PUBLIC_URLS).permitAll()
                                // 관리자 API는 특정 권한 필요
                                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                                // 나머지 /api/** 경로는 인증된 사용자만 접근 가능
                                .requestMatchers("/api/**").authenticated()
                                //WebSocket 연결
                                .requestMatchers("/ws-stomp/**").permitAll()
                                // 그 외 모든 요청은 불허
                                .anyRequest().denyAll()
                )
                .userDetailsService(customUserDetailsService)

                //5. JWT 필터 등록
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider), //필터 인스턴스 생성
                        UsernamePasswordAuthenticationFilter.class //Username/Password 로그인 필터보다 먼저 실행되도록 설정
                )

                /**[추가]**/
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            // 인증되지 않은 접근 시 401 응답
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\": \"UNAUTHORIZED\"}");
                        })
                );

        return http.build();
    }

}