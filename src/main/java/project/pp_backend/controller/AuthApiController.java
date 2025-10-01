package project.pp_backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project.pp_backend.dto.MemberDto;
import project.pp_backend.dto.TokenDto;
import project.pp_backend.service.AuthService;
import project.pp_backend.service.MemberService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final MemberService memberService;
    private final AuthService authService;

    /**
     * 1. 회원가입 엔드포인트
     * POST /api/auth/register
     * @param request ID, PW, 닉네임, 이메일 등을 포함하는 회원가입 요청 DTO
     * @return 성공적으로 생성된 회원 ID
     */
    @PostMapping("/register")
    public ResponseEntity<Long> register(@Valid @RequestBody MemberDto.CreateRequest request) {
        //회원가입 처리
        MemberDto.Response response = memberService.createMember(request);
        return ResponseEntity.ok(response.getId());
    }


    /**
     * 2. 로그인 및 토큰 발급 엔드포인트
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

}
