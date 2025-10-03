package project.pp_backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import project.pp_backend.dto.MemberDto;
import project.pp_backend.service.MemberService;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    /**
     * 1. 회원가입 엔드포인트
     * POST /api/members/register
     * @param request ID, PW, 닉네임, 이메일 등을 포함하는 회원가입 요청 DTO
     * @return 성공적으로 생성된 회원 ID
     */
    @PostMapping("/register")
    public ResponseEntity<Long> register(@Valid @RequestBody MemberDto.CreateRequest request) {
        //회원가입 처리
        MemberDto.Response response = memberService.createMember(request);
        return ResponseEntity.ok(response.getId());
    }

    /** *****TEST 용 엔드포인트야!
     * 2. 인증된 사용자 정보 조회 엔드포인트
     * GET /api/members/my
     * AccessToken을 포함한 요청만 접근 가능
     * @return 현재 인증된 사용자의 ID(username)와 닉네임
     */
    @GetMapping("/my")
    public ResponseEntity<String> getMyInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        //AccessToken이 없거나 유효하지 않아 인증에 실패한 경우
        if (authentication == null || authentication.getName().equals("anonymousUser")) {
            return ResponseEntity.status(401).body("Unauthorized: 인증 정보가 유효하지 않습니다.");
        }

        String username = authentication.getName();

        return ResponseEntity.ok("인증 성공! 현재 로그인 ID: " + username);
    }

}
