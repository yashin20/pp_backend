package project.pp_backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project.pp_backend.dto.MemberDto;
import project.pp_backend.dto.TokenDto;
import project.pp_backend.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthService authService;

    /**
     * 1. 로그인 및 토큰 발급 엔드포인트
     * POST /api/auth/login
     * @param request ID와 PW를 포함하는 로그인 요청 DTO
     * @return 발급된 Access/Refresh 토큰 정보를 담은 TokenDto
     */
    @PostMapping("/login")
    public ResponseEntity<TokenDto.Response> login(@Valid @RequestBody MemberDto.LoginRequest request) {
        // AuthService를 통해 인증 후 토큰 발급
        TokenDto.Response token = authService.login(request);
        return ResponseEntity.ok(token);
    }

    /**
     * 2. 토큰 재발급 엔드 포인트
     * POST /api/auth/reissue
     * @param request Access Token과 Refresh Token을 포함하는 DTO
     * @return 새로 발급된 Access/Refresh 토큰 정보를 담은 TokenDto
     */
    @PostMapping("/reissue")
    public ResponseEntity<TokenDto.Response> reissue(@Valid @RequestBody TokenDto.Request request) {
        TokenDto.Response newToken = authService.reissue(request);
        return ResponseEntity.ok(newToken);
    }

    /**
     * 3. 로그아웃 엔드 포인트
     * POST /api/auth/logout
     * @param request AccessToken을 포함하는 DTO (RefreshToken을 찾아 삭제하기 위함)
     * @return 200 OK
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody TokenDto.Request request) {
        //AuthService를 통해 Redis에 저장된 RefreshToken 삭제
        authService.logout(request);
        return ResponseEntity.ok().build();
    }

}
