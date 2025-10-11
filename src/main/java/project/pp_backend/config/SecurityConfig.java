package project.pp_backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtTokenProvider jwtTokenProvider;

    /*PasswordEncoder Bean 등록 - password 암호화 (방식 - BCryptPasswordEncoder)*/
    @Bean
    public static PasswordEncoder passwordEncoder() {return new BCryptPasswordEncoder();}

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

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
                                //** 테스트 코드 임시 허용
                                .requestMatchers("/api/test/**").permitAll()
                                // 인증 엔드포인트는 모두 허용 (회원가입 엔드포인트)
                                .requestMatchers("/api/auth/**", "/ws-stomp/**", "/api/members/register").permitAll()
                                // 관리자 API는 특정 권한 필요
                                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                                // 나머지 /api/** 경로는 인증된 사용자만 접근 가능
                                .requestMatchers("/api/**").authenticated()
                                // 그 외 모든 요청은 불허
                                .anyRequest().denyAll()
                )
                .userDetailsService(customUserDetailsService)

                //5. JWT 필터 등록
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider), //필터 인스턴스 생성
                        UsernamePasswordAuthenticationFilter.class //Username/Password 로그인 필터보다 먼저 실행되도록 설정
                );

        return http.build();
    }

}